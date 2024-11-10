package serverapp;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DockerUtil {

    private static final Logger logger = Logger.getLogger(DockerUtil.class.getName());

    // Launch the Docker container, save the result in Redis
    public void launchPrimeCheckContainer(long number, int redisPort) {
        try {
            String HOST_URI = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "tcp://localhost:2375"
                    : "unix:///var/run/docker.sock";

            String containerName = "IsPrimeDocker";
            String imageName = "isprime";
            String redisHostIP = "172.17.0.1";

            List<String> command = new ArrayList<>();

            command.add(String.valueOf(number));
            command.add(redisHostIP);
            command.add(String.valueOf(redisPort));

            System.out.println("Launching Docker container to check if " + number + " is prime...");

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
                dockerclient.startContainerCmd(containerResponse.getId()).exec();

                // wait for container to finish
                for(;;) {
                    InspectContainerResponse inspResp = dockerclient
                            .inspectContainerCmd(containerName).exec();
                    if ("exited".equals(inspResp.getState().getStatus())) break;
                    Thread.sleep(1000);
                }

                // remove container
                dockerclient.removeContainerCmd(containerName).exec();
            }
        } catch (Exception ex) {
            logger.severe("Exception in function 'launchPrimeCheckContainer' at DockerUtil: " + ex.getMessage());
        }
    }
}

