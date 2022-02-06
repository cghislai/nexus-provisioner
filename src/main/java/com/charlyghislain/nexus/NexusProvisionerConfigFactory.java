package com.charlyghislain.nexus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NexusProvisionerConfigFactory {
    private final static Logger LOG = Logger.getLogger(NexusProvisionerConfigFactory.class.getName());

    public static final String SECRETS_PATH = "/var/run/secrets";

    // List them in colleciton below
    public static final String PROP_DEBUG = "debug";
    public static final String PROP_URI = "uri";
    public static final String PROP_ADMIN_USERNAME = "username";
    public static final String PROP_ADMIN_PASSWORD = "password";
    public static final String PROP_JEKNINS_ACCOUNT_DIRS = "jenkinsaccountsdirs";
    public static final String PROP_NEXUS_NAMESPACE = "nexusnamespace";
    public static final String PROP_NEXUS_LABELSELECTOR = "nexuslabelselector";
    public static final String PROP_MAIL_DOMAIN = "mailDomain";

    public static final String PROP_REPO_NPM = "reponpm";
    public static final String PROP_REPO_RAW = "reporaw";
    public static final String PROP_REPO_RAW_PUBLIC = "reporawpublic";
    public static final String PROP_REPO_HELM = "repohelm";
    public static final String PROP_REPO_DOCKER = "repodocker";
    public static final String PROP_REPO_MAVEN_PROXY_PREVIOUS = "repomavenproxyprevious";
    public static final String PROP_REPO_MAVEN_PROXY_PREVIOUS_URL = "repomavenproxypreviousurl";
    public static final String PROP_REPO_MAVEN_PROXY_REDHAT = "repomavenproxyredhat";
    public static final String PROP_REPO_MAVEN_PROXY_ITEXT = "repomavenproxyitext";
    public static final String PROP_REPO_MAVEN_PROXY_JASPERREPORT = "repomavenproxyjasperreport";
    public static final String PROP_REPO_MAVEN_PROXY_CERFDIGITAL = "repomavenproxycerfdigital";
    public static final String PROP_REPO_MAVEN_PROXY_JBOSSPUBLIC = "repomavenproxyjbosspublic";

    private final static Set<String> allProperties = Set.of(
            PROP_DEBUG, PROP_URI,
            PROP_ADMIN_USERNAME, PROP_ADMIN_PASSWORD,
            PROP_JEKNINS_ACCOUNT_DIRS, PROP_MAIL_DOMAIN,
            PROP_NEXUS_NAMESPACE, PROP_NEXUS_LABELSELECTOR,
            PROP_REPO_NPM, PROP_REPO_RAW, PROP_REPO_RAW_PUBLIC,
            PROP_REPO_HELM, PROP_REPO_DOCKER,
            PROP_REPO_MAVEN_PROXY_PREVIOUS, PROP_REPO_MAVEN_PROXY_PREVIOUS_URL,
            PROP_REPO_MAVEN_PROXY_REDHAT, PROP_REPO_MAVEN_PROXY_ITEXT, PROP_REPO_MAVEN_PROXY_JASPERREPORT,
            PROP_REPO_MAVEN_PROXY_CERFDIGITAL, PROP_REPO_MAVEN_PROXY_JBOSSPUBLIC
    );

    public static NexusProvisionerConfig createConfig(String[] args) {
        NexusProvisionerConfig exportConfig = new NexusProvisionerConfig();

        Map<String, String> properties = new HashMap<>();
        loadPropertiesFromEnv(properties);
        loadPropertiesFromSecrets(properties);
        loadPropertiesFromArguments(properties, args);

        String uri = Optional.ofNullable(properties.get(PROP_URI))
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new RuntimeException("No api uri"));
        URI apiUri;
        try {
            apiUri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid uri", e);
        }
        exportConfig.setNexusUri(apiUri);

        String user = Optional.ofNullable(properties.get(PROP_ADMIN_USERNAME))
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new RuntimeException("No admin username"));
        exportConfig.setAdminUser(user);

        String adminPassword = Optional.ofNullable(properties.get(PROP_ADMIN_PASSWORD))
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new RuntimeException("No admin password"));
        exportConfig.setAdminPassword(adminPassword);


        Map<String, String> jenkinsAccountsMap = Optional.ofNullable(properties.get(PROP_JEKNINS_ACCOUNT_DIRS))
                .filter(s -> !s.isBlank())
                .map(s -> s.split(";"))
                .stream()
                .flatMap(Arrays::stream)
                .map(Paths::get)
                .map(NexusProvisionerConfigFactory::getJenkinsAccountsMap)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        exportConfig.setJenkinsRoleAccounts(jenkinsAccountsMap);

        String nexusNamespace = Optional.ofNullable(properties.get(PROP_NEXUS_NAMESPACE))
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new RuntimeException("No neuxs namespace"));
        exportConfig.setNexusNamespace(nexusNamespace);


        String nexusApplabel = Optional.ofNullable(properties.get(PROP_NEXUS_LABELSELECTOR))
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new RuntimeException("No neuxs label selector"));
        exportConfig.setNexusLabelSelector(nexusApplabel);


        boolean debug = Optional.ofNullable(properties.get(PROP_DEBUG))
                .map(Boolean::parseBoolean)
                .orElse(false);
        exportConfig.setDebug(debug);


        Optional.ofNullable(properties.get(PROP_REPO_NPM))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoNpm, () -> exportConfig.setRepoNpm(true));

        Optional.ofNullable(properties.get(PROP_REPO_RAW))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoRaw, () -> exportConfig.setRepoRaw(true));

        Optional.ofNullable(properties.get(PROP_REPO_RAW_PUBLIC))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoRawPublic, () -> exportConfig.setRepoRawPublic(false));

        Optional.ofNullable(properties.get(PROP_REPO_HELM))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoHelm, () -> exportConfig.setRepoHelm(true));

        Optional.ofNullable(properties.get(PROP_REPO_DOCKER))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoDocker, () -> exportConfig.setRepoDocker(false));

        Optional.ofNullable(properties.get(PROP_REPO_MAVEN_PROXY_PREVIOUS))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoMavenProxyPrevious, () -> exportConfig.setRepoMavenProxyPrevious(true));

        Optional.ofNullable(properties.get(PROP_REPO_MAVEN_PROXY_PREVIOUS_URL))
                .ifPresentOrElse(exportConfig::setMavenPreviousProxyUrl, () -> exportConfig.setMavenPreviousProxyUrl(null));

        Optional.ofNullable(properties.get(PROP_REPO_MAVEN_PROXY_REDHAT))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoMavenProxyRedHat, () -> exportConfig.setRepoMavenProxyRedHat(true));

        Optional.ofNullable(properties.get(PROP_REPO_MAVEN_PROXY_JASPERREPORT))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoMavenProxyJasperReport, () -> exportConfig.setRepoMavenProxyJasperReport(true));

        Optional.ofNullable(properties.get(PROP_REPO_MAVEN_PROXY_CERFDIGITAL))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoMavenProxyCefDigital, () -> exportConfig.setRepoMavenProxyCefDigital(true));

        Optional.ofNullable(properties.get(PROP_REPO_MAVEN_PROXY_JBOSSPUBLIC))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoMavenProxyJbossPublic, () -> exportConfig.setRepoMavenProxyJbossPublic(true));


        Optional.ofNullable(properties.get(PROP_REPO_MAVEN_PROXY_ITEXT))
                .map(Boolean::parseBoolean)
                .ifPresentOrElse(exportConfig::setRepoMavenProxyItext, () -> exportConfig.setRepoMavenProxyItext(true));

        Optional.ofNullable(properties.get(PROP_MAIL_DOMAIN))
                .ifPresentOrElse(exportConfig::setMainDomain, () -> exportConfig.setMainDomain("nexus.local"));


        return exportConfig;
    }

    private static Map<String, String> getJenkinsAccountsMap(Path basePath) {
        try {
            return Files.list(basePath)
                    .filter(p -> Files.isReadable(p) && Files.isRegularFile(p))
                    .collect(Collectors.toMap(
                            p -> p.getFileName().toString(),
                            NexusProvisionerConfigFactory::getFileCOntent
                    ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFileCOntent(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadPropertiesFromArguments(Map<String, String> properties, String[] args) {
        Arrays.asList(args).stream()
                .forEach(a -> loadPropertyFromArg(properties, a));
    }

    private static void loadPropertyFromArg(Map<String, String> properties, String arg) {
        String[] argParts = arg.split("=");
        if (argParts.length == 2) {
            String key = argParts[0];
            String value = argParts[1];
            if (allProperties.contains(key)) {
                loadProperty(properties, key, value);
            }
        }
    }

    private static void loadPropertiesFromSecrets(Map<String, String> properties) {
        allProperties.stream()
                .forEach(k -> loadPropertySecret(properties, k));
    }

    private static void loadPropertySecret(Map<String, String> properties, String propName) {
        Path secretPath = getSecretPath(propName);
        if (Files.exists(secretPath) && Files.isReadable(secretPath)) {
            try {
                List<String> allLines = Files.readAllLines(secretPath);
                if (allLines.size() > 0) {
                    String secretLine = allLines.get(0);
                    loadProperty(properties, propName, secretLine);
                }
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Unable to loac secret at " + secretPath, e);
            }
        }
    }

    private static Path getSecretPath(String propName) {
        return Paths.get(SECRETS_PATH).resolve(propName);
    }

    private static void loadPropertiesFromEnv(Map<String, String> properties) {
        allProperties.forEach(p -> loadProperty(properties, p, System.getenv(p)));
    }

    private static void loadProperty(Map<String, String> properties, String propName, String propValueNullable) {
        Optional.ofNullable(propValueNullable)
                .ifPresent(v -> properties.put(propName, v));
    }
}
