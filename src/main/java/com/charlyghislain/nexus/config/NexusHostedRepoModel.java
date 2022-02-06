package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NexusHostedRepoModel {

    private NexusRepoFormat format;
    private String name;
    private Boolean online = true;

    private String blobStore = "default";
    private Boolean strictContentValidation = true;
    private WritePolicy writePolicy = WritePolicy.ALLOW;

    private List<String> cleanupPolicies = new ArrayList<>();

    // MAven
    private String mavenVersionPolicy;
    private Boolean mavenLayoutStrict = true;

    // Raw, maven
    private String contentDisposition = "ATTACHMENT";

    // Docker
    private Integer httpPort;
    private Integer httpsPort;
    private Boolean forceBasicAuth;
    private Boolean v1Enabled;


}
