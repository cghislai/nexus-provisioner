package com.charlyghislain.nexus.config;

import lombok.Getter;

import java.util.Arrays;

public enum NexusRealms {
    LOCAL_AUTHORIZING("NexusAuthorizingRealm"),
    LOCAL_AUTHENTICATING("NexusAuthenticatingRealm"),
    NPM_TOKEN("NpmToken"),
    DOCKER_TOKEN("DockerToken"),
    CONAN_TOKEN("ConanToken"),
    LDAP("LdapRealm"),
    NUGET_API_KEY("NuGetApiKey"),
    RUT_AUTH("rutauth-realm"),
    DEFAULT_ROLE("DefaultRole"),
    ;

    @Getter
    private final String nxName;

    NexusRealms(String nxName) {
        this.nxName = nxName;
    }

    public static String parseConfigString(String value) {
        return Arrays.stream(NexusRealms.values())
                .filter(s -> s.getNxName().equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value))
                .findAny()
                .map(NexusRealms::getNxName)
                .orElse(value);
    }
}
