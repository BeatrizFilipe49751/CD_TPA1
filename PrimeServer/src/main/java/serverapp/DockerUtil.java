package serverapp;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class DockerUtil {
    // Returns true if the number is prime, false otherwise
    public void launchPrimeCheckContainer(long number, int redisPort) {
        // arg0 : windows: tcp://localhost:2375 linux: unix:///var/run/docker.sock
        // arg1 : container name
        // arg2 : image name
        // arg3 : number to calculate if it is prime
        // arg4 : Redis Host IP
        //arg 5: Redis port on host
        try {
            String HOST_URI = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "tcp://localhost:2375"
                    : "unix:///var/run/docker.sock";
            String containerName = "IsPrimeDocker";
            String imageName = "isprime";
            String redisHostIP = "host.docker.internal";

            List<String> command = new ArrayList<>();

            command.add(String.valueOf(number));
            command.add(redisHostIP);
            command.add(String.valueOf(redisPort));

            System.out.println("redisPort: " + redisPort);

            DockerClient dockerclient = DockerClientBuilder
                    .getInstance()
                    .withDockerHttpClient(
                            new ApacheDockerHttpClient.Builder()
                                    .dockerHost(URI.create(HOST_URI)).build()
                    )
                    .build();
            CreateContainerResponse containerResponse = dockerclient
                    .createContainerCmd(imageName)
                    .withName(containerName)
                    .withCmd(command)
                    .exec();
            System.out.println("ID:" + containerResponse.getId());
            dockerclient.startContainerCmd(containerResponse.getId()).exec();

            for(;;) {
                InspectContainerResponse inspResp = dockerclient
                        .inspectContainerCmd(containerName).exec();
                System.out.println("Container Status: " + inspResp.getState().getStatus());
                if (inspResp.getState().getStatus().equals("exited")) break;
                Thread.sleep(1*1000);
            }


            // if container is running
            // dockerclient.killContainerCmd(containerName).exec();
            // remove container
            dockerclient.removeContainerCmd(containerName).exec();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("Launching Docker container to check if " + number + " is prime...");
    }
}

