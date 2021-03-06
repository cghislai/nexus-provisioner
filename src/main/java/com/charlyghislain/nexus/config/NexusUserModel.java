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
    /**
     * active | locked | disabled | changepassword
     */
    private String status;

    private List<String> roles;

}
