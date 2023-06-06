package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NexusSecretValueModel {

    private String secretName;
    private String secretKey;

    /**
     * When unspecified, will use the deployment namespace
     */
    private String secretNamespace;
    /**
     * Clear text value to use when specified. Will ignore secrets reading
     */
    private String clearText;

    /**
     * If not exists, will be provisionned with a random value.
     */
    private Boolean generated;

}
