package clientapp;

import isprimestubs.*;
import getserverstubs.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import isprimestubs.Number;

import java.util.*;


public class Client {

    // RingManager IP - To Change
    private static final String svcIP = "localhost";
    // RingManager Port - To Change
    private static final int svcPort = 8500;
    private static ManagedChannel channel;
    private static IsPrimeServiceGrpc.IsPrimeServiceBlockingStub primeBlockingStub;

    private static GetServerServiceGrpc.GetServerServiceBlockingStub serverBlockingStub;


    public static void main(String[] args) {
        try {
            // Retrieve the PrimeServer endpoint from RingManager
            EndPoint primeServerEndPoint = getPrimeServerEndPoint();
            String serverIp = primeServerEndPoint.getIp();
            int serverPort = primeServerEndPoint.getPort();
            System.out.println("Received PrimeServer EndPoint: " + serverIp + ":" + serverPort);

            while (true) {
                switch (Menu()) {
                    case 1:  // Check if number is prime
                        checkPrimeServer(serverIp, serverPort);
                        break;
                    case 0:
                        System.exit(0);
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            System.out.println("An error occurred");
        }
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println("1 - Check if number is prime");
            System.out.println("0 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 4) || op == 0));
        return op;
    }

    // Function to retrieve the PrimeServer EndPoint from the RingManager
    private static EndPoint getPrimeServerEndPoint() {
        EndPoint primeServerEndPoint = null;
        try {
            // Connect to RingManager
            System.out.println("Connecting to RingManager at " + svcIP + ":" + svcPort);
            channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    .usePlaintext()
                    .build();
            serverBlockingStub = GetServerServiceGrpc.newBlockingStub(channel);

            // Request PrimeServer EndPoint
            System.out.println("Requesting PrimeServer EndPoint from RingManager...");
            primeServerEndPoint = serverBlockingStub.getPrimeServer(Request.newBuilder().build());
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
        return primeServerEndPoint;
    }

    // Function to communicate with PrimeServer to check if a number is prime
    private static void checkPrimeServer(String primeServerIp, int primeServerPort) {
        try {
            // Connect to PrimeServer
            channel = ManagedChannelBuilder.forAddress(primeServerIp, primeServerPort)
                    .usePlaintext()
                    .build();
            primeBlockingStub = IsPrimeServiceGrpc.newBlockingStub(channel);

            // Get number input from the user
            Scanner scan = new Scanner(System.in);
            System.out.print("Enter a number to check if it's prime: ");
            int num = scan.nextInt();

            // Make RPC call to PrimeServer
            Result res = primeBlockingStub.isPrime(Number.newBuilder().setNum(num).build());

            // Display result
            if (res.getPrime()) {
                System.out.println(num + " is a prime number.");
            } else {
                System.out.println(num + " is not a prime number.");
            }

        } catch (Exception e) {
            System.out.println("An error occurred");
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }

}
