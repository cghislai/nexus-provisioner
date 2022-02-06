package com.charlyghislain.nexus.config;

import com.charlyghislain.nexus.client.NexusClient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NexusAnonymousModel {

    private Boolean enabled = true;
    private String userId = NexusClient.NX_ANONYMOUS_USER;
    private String realmName = NexusRealms.LOCAL_AUTHORIZING.getNxName();

}
