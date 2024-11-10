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

        // Check if this server already knows the result (Redis)
        RedisCheckResult redisCheckResult = redisUtil.checkIfPrimeInRedis(number);
        boolean found = redisCheckResult.isFound();
        boolean isPrime = redisCheckResult.isPrime();
        Result.Builder result = Result.newBuilder();
        Result response;

        // The Server already knows the result, send it directly to the client
        if (found) {
            System.out.println("Found result in Redis, no need to forward");
            response = result.setPrime(isPrime).build();

        } else  // Check if there is only one server in the ring
            if (primeServer.getNextIp() == null && primeServer.getNextPort() == 0) {
            System.out.println("Only one server in the ring");
            dockerUtil.launchPrimeCheckContainer(number,redisUtil.getRedisPort());
            boolean prime = redisUtil.checkIfPrimeInRedis(number).isPrime();
            response = Result.newBuilder().setPrime(prime).build();


        } else /* Forward to the next ring and wait for the result */ {
            System.out.println("Forwarding to the ring");
            forwardToRing(number, responseObserver);
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
                .setOriginIp(primeServer.getIp())       // Pass the original server IP
                .setOriginPort(primeServer.getPort())   // Pass the original server port
                .setHasInfo(false)                      // This server does not know the result
                .setIsPrime(false)                      // Default value
                .build();

        // Forward the request to the ring
        StreamObserver<RingResponse> ringResponseObserver = new StreamObserver<>() {
            @Override
            public void onNext(RingResponse response) {
                // Once the response is back from the ring, send it to a client
                System.out.println("Received response from ring");
                Result result = Result.newBuilder().setPrime(response.getIsPrime()).build();
                responseObserver.onNext(result);
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                // Forward error to the client
                System.err.println("Error forwarding to ring: " + t.getMessage());
                responseObserver.onError(t);  // Forward error to a client
            }

            @Override
            public void onCompleted() {
                // Close the channel when done
                System.err.println("Connection with the ring closed");
                channel.shutdown();
            }
        };

        // Send the request to the next server in the ring
        stub.ringMessage(ringResponseObserver).onNext(ringRequest);
    }

}
