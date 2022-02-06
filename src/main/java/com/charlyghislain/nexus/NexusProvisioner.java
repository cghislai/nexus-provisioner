package com.charlyghislain.nexus;

import com.charlyghislain.nexus.config.ConfigParser;
import com.charlyghislain.nexus.config.NexusConfigModel;
import com.charlyghislain.nexus.provisioner.NexusReconciliator;

import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class NexusProvisioner {
    private final static Logger LOG = Logger.getLogger(NexusProvisioner.class.getName());

    public static void main(String[] args) {
        ConfigParser configParser = new ConfigParser();
        NexusConfigModel config = configParser.createConfig();

        boolean debug = Optional.ofNullable(config.getDebug()).orElse(false);
        tryReadLoggingConfig(debug);
        if (debug) {
            configParser.dumpConfig(config);
        }

        try {
            NexusReconciliator reconciliator = new NexusReconciliator(config);
            reconciliator.provision();
        } catch (Exception e) {
            throw new RuntimeException("Unable to provision nexus", e);
        }
    }

    private static void tryReadLoggingConfig(boolean debug) {
        try {
            String loggingPropertiesFileName = debug ? "logging.debug.properties" : "logging.properties";
            InputStream logPropsFile = NexusProvisioner.class.getClassLoader().getResourceAsStream(loggingPropertiesFileName);
            LogManager.getLogManager().readConfiguration(logPropsFile);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to load logging config");
        }
    }
}
