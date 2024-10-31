package serverapp;

import io.grpc.stub.StreamObserver;
import getserverstubs.*;
import ringmanagerstubs.ServerInfo;

public class RingManagerPrime extends GetServerServiceGrpc.GetServerServiceImplBase {

    private final RingManager ringManager;
    private int currentIndex = 0; // Index of server for load balancing

    public RingManagerPrime(RingManager ringManager) {
        this.ringManager = ringManager;
    }

    @Override
    public void getPrimeServer(Request request, StreamObserver<EndPoint> responseObserver) {
        if (ringManager.getServerInfoList().isEmpty()) {
            responseObserver.onError(new Exception("No server registered yet"));
            return;
        }

        ServerInfo serverInfo = ringManager.getServerInfoList().get(currentIndex);
        currentIndex = (currentIndex + 1) % ringManager.getServerInfoList().size();  // Incrementa e volta ao início, se necessário

        EndPoint endPoint = EndPoint.newBuilder()
                .setIp(serverInfo.getIp())
                .setPort(serverInfo.getPort())
                .build();
        responseObserver.onNext(endPoint);
        responseObserver.onCompleted();
    }
}
