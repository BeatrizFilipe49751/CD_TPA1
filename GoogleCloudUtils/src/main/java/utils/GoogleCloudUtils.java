package utils;

import com.google.auth.oauth2.GoogleCredentials;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class GoogleCloudUtils {

    private static String getIp(String projectId, String zone, String instanceName) throws Exception {
        HttpURLConnection con = getConnection(projectId, zone, instanceName);

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonObject = new JSONObject(response.toString());
            return jsonObject.getJSONArray("networkInterfaces")
                    .getJSONObject(0)
                    .getJSONArray("accessConfigs")
                    .getJSONObject(0)
                    .getString("natIP");
        } else {
            // Read the error response
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            String inputLine;
            StringBuilder errorResponse = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                errorResponse.append(inputLine);
            }
            in.close();
            throw new Exception("GET request failed: " + responseCode + " - " + errorResponse);
        }
    }

    private static HttpURLConnection getConnection(String projectId, String zone, String instanceName) throws Exception {
        String accessToken = getAccessToken(); // An OAuth 2.0 access token

        String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s", projectId, zone, instanceName);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + accessToken);
        return con;
    }

    private static String getAccessToken() throws Exception {
        // Load the JSON key file from the resources
        InputStream keyFileStream = GoogleCloudUtils.class.getClassLoader().getResourceAsStream("cd2425d-g09-7ad8b28d211d.json");

        if (keyFileStream == null) {
            throw new FileNotFoundException("Resource not found: cd2425d-g09-7ad8b28d211d.json");
        }

        // Load credentials from the key file
        GoogleCredentials credentials = GoogleCredentials.fromStream(keyFileStream)
                .createScoped("https://www.googleapis.com/auth/cloud-platform");

        // Refresh the access token
        return credentials.refreshAccessToken().getTokenValue();
    }

    public static String getExternalIp() throws Exception {
        // URL for getting the external IP from the metadata server
        String metadataUrl = "http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip";

        HttpURLConnection con = (HttpURLConnection) new URL(metadataUrl).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Metadata-Flavor", "Google"); // Required header for metadata server access

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                return in.readLine(); // Should return the external IP as a single line
            }
        } else {
            throw new Exception("Failed to get external IP: " + responseCode);
        }
    }

    public static String getRingManagerIp() throws Exception {
        String projectId = "cd2425d-g09";
        String zone = "europe-southwest1-c";
        String instanceName = "ring-manager";
        return getIp(projectId, zone, instanceName);
    }

    public static int getRingManagerPort() {
        return 8500;
    }
}
