package ch.ethz.lapis.source.gisaid;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class SubmitterInformationFetcher {

    public static enum SubmitterInformationFetchingStatus {
        SUCCESSFUL, NOT_FOUND, TOO_MANY_REQUESTS, UNEXPECTED_ERROR
    }

    public record SubmitterInformationFetchingResult(
        SubmitterInformationFetchingStatus status,
        SubmitterInformation value
    ) {
    }

    public SubmitterInformationFetchingResult fetchSubmitterInformation(String gisaidEpiIsl) {
        HttpURLConnection conn = null;
        try {
            String accessionNumber = gisaidEpiIsl.split("_")[2];
            int l = accessionNumber.length();
            String urlStr = "https://www.epicov.org/acknowledgement/" + accessionNumber.substring(l - 4, l - 2)
                + "/" + accessionNumber.substring(l - 2, l)
                + "/" + gisaidEpiIsl + ".json";
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case 200:
                    String jsonString = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);
                    JSONObject json = (JSONObject) new JSONParser().parse(jsonString);
                    String jsonO = (String) json.get("covv_orig_lab");
                    String jsonS = (String) json.get("covv_subm_lab");
                    String jsonA = (String) json.get("covv_authors");
                    SubmitterInformation result = new SubmitterInformation()
                        .setOriginatingLab("na".equalsIgnoreCase(jsonO) ? null : jsonO)
                        .setSubmittingLab("na".equalsIgnoreCase(jsonS) ? null : jsonS)
                        .setAuthors("na".equalsIgnoreCase(jsonA) ? null : jsonA);
                    return new SubmitterInformationFetchingResult(
                        SubmitterInformationFetchingStatus.SUCCESSFUL,
                        result
                    );
                case 404:
                    System.err.println("Not found: " + urlStr);
                    return new SubmitterInformationFetchingResult(SubmitterInformationFetchingStatus.NOT_FOUND, null);
                case 429:
                    return new SubmitterInformationFetchingResult(SubmitterInformationFetchingStatus.TOO_MANY_REQUESTS, null);
                default:
                    System.err.println("Unexpected HTTP response code: " + responseCode);
                    return new SubmitterInformationFetchingResult(SubmitterInformationFetchingStatus.UNEXPECTED_ERROR, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SubmitterInformationFetchingResult(SubmitterInformationFetchingStatus.UNEXPECTED_ERROR, null);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
