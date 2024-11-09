package serverapp;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class DockerUtil {

    private static final Logger logger = Logger.getLogger(DockerUtil.class.getName());

    // Returns true if the number is prime, false otherwise
    public void launchPrimeCheckContainer(long number, int redisPort) {
        // arg0 : windows: tcp://localhost:2375 linux: unix:///var/run/docker.sock
        // arg1 : container name
        // arg2 : image name
        // arg3 : number to calculate if it is prime
        // arg4 : Redis Host IP
        // arg5 : Redis port on host
        try {
            String HOST_URI = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "tcp://localhost:2375"
                    : "unix:///var/run/docker.sock";

            String containerName = "IsPrimeDocker";
            String imageName = "isPrime";
            String redisHostIP = "host.docker.internal";

            List<String> command = new ArrayList<>();

            command.add(String.valueOf(number));
            command.add(redisHostIP);
            command.add(String.valueOf(redisPort));

            System.out.println("redisPort: " + redisPort);

            try (DockerClient dockerclient = DockerClientBuilder
                    .getInstance()
                    .withDockerHttpClient(
                            new ApacheDockerHttpClient.Builder()
                                    .dockerHost(URI.create(HOST_URI)).build()
                    )
                    .build()) {
                CreateContainerResponse containerResponse = dockerclient
                        .createContainerCmd(imageName)
                        .withName(containerName)
                        .withCmd(command)
                        .exec();
                System.out.println("ID:" + containerResponse.getId());
                dockerclient.startContainerCmd(containerResponse.getId()).exec();

                for (; ; ) {
                    InspectContainerResponse inspResp = dockerclient
                            .inspectContainerCmd(containerName).exec();
                    System.out.println("Container Status: " + inspResp.getState().getStatus());

                    if (Objects.equals(inspResp.getState().getStatus(), "exited"))
                        break;
                    Thread.sleep(1000);
                }

                // if container is running
                // dockerclient.killContainerCmd(containerName).exec();
                // remove container
                dockerclient.removeContainerCmd(containerName).exec();
            }
        } catch (Exception ex) {
            logger.severe("Exception in function 'launchPrimeCheckContainer' at DockerUtil: " + ex.getMessage());
        }

        System.out.println("Launching Docker container to check if " + number + " is prime...");
    }
}

