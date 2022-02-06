package com.charlyghislain.nexus.config;

import lombok.Getter;

public enum NexusRepoFormat {
    NPM("npm"),
    RAW("raw"),
    HELM("helm"),
    DOCKER("docker"),
    MAVEN("maven2"),

    ;

    @Getter
    private final String value;

    NexusRepoFormat(String value) {

        this.value = value;
    }
}
