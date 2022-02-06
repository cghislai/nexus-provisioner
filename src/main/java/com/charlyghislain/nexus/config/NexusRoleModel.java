package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class NexusRoleModel {

    private String id;
    private String name;
    private String description;

    private List<String> privileges;
    private List<String> roles;

}
