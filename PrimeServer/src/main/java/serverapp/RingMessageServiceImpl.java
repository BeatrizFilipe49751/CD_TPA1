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

                System.out.println("Received message for number " + number);

                if (primeServer.getIp().equals(originIp) && primeServer.getPort() == originPort) { // This is the original server (looped back)
                    final RingResponse.Builder ringResponse = RingResponse.newBuilder();
                    RingResponse response;
                    if (found) {
                        response = ringResponse.setIsPrime(isPrime).build();
                    } else {
                        response = launchDockerContainerCase(number);
                    }
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                } else {
                    if (found) { // Result is already known, add to Redis
                        foundResultCase(request, number, isPrime, responseObserver);
                    } else { // Result is not known, check Redis and update if found
                        notFoundResultCase(number, isPrime, request, responseObserver);
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

    // Launch a Docker container to check if a number is prime
    private RingResponse launchDockerContainerCase(long number) {
        dockerUtil.launchPrimeCheckContainer(number, redisUtil.getRedisPort());
        boolean prime = redisUtil.checkIfPrimeInRedis(number).isPrime();
        return RingResponse.newBuilder().setIsPrime(prime).build();
    }

    // Save the result in Redis and forward the message
    private void foundResultCase(RingRequest request, long number, boolean isPrime, StreamObserver<RingResponse> responseObserver) {
        redisUtil.savePrimeInRedis(number, isPrime);
        forwardRingMessage(request, true, isPrime, false, responseObserver);
    }

    // Check Redis for the result and forward the message
    private void notFoundResultCase(long number, boolean isPrime, RingRequest request, StreamObserver<RingResponse> responseObserver) {
        boolean update = false;

        RedisCheckResult redisResult = redisUtil.checkIfPrimeInRedis(number);
        boolean found = redisResult.isFound();

        if (found) {
            isPrime = redisResult.isPrime();
            System.out.println("Result found in Redis, number: " + number + ", isPrime: " + isPrime);
            update = true;
        } else {
            System.out.println("Result not found in Redis, number: " + number);
        }

        // Forward the request to the next server in the ring
        forwardRingMessage(request, found, isPrime, update, responseObserver);
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

        System.out.println("Forwarding message to " + nextIp + ":" + nextPort);

        // Forward the request to the next server
        stub.ringMessage(responseObserver).onNext(request);
    }

}