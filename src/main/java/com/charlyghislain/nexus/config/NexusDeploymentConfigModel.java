package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NexusDeploymentConfigModel {

    // Used to read initial admin secret using kube api
    private String namespace;
    // Used to read initial admin secret using kube api
    private String labelSelector;
    // The rest api endpoint
    private String apiUri;


}
