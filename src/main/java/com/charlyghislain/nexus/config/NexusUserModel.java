package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class NexusUserModel {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String source = "default";
    private NexusSecretValueModel password;
    /**
     * active | locked | disabled | changepassword
     */
    private String status;

    private Set<String> roles;

}
