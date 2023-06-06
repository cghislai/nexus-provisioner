package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NexusCertificateModel {
    private NexusSecretValueModel publicKeyPem;
}
