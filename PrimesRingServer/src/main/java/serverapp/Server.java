package serverapp;

/*
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;


public class Server extends CalcServiceGrpc.CalcServiceImplBase {

    private static int svcPort = 8500;

    public static void main(String[] args) {
        try {
            if (args.length > 0) svcPort = Integer.parseInt(args[0]);
            io.grpc.Server svc = ServerBuilder
                .forPort(svcPort)
                .addService(new Server())
                .build();
            svc.start();
            System.out.println("Server started, listening on " + svcPort);
            
            svc.awaitTermination();
            svc.shutdown();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void add(AddOperands request, StreamObserver<Result> responseObserver) {
        // not implemented
    }

    @Override
    public void generatePowers(NumberAndMaxExponent request, StreamObserver<Result> responseObserver) {
        // not implemented
    }

    @Override
    public StreamObserver<Number> addSeqOfNumbers(StreamObserver<Result> responseObserver) {
       // not implemented
        return null;

    }

    @Override
    public StreamObserver<AddOperands> multipleAdd(StreamObserver<Result> responseObserver) {
       // not implemented
        return null;

    }
}
 */