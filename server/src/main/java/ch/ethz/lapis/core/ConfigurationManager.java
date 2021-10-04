package ch.ethz.lapis.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;


public class ConfigurationManager {

    private static final String ENV_PREFIX = "HARVESTER_";

    /**
     * Loads the configuration from both the config file and from environment variables
     */
    public <T extends Config> T loadConfiguration(Path configPath, Class<T> configClass, String programName)
        throws IOException {
        Map<String, ?> fileConfigMaps1 = readConfigMapsFromYaml(configPath);
        Map<String, ?> envConfigMaps1 = readConfigMapFromEnvironmentVariables();

        // Turn the program name keys to lower case. The ObjectMapper will take care of the "case-insensitivity-ness"
        // of everything else.
        programName = programName.toLowerCase();
        Map<String, Object> fileConfigMaps = new HashMap<>();
        Map<String, Object> envConfigMaps = new HashMap<>();
        fileConfigMaps1.forEach((key, value) -> fileConfigMaps.put(key.toLowerCase(), value));
        envConfigMaps1.forEach((key, value) -> envConfigMaps.put(key.toLowerCase(), value));

        Map<String, ?> fileDefaultConfig = (Map<String, ?>) fileConfigMaps.get("default");
        Map<String, ?> fileProgramConfig = (Map<String, ?>) fileConfigMaps.get(programName);
        Map<String, ?> envDefaultConfig = (Map<String, ?>) envConfigMaps.get("default");
        Map<String, ?> envProgramConfig = (Map<String, ?>) envConfigMaps.get(programName);

        Map merged = Utils.nullableMapDeepMerge(fileDefaultConfig, envDefaultConfig);
        merged = Utils.nullableMapDeepMerge(merged, fileProgramConfig);
        merged = Utils.nullableMapDeepMerge(merged, envProgramConfig);

        ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        T mergedConfig = objectMapper.convertValue(merged, configClass);
        return mergedConfig;
    }

    /**
     * Loads the configuration only from environment variables
     */
    public <T extends Config> T loadConfiguration(Class<T> configClass, String programName) {
        Map<String, ?> envConfigMaps1 = readConfigMapFromEnvironmentVariables();

        // Turn the program name keys to lower case. The ObjectMapper will take care of the "case-insensitivity-ness"
        // of everything else.
        programName = programName.toLowerCase();
        Map<String, Object> envConfigMaps = new HashMap<>();
        envConfigMaps1.forEach((key, value) -> envConfigMaps.put(key.toLowerCase(), value));

        Map<String, ?> envDefaultConfig = (Map<String, ?>) envConfigMaps.get("default");
        Map<String, ?> envProgramConfig = (Map<String, ?>) envConfigMaps.get(programName);

        Map merged = Utils.nullableMapDeepMerge(envDefaultConfig, envProgramConfig);

        ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        T mergedConfig = objectMapper.convertValue(merged, configClass);
        return mergedConfig;
    }

    private Map<String, ?> readConfigMapsFromYaml(Path configPath)
        throws IOException {
        Yaml yaml = new Yaml();
        return yaml.load(Files.readString(configPath));
    }

    private Map<String, ?> readConfigMapFromEnvironmentVariables() {
        Map<String, String> envs = System.getenv();
        Map configMaps = new HashMap<>();
        envs.forEach((prefixedKey, value) -> {
            if (!prefixedKey.startsWith(ENV_PREFIX)) {
                return;
            }
            String key = prefixedKey.substring(ENV_PREFIX.length());
            String[] keyParts = key.split("_");
            Map curMap = configMaps;
            for (int i = 0; i < keyParts.length; i++) {
                String keyPart = keyParts[i];
                if (i < keyParts.length - 1) { // Not the last level
                    curMap.putIfAbsent(keyPart, new HashMap());
                    curMap = (Map) curMap.get(keyPart);
                } else { // The last leveL: assign the actual value
                    curMap.put(keyPart, value);
                }
            }
        });
        return configMaps;
    }
}
