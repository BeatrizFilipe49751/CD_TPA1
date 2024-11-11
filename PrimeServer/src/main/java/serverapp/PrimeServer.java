package serverapp;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import ringmanagerstubs.*;
import ringmanagerstubs.ServerInfo;
import utils.GoogleCloudUtils;

import java.io.IOException;
import java.net.URI;
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
    private final DockerClient dockerClient;
    private RedisUtil redisUtil;

    public PrimeServer(String ringManagerIp, int ringManagerPort, String myIp, int myPort) {
        this.myIp = myIp;
        this.myPort = myPort;
        this.nextIp = null;
        this.nextPort = 0;

        // Initialize a Redis Docker client as long it is in a windows or linux environment
        String HOST_URI = System.getProperty("os.name").toLowerCase().contains("win")
                ? "tcp://localhost:2375"
                : "unix:///var/run/docker.sock";

        // Create the Docker client
        this.dockerClient = DockerClientBuilder.getInstance()
                .withDockerHttpClient(new ApacheDockerHttpClient.Builder()
                        .dockerHost(URI.create(HOST_URI))
                        .build())
                .build();

        // Create the channel to connect to the RingManager
        this.channel = ManagedChannelBuilder.forAddress(ringManagerIp, ringManagerPort)
                .usePlaintext() // No SSL for simplification
                .build();

        // Create the async stub for RingManager calls
        this.ringManagerStub = RingManagerServiceGrpc.newStub(channel);
    }

    // Method to launch Redis container
    public void launchRedisContainer() {
        String containerName = "RedisCt_" + myPort;
        String imageName = "redis";
        int mainPort = 6379;
        int redisHostPort = mainPort + myPort;  // Dynamic port allocation for multiple instances

        try {
            // Configure port binding
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withPortBindings(PortBinding.parse(redisHostPort + ":" + mainPort));

            // Create and start the container
            CreateContainerResponse containerResponse = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .exec();

            String redisContainerId = containerResponse.getId();
            dockerClient.startContainerCmd(redisContainerId).exec();

            System.out.println("Redis container launched on port " + redisHostPort);

            // Create RedisUtil with the Redis port
            redisUtil = new RedisUtil(redisHostPort);

        } catch (Exception e) {
            System.err.println("Failed to launch Redis container: " + e.getMessage());
        }
    }

    // Getter for the next server's IP
    public String getNextIp() { return nextIp; }

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

    //Getter for the redisUtil
    public RedisUtil getRedisUtil() { return redisUtil; }

    // The stub is asynchronous, so its wiser need to create a new thread
    public void registerWithRingManager() {
        // Create a new thread for registration and listening
        Thread t = new Thread(() -> {
            try {
                // Create a latch to wait for the response
                CountDownLatch latch = new CountDownLatch(1);

                // Create the response observer for server streaming
                StreamObserver<ServerInfo> responseObserver = new StreamObserver<>() {
                    @Override
                    public void onNext(ServerInfo nextServerInfo) {
                        // When a new server is registered, receive the next server's information
                        nextIp = nextServerInfo.getIp();
                        nextPort = nextServerInfo.getPort();
                        System.out.println("Next server on the ring: " + nextIp + ":" + nextPort);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error receiving information about the next server on the ring: " + t.getMessage());
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
        });
        t.start();
    }

    // Method to start the gRPC server
    public void start() {
        try {
            server = ServerBuilder.forPort(myPort)
                    .addService(new IsPrimeServiceImpl(this))
                    .addService(new RingMessageServiceImpl(this))
                    .build()
                    .start();
            System.out.println("Server started on " + myIp + ":" + myPort);
        } catch (IOException e) {
            System.out.println("Error starting the server: " + e.getMessage());
        }
    }

    // Method to close the communication channel
    public void shutdown() {
        try {
            // Stop and remove the Redis container
            if (redisUtil != null) {
                String containerName = "RedisCt_" + myPort;
                // Stop the container
                dockerClient.stopContainerCmd(containerName).exec();
                // Remove the container
                dockerClient.removeContainerCmd(containerName).exec();
            }

            channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
            if (server != null) {
                server.shutdown().awaitTermination();
            }
        } catch (InterruptedException e) {
            System.out.println("Error shutting down the server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Check if the required arguments are provided
        if (args.length < 1) {
            System.out.println("Usage: java -jar PrimeServer.jar <PrimeServerPort>");
            return;
        }

        int ringManagerPort = GoogleCloudUtils.getRingManagerPort();

        String ringManagerIp;
        String myIp;
        try {
            myIp = GoogleCloudUtils.getExternalIp();
            ringManagerIp = GoogleCloudUtils.getRingManagerIp();
        } catch (Exception e) {
            System.out.println("Error getting the external IP: " + e.getMessage());
            return;
        }
        // PrimeServer Port from the arguments
        int myPort = Integer.parseInt(args[0]);

        // Create and register the PrimeServer
        PrimeServer primeServer = new PrimeServer(ringManagerIp, ringManagerPort, myIp, myPort);
        primeServer.launchRedisContainer();
        primeServer.registerWithRingManager();
        primeServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(primeServer::shutdown));
    }
}