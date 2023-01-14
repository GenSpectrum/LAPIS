package ch.ethz.lapis.source;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;


public class NextcladeDatasetTagReader {

    public static String getDatasetTag(Path tagJsonPath) {
        try {
            JSONObject json = (JSONObject) new JSONParser().parse(new FileReader(tagJsonPath.toFile()));
            return (String) json.get("tag");
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
