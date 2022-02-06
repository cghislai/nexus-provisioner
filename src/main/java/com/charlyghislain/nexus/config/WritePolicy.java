package com.charlyghislain.nexus.config;

import lombok.Getter;

public enum WritePolicy {
    ALLOW(String.valueOf("allow")), ALLOW_ONCE(String.valueOf("allow_once")), DENY(String.valueOf("deny"));

    @Getter
    private final String value;

    WritePolicy(String value) {

        this.value = value;
    }
}
