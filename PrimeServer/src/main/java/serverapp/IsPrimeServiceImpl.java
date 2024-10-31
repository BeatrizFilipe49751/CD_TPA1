package serverapp;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import isprimestubs.IsPrimeServiceGrpc;
import isprimestubs.Number;
import isprimestubs.Result;
import ringmessagestubs.RingMessageServiceGrpc;
import ringmessagestubs.RingRequest;
import ringmessagestubs.RingResponse;

public class IsPrimeServiceImpl extends IsPrimeServiceGrpc.IsPrimeServiceImplBase {
    private final PrimeServer primeServer;
    private final RedisUtil redisUtil;
    private final DockerUtil dockerUtil = new DockerUtil();

    public IsPrimeServiceImpl(PrimeServer primeServer) {
        this.primeServer = primeServer;
        this.redisUtil = primeServer.getRedisUtil();
    }

    @Override
    public void isPrime(Number request, StreamObserver<Result> responseObserver) {
        long number = request.getNum();

        // Check if this server already knows the result (e.g., from Redis)
        RedisCheckResult redisCheckResult = redisUtil.checkIfPrimeInRedis(number);
        boolean found = redisCheckResult.isFound();
        boolean isPrime = redisCheckResult.isPrime();

        if (found) { // Server already knows the result, send it directly to the client
            Result result = Result.newBuilder().setPrime(isPrime).build();
            responseObserver.onNext(result);
            responseObserver.onCompleted();
        } else if (primeServer.getNextIp() == null && primeServer.getNextPort() == 0) { // Only one server in the ring
            boolean prime = dockerUtil.launchPrimeCheckContainer(number);
            Result result = Result.newBuilder().setPrime(prime).build();
            responseObserver.onNext(result);
            responseObserver.onCompleted();
        } else {
            // Forward to the ring and wait for the result
            forwardToRing(number, responseObserver);
        }
    }

    private void forwardToRing(long number, StreamObserver<Result> responseObserver) {
        String nextIp = primeServer.getNextIp();
        int nextPort = primeServer.getNextPort();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(nextIp, nextPort)
                .usePlaintext()
                .build();

        RingMessageServiceGrpc.RingMessageServiceStub stub = RingMessageServiceGrpc.newStub(channel);

        // Create the RingRequest to forward
        RingRequest ringRequest = RingRequest.newBuilder()
                .setNumber(number)
                .setOriginIp(primeServer.getIp()) // Pass the original server IP
                .setOriginPort(primeServer.getPort()) // Pass the original server port
                .setHasInfo(false) // This server doesn't know the result
                .setIsPrime(false) // Default value
                .build();

        // Forward the request to the ring
        StreamObserver<RingResponse> ringResponseObserver = new StreamObserver<>() {
            @Override
            public void onNext(RingResponse response) {
                // Once response is back from the ring, send it to client
                System.out.println("Received response from ring");
                Result result = Result.newBuilder().setPrime(response.getIsPrime()).build();
                responseObserver.onNext(result);
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error forwarding to ring: " + t.getMessage());
                responseObserver.onError(t);  // Forward error to client
            }

            @Override
            public void onCompleted() {
                // Close the channel when done
                channel.shutdown();
            }
        };

        // Send the request to the next server in the ring
        stub.ringMessage(ringResponseObserver).onNext(ringRequest);
    }

}
