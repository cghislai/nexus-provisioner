package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NexusUserModel {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String source = "default";
    private NexusSecretValueModel password;
    private Boolean active;

    private List<String> roles;

}
