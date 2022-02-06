package com.charlyghislain.nexus.config;

import com.charlyghislain.nexus.client.ClientRuntimeError;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigParser {
    private final static String CONFIG_LOCATION_PROPERTY = "NEXUS_PROVISIONER_CONFIG";

    private final static Logger LOG = Logger.getLogger(ConfigParser.class.getName());
    private final ObjectMapper mapper;

    public ConfigParser() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        this.mapper.findAndRegisterModules();
        this.mapper.enable(
                        JsonParser.Feature.ALLOW_COMMENTS,
                        JsonParser.Feature.ALLOW_SINGLE_QUOTES,
                        JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
                        JsonParser.Feature.ALLOW_YAML_COMMENTS
                )
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public NexusConfigModel createConfig() {
        String configLocationFromEnv = System.getenv(CONFIG_LOCATION_PROPERTY);
        String configPathString = Optional.ofNullable(configLocationFromEnv)
                .orElse("/var/run/nexus-provisioner.yaml");
        LOG.log(Level.INFO, "Loading configuration from " + configPathString + " (env: " + CONFIG_LOCATION_PROPERTY + ")");

        Path configFromEnvPath = Paths.get(configPathString);
        if (Files.exists(configFromEnvPath)) {
            try {
                InputStream inputStream = Files.newInputStream(configFromEnvPath);
                NexusConfigModel configModel = mapper.reader().readValue(inputStream, NexusConfigModel.class);
                return configModel;
            } catch (IOException e) {
                throw new ClientRuntimeError("Unable to load config at " + configLocationFromEnv, e);
            }
        }
        throw new ClientRuntimeError("No config file provided");
    }

    public void dumpConfig(NexusConfigModel config) {
        try {
            String configString = mapper.writer().writeValueAsString(config);
            LOG.log(Level.FINE, "Loaded config:");
            LOG.log(Level.FINE, configString);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
