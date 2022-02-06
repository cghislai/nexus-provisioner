package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NexusGroupRepoModel {

    private NexusRepoFormat format;
    private String name;
    private Boolean online = true;


    private String blobStore = "default";
    private Boolean strictContentValidation = true;
    private WritePolicy writePolicy = WritePolicy.ALLOW;

    private List<String> memeberNames = new ArrayList<>();


}
