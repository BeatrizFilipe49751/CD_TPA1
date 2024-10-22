package serverapp;

import io.grpc.stub.StreamObserver;
import getserverstubs.*;
import ringmanagerstubs.ServerInfo;

public class RingManagerPrime extends GetServerServiceGrpc.GetServerServiceImplBase {

    private final RingManager ringManager;

    public RingManagerPrime(RingManager ringManager) {
        this.ringManager = ringManager;
    }

    @Override
    public void getPrimeServer(Request request, StreamObserver<EndPoint> responseObserver) {
        // TODO - Remove this and add load balancing
        ServerInfo firstServerInfo = ringManager.getFirstServerInfo();
        if (firstServerInfo == null) {
            responseObserver.onError(new Exception("No server registered yet"));
            return;
        }

        EndPoint endPoint = EndPoint.newBuilder()
                .setIp(firstServerInfo.getIp())
                .setPort(firstServerInfo.getPort())
                .build();
        responseObserver.onNext(endPoint);
        responseObserver.onCompleted();
    }
}
