package serverapp;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import ringmessagestubs.RingMessageServiceGrpc;
import ringmessagestubs.RingRequest;
import ringmessagestubs.RingResponse;

public class RingMessageServiceImpl extends RingMessageServiceGrpc.RingMessageServiceImplBase {

    private final PrimeServer primeServer;
    private final RedisUtil redisUtil;
    private final DockerUtil dockerUtil = new DockerUtil();

    public RingMessageServiceImpl(PrimeServer primeServer) {
        this.primeServer = primeServer;
        this.redisUtil = primeServer.getRedisUtil();
    }

    @Override
    public StreamObserver<RingRequest> ringMessage(StreamObserver<RingResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(RingRequest request) {
                long number = request.getNumber();
                String originIp = request.getOriginIp();
                int originPort = request.getOriginPort();
                boolean isPrime = request.getIsPrime();
                boolean found = request.getHasInfo();

                System.out.println("Server - " + primeServer.getPort());
                if (primeServer.getIp().equals(originIp) && primeServer.getPort() == originPort) { // This is the original server (looped back)
                    found = true; // hardcode to true to avoid infinite loop, TODO - Remove after redis and docker implementation
                    if (found) { // Result is already known, immediately respond to client
                        RingResponse ringResponse = RingResponse.newBuilder()
                                .setIsPrime(isPrime)
                                .build();
                        responseObserver.onNext(ringResponse);
                        responseObserver.onCompleted();
                    } else { // Result is not known, launch Docker container
                        System.out.println("Launching Docker container to check if prime...");
                        boolean prime = dockerUtil.launchPrimeCheckContainer(number,redisUtil.getRedisPort());
                        RingResponse ringResponse = RingResponse.newBuilder()
                                .setIsPrime(prime)
                                .build();
                        responseObserver.onNext(ringResponse);
                        responseObserver.onCompleted();
                    }
                } else {
                    if (found) { // Result is already known, add to Redis
                        System.out.println("Adding result to Redis...");
                        redisUtil.savePrimeInRedis(number, isPrime);
                        forwardRingMessage(request, found, isPrime, false, responseObserver);
                    } else { // Result is not known, check Redis and update if found
                        System.out.println("Checking Redis for result...");
                        boolean update = false;

                        RedisCheckResult redisResult = redisUtil.checkIfPrimeInRedis(number);
                        found = redisResult.isFound();

                        if (found) {
                            System.out.println("Result found in Redis");
                            isPrime = redisResult.isPrime();
                            update = true;
                        }

                        // Forward the request to the next server in the ring
                        forwardRingMessage(request, found, isPrime, update, responseObserver);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error in ringMessage: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                // Nothing specific to do on completed for intermediate servers
            }
        };
    }

    // Forward the request to the next server in the ring
    private void forwardRingMessage(RingRequest request, boolean hasInfo, boolean isPrime, boolean update, StreamObserver<RingResponse> responseObserver) {
        String nextIp = primeServer.getNextIp();
        int nextPort = primeServer.getNextPort();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(nextIp, nextPort)
                .usePlaintext()
                .build();

        RingMessageServiceGrpc.RingMessageServiceStub stub = RingMessageServiceGrpc.newStub(channel);

        // Update the RingRequest with information from Redis if available
        if (update) {
            request = request.toBuilder()
                    .setHasInfo(hasInfo)
                    .setIsPrime(isPrime)
                    .build();
        }

        System.out.println("Forwarding request to " + nextIp + ":" + nextPort);

        // Forward the request to the next server
        stub.ringMessage(responseObserver).onNext(request);
    }

}