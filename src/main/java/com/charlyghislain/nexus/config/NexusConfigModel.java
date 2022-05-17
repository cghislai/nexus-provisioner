package com.charlyghislain.nexus.config;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class NexusConfigModel {

    private Boolean debug;
    private Boolean prune;
    private Boolean kubernetesClientEnabled;

    @NotNull
    private NexusDeploymentConfigModel deployment;

    private Boolean initAdminPassword;
    private NexusSecretValueModel adminPassword;
    private NexusEmailModel email;
    private List<NexusLdapServerModel> ldapServers;

    private List<String> enabledRealm;
    private NexusAnonymousModel anonymous;
    private List<NexusRoleModel> roles;
    private List<NexusUserModel> users;

    private List<NexusHostedRepoModel> hostedRepositories;
    private List<NexusProxyRepoModel> proxyRepositories;
    private List<NexusGroupRepoModel> groupRepositories;


}
