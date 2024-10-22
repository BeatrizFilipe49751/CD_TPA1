package serverapp;

import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import ringmanagerstubs.*;
import java.util.ArrayList;

public class RingManager extends RingManagerServiceGrpc.RingManagerServiceImplBase {

    private final ArrayList<StreamObserver<ServerInfo>> serverObservers = new ArrayList<>();
    private ServerInfo firstServerInfo; // Information about the first server in the ring
    private static int svcPort = 8500;

    public static void main(String[] args) {
        try {
            if (args.length > 0) svcPort = Integer.parseInt(args[0]);
            RingManager ringManager = new RingManager(); // Create an instance
            io.grpc.Server svc = ServerBuilder
                    .forPort(svcPort)
                    .addService(ringManager)
                    .addService(new RingManagerPrime(ringManager))
                    .build();
            svc.start();
            System.out.println("Server started, listening on " + svcPort);

            svc.awaitTermination();
            svc.shutdown();

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    public RingManager() {
    }

    // New method to expose the firstServerInfo
    public ServerInfo getFirstServerInfo() {
        return serverObservers.isEmpty() ? null : firstServerInfo;
    }

    @Override
    public void registServer(ServerInfo request, StreamObserver<ServerInfo> responseObserver) {
        // If this is the first server, store its info as the "first server"
        if (serverObservers.isEmpty()) {
            firstServerInfo = request;
        }

        // Add the current server's observer to the list
        serverObservers.add(responseObserver);

        // If there is more than one server, update the previous server with the new server's information
        if (serverObservers.size() > 1) {
            StreamObserver<ServerInfo> previousObserver = serverObservers.get(serverObservers.size() - 2);

            ServerInfo nextServerInfo = ServerInfo.newBuilder()
                    .setIp(request.getIp())
                    .setPort(request.getPort())
                    .build();

            // Notify the previous server of the next one
            previousObserver.onNext(nextServerInfo);
        }

        // If this is the last server and there's more than one server in the list, make sure that the last server points to the first one to close the ring
        if (serverObservers.size() > 1) {
            StreamObserver<ServerInfo> lastObserver = serverObservers.get(serverObservers.size() - 1);

            // Notify the last server that its next server is the first server
            lastObserver.onNext(firstServerInfo);
        }

        System.out.println("Server registered " + request.getPort());
    }
}
