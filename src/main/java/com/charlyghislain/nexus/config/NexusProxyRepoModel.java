package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NexusProxyRepoModel {

    private NexusRepoFormat format;
    private String name;
    private Boolean online = true;

    private String uri;
    private Integer maxAge = -1;
    private Integer metadataMaxAge = 1440;
    private List<String> cleanupPolicies;

    private String blobStore = "default";
    private Boolean strictContentValidation = true;

    private Boolean negativeCache = true;
    private Integer negativeCacheTTl = 1440;

    private Boolean httpAutoBlock = true;
    private Boolean httpAuthBlocked = false;

    // Raw, maven
    private String contentDisposition = "ATTACHMENT";

    // MAven
    private String mavenVersionPolicy = "MIXED";
    private Boolean mavenLayoutStrict = true;


    // Docker
    private Integer httpPort;
    private Integer httpsPort;
    private Boolean forceBasicAuth;
    private Boolean v1Enabled;


}
