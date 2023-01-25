package ch.ethz.lapis.core;

import ch.ethz.lapis.LapisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Path;


public class ConfigurationManager {

    private final ObjectMapper objectMapper;

    public ConfigurationManager() {
        objectMapper = new ObjectMapper(new YAMLFactory());
    }

    public LapisConfig loadConfiguration(Path configPath) throws IOException {
        return objectMapper.readValue(configPath.toFile(), LapisConfig.class);
    }
}
