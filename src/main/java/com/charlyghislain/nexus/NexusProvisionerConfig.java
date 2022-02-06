package com.charlyghislain.nexus;

import lombok.Data;

import java.net.URI;
import java.util.Map;

@Data
public class NexusProvisionerConfig {

    private String nexusNamespace;
    private String nexusLabelSelector;

    private URI nexusUri;
    private String adminUser;
    private String adminPassword;

    private String mainDomain;
    private Map<String, String> jenkinsRoleAccounts;

    private boolean debug;

    private boolean repoNpm;
    private boolean repoRaw;
    private boolean repoRawPublic;
    private boolean repoHelm;
    private boolean repoMavenProxyPrevious;
    private boolean repoMavenProxyRedHat;
    private boolean repoMavenProxyItext;
    private boolean repoMavenProxyJasperReport;
    private boolean repoMavenProxyCefDigital;
    private boolean repoMavenProxyJbossPublic;
    private boolean repoDocker;
    private String mavenPreviousProxyUrl;

}
