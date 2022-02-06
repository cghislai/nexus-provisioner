package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NexusEmailModel {
    private Boolean enabled = false;
    private String host;
    private Integer port = 25;
    private String username;
    private NexusSecretValueModel password;
    private String from;
    private String subjectPrefix;
    private Boolean startTls;
    private Boolean startTlsRequired;
    private Boolean ssl;
    private Boolean sslServerCheck;
    private Boolean nexusTrustStore;
}

