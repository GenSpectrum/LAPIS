package ch.ethz.lapis.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ch.ethz.lapis.LapisConfig;
import ch.ethz.lapis.api.entity.OpennessLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

class ConfigurationManagerTest {

    private ConfigurationManager configurationManager;

    @BeforeEach
    void setup() {
        configurationManager = new ConfigurationManager();
    }

    @Test
    void readConfiguration() throws IOException, URISyntaxException {
        LapisConfig lapisConfig = configurationManager.loadConfiguration(
            Path.of(getClass().getResource("/config.test.yml").toURI())
        );

        assertThat(lapisConfig.getVineyard().getHost(), is("<db host>"));
        assertThat(lapisConfig.getVineyard().getPort(), is(123));
        assertThat(lapisConfig.getVineyard().getDbname(), is("<db name>"));
        assertThat(lapisConfig.getVineyard().getUsername(), is("<db user name>"));
        assertThat(lapisConfig.getVineyard().getPassword(), is("<db user password>"));
        assertThat(lapisConfig.getVineyard().getSchema(), is("<db schema>"));
        assertThat(lapisConfig.getApiOpennessLevel(), is(OpennessLevel.GISAID));
        assertThat(lapisConfig.getCacheEnabled(), is(false));
        assertThat(lapisConfig.getRedisHost(), is("<redis host>"));
        assertThat(lapisConfig.getRedisPort(), is(456));
    }
}
