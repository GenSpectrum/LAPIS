package ch.ethz.lapis.source.gisaid;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class SubmitterInformationFetcher {

    public Optional<SubmitterInformation> fetchSubmitterInformation(String gisaidEpiIsl) {
        try {
            String accessionNumber = gisaidEpiIsl.split("_")[2];
            int l = accessionNumber.length();
            String url = "https://www.epicov.org/acknowledgement/" + accessionNumber.substring(l - 4, l - 2)
                + "/" + accessionNumber.substring(l - 2, l)
                + "/" + gisaidEpiIsl + ".json";
            String jsonString = IOUtils.toString(new URL(url).openStream(), StandardCharsets.UTF_8);
            JSONObject json = (JSONObject) new JSONParser().parse(jsonString);
            String jsonO = (String) json.get("covv_orig_lab");
            String jsonS = (String) json.get("covv_subm_lab");
            String jsonA = (String) json.get("covv_authors");
            SubmitterInformation result = new SubmitterInformation()
                .setOriginatingLab("na".equalsIgnoreCase(jsonO) ? null : jsonO)
                .setSubmittingLab("na".equalsIgnoreCase(jsonS) ? null : jsonS)
                .setAuthors("na".equalsIgnoreCase(jsonA) ? null : jsonA);
            return Optional.of(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
