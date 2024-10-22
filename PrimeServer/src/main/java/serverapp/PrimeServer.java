package serverapp;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import ringmanagerstubs.*;
import ringmanagerstubs.ServerInfo;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PrimeServer {

    private final String myIp;
    private final int myPort;
    private final ManagedChannel channel;
    private final RingManagerServiceGrpc.RingManagerServiceStub ringManagerStub;
    private String nextIp;
    private int nextPort;
    private Server server;

    public PrimeServer(String ringManagerIp, int ringManagerPort, String myIp, int myPort) {
        this.myIp = myIp;
        this.myPort = myPort;
        this.nextIp = null;
        this.nextPort = 0;

        // Create the channel to connect to the RingManager
        channel = ManagedChannelBuilder.forAddress(ringManagerIp, ringManagerPort)
                .usePlaintext() // No SSL for simplification
                .build();

        // Create the async stub for RingManager calls
        ringManagerStub = RingManagerServiceGrpc.newStub(channel);
    }

    // Getter for the next server's IP
    public String getNextIp() {
        return nextIp;
    }

    // Getter for the next server's port
    public int getNextPort() {
        return nextPort;
    }

    // Getter for the current server's IP
    public String getIp() {
        return myIp;
    }

    // Getter for the current server's port
    public int getPort() {
        return myPort;
    }

    public void registerWithRingManager() {
        // Create a new thread for registration and listening
        new Thread(() -> {
            try {
                // Create a latch to wait for the response
                CountDownLatch latch = new CountDownLatch(1);

                // Create the response observer for server streaming
                StreamObserver<ServerInfo> responseObserver = new StreamObserver<>() {
                    @Override
                    public void onNext(ServerInfo nextServerInfo) {
                        // When a new server is registered, receive the next server's info
                        System.out.println("Received information about the next server");
                        nextIp = nextServerInfo.getIp();
                        nextPort = nextServerInfo.getPort();
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error receiving information about the next server: " + t.getMessage());
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Connection with the RingManager closed.");
                        latch.countDown();
                    }
                };

                // Create the message with the current server's information (IP and port)
                ServerInfo myInfo = ServerInfo.newBuilder()
                        .setIp(myIp)
                        .setPort(myPort)
                        .build();

                // Call the `registServer` method of the RingManager, passing the current server and observer
                ringManagerStub.registServer(myInfo, responseObserver);

                // Wait indefinitely for the stream to complete
                latch.await();
            } catch (InterruptedException e) {
                System.out.println("Registration thread interrupted: " + e.getMessage());
            } finally {
                shutdown();
            }
        }).start(); // Start the new thread
    }

    // Method to start the gRPC server
    public void start() throws IOException {
        server = ServerBuilder.forPort(myPort)
                .addService(new IsPrimeServiceImpl(this))
                .addService(new RingMessageServiceImpl(this))
                .build()
                .start();
        System.out.println("Server started on " + myIp + ":" + myPort);
    }

    // Method to close the communication channel
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            if (server != null) {
                server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            System.out.println("Error shutting down the server: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        // Check if the required arguments are provided
        if (args.length < 2) {
            System.out.println("Usage: java -jar PrimeServer.jar <PrimeServerIp> <PrimeServerPort>");
            return;
        }

        // RingManager settings (IP and Port) from the arguments
        String ringManagerIp = "localhost";
        int ringManagerPort = 8500;

        // PrimeServer settings (IP and Port) from the arguments
        String myIp = args[0];
        int myPort = Integer.parseInt(args[1]);

        // Create and register the PrimeServer
        PrimeServer primeServer = new PrimeServer(ringManagerIp, ringManagerPort, myIp, myPort);
        primeServer.registerWithRingManager();
        primeServer.start();
    }
}