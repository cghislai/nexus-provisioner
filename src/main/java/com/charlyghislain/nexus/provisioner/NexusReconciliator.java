package com.charlyghislain.nexus.provisioner;

import com.charlyghislain.nexus.api.LdapServer;
import com.charlyghislain.nexus.client.ClientException;
import com.charlyghislain.nexus.client.ClientRuntimeError;
import com.charlyghislain.nexus.client.KubernetesClient;
import com.charlyghislain.nexus.client.NexusClient;
import com.charlyghislain.nexus.config.NexusAnonymousModel;
import com.charlyghislain.nexus.config.NexusConfigModel;
import com.charlyghislain.nexus.config.NexusDeploymentConfigModel;
import com.charlyghislain.nexus.config.NexusEmailModel;
import com.charlyghislain.nexus.config.NexusGroupRepoModel;
import com.charlyghislain.nexus.config.NexusHostedRepoModel;
import com.charlyghislain.nexus.config.NexusLdapServerModel;
import com.charlyghislain.nexus.config.NexusProxyRepoModel;
import com.charlyghislain.nexus.config.NexusRealms;
import com.charlyghislain.nexus.config.NexusRoleModel;
import com.charlyghislain.nexus.config.NexusSecretValueModel;
import com.charlyghislain.nexus.config.NexusUserModel;
import com.charlyghislain.nexus.nexus.ApiEmailConfiguration;
import com.charlyghislain.nexus.nexus.ApiUser;
import com.charlyghislain.nexus.nexus.RepositoryXO;
import com.charlyghislain.nexus.nexus.RoleXORequest;
import com.charlyghislain.nexus.nexus.RoleXOResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NexusReconciliator {
    private final static Logger LOG = Logger.getLogger(NexusReconciliator.class.getName());

    private final NexusConfigModel config;
    private final NexusClient nexusClient;
    private final KubernetesClient kubernetesClient;
    private final Boolean debug;
    private final CollectionReconciliator reconciliator;

    public NexusReconciliator(NexusConfigModel configModel) {
        this.config = configModel;
        this.debug = Optional.ofNullable(config.getDebug())
                .orElse(false);
        this.kubernetesClient = createKuberentesClient();
        this.nexusClient = createNexusClient();

        boolean prune = Optional.ofNullable(config.getPrune()).orElse(false);
        reconciliator = new CollectionReconciliator(prune);
    }


    public void provision() throws ClientException {
        boolean initPassword = Optional.ofNullable(config.getInitAdminPassword())
                .orElse(true);
        String adminPassword;
        if (initPassword) {
            adminPassword = updateAdminPassword();
        } else {
            NexusSecretValueModel adminPassword1 = config.getAdminPassword();
            adminPassword = kubernetesClient.resolveSecretValue(adminPassword1);
        }

        nexusClient.openApiClient(adminPassword);
        try {
            Optional.ofNullable(config.getEmail())
                    .ifPresent(this::updateEmailConfig);

            Optional.ofNullable(config.getLdapServers())
                    .ifPresent(this::updateLdapServers);

            Optional.ofNullable(config.getEnabledRealm())
                    .ifPresent(this::updateRealms);

            Optional.ofNullable(config.getAnonymous())
                    .ifPresent(this::setAnonymous);


            Optional.ofNullable(config.getHostedRespositories())
                    .ifPresent(this::updateHosted);

            Optional.ofNullable(config.getProxyRepositories())
                    .ifPresent(this::updateProxies);


            Optional.ofNullable(config.getGroupRepositories())
                    .ifPresent(this::updateGroups);


            Optional.ofNullable(config.getRoles())
                    .ifPresent(this::updateRoles);

            Optional.ofNullable(config.getUsers())
                    .ifPresent(this::updateUsers);

        } finally {
            nexusClient.closeApiClient();
        }

    }


    private void updateGroups(List<NexusGroupRepoModel> configRepos) {
        try {
            List<RepositoryXO> serverRepos = nexusClient.getGroupRepos();

            NexusRepoConverter repoConverter = new NexusRepoConverter(kubernetesClient, nexusClient, reconciliator);

            reconciliator.reconcileCollectionsAPreferModel(
                    "Group repositories",
                    serverRepos, configRepos,
                    RepositoryXO::getName, NexusGroupRepoModel::getName,
                    nexusClient::createGroup,
                    nexusClient::removeReposiory,
                    nexusClient::updateGroup,
                    repoConverter::patchGroupModel
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to synchronize roles", e);
        }
    }

    private void updateProxies(List<NexusProxyRepoModel> configRepos) {
        try {
            List<RepositoryXO> serverRepos = nexusClient.getProxies();

            NexusRepoConverter repoConverter = new NexusRepoConverter(kubernetesClient, nexusClient, reconciliator);

            reconciliator.reconcileCollectionsAPreferModel(
                    "Proxy repositories",
                    serverRepos, configRepos,
                    RepositoryXO::getName, NexusProxyRepoModel::getName,
                    nexusClient::createProxy,
                    nexusClient::removeReposiory,
                    nexusClient::updateProxy,
                    repoConverter::patchProxyModel
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to synchronize roles", e);
        }
    }

    private void updateHosted(List<NexusHostedRepoModel> configRepos) {
        try {
            List<RepositoryXO> serverRepos = nexusClient.getHosted();

            NexusRepoConverter repoConverter = new NexusRepoConverter(kubernetesClient, nexusClient, reconciliator);

            reconciliator.reconcileCollectionsAPreferModel(
                    "Hosted repositories",
                    serverRepos, configRepos,
                    RepositoryXO::getName, NexusHostedRepoModel::getName,
                    nexusClient::createHosted,
                    nexusClient::removeReposiory,
                    nexusClient::updateHosted,
                    repoConverter::patchHostedModel
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to synchronize roles", e);
        }
    }

    private void updateUsers(List<NexusUserModel> nexusUserModels) {
        try {
            List<ApiUser> serverDefaultRealmusers = nexusClient.getUsers();

            NexusUsersConverter nexusRoleConverter = new NexusUsersConverter(kubernetesClient, reconciliator);

            reconciliator.reconcileCollectionsAPreferModel(
                    "users",
                    serverDefaultRealmusers, nexusUserModels,
                    ApiUser::getUserId, NexusUserModel::getId,
                    m -> {
                        Optional<String> password = Optional.ofNullable(m.getPassword())
                                .map(kubernetesClient::resolveSecretValue);
                        nexusClient.createUser(m, password);
                    },
                    nexusClient::removeUser,
                    m -> {
                        Optional<String> password = Optional.ofNullable(m.getPassword())
                                .map(kubernetesClient::resolveSecretValue);
                        nexusClient.updateUser(m, password);
                    },
                    nexusRoleConverter::patchUser
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to synchronize users", e);
        }
    }

    private void updateRoles(List<NexusRoleModel> nexusRoleModels) {
        try {
            List<RoleXOResponse> serverRoles = nexusClient.getRoles();

            NexusRoleConverter nexusRoleConverter = new NexusRoleConverter(kubernetesClient, reconciliator);

            reconciliator.reconcileCollectionsA(
                    "Roles",
                    serverRoles, nexusRoleModels,
                    RoleXOResponse::getId, NexusRoleModel::getId,
                    nexusClient::createRole,
                    nexusClient::removeRole,
                    nexusClient::updateRole,
                    RoleXORequest::new,
                    nexusRoleConverter::convertResponse,
                    nexusRoleConverter::pathRequest
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to synchronize roles", e);
        }
    }

    private void setAnonymous(NexusAnonymousModel nexusAnonymousModel) {
        try {
            nexusClient.setAnonymous(nexusAnonymousModel);
        } catch (Exception e) {
            throw new RuntimeException("Unable to synchronize realms", e);
        }
    }

    private void updateRealms(List<String> configRealmsNames) {
        try {
            List<String> serverNames = nexusClient.getEnabledRealms();
            List<String> configNames = configRealmsNames.stream()
                    .map(NexusRealms::parseConfigString)
                    .collect(Collectors.toList());
            List<String> newNames = reconciliator.reconcileNames("Realms",
                    serverNames, configNames
            );
            nexusClient.setActiveRealms(newNames);
        } catch (Exception e) {
            throw new RuntimeException("Unable to synchronize realms", e);
        }
    }


    private void updateLdapServers(List<NexusLdapServerModel> ldapServers) {
        try {
            List<LdapServer> serverModelList = nexusClient.getLdaps();

            NexusLdapServierConverter nexusLdapServierConverter = new NexusLdapServierConverter(kubernetesClient, reconciliator);
            reconciliator.reconcileCollectionsBPreferServer(
                    "LDAP server",
                    serverModelList, ldapServers,
                    LdapServer::getName, NexusLdapServerModel::getName,
                    nexusClient::createLdapServer,
                    nexusClient::removeLdapServer,
                    nexusClient::updateLdapServer,
                    LdapServer::new,
                    nexusLdapServierConverter::patchLdapServerModel
            );
        } catch (ClientException e) {
            throw new RuntimeException("Unable to synchronize ldaps", e);
        }
    }

    private void updateEmailConfig(NexusEmailModel mailConfig) throws ClientRuntimeError {
        try {
            ApiEmailConfiguration email = nexusClient.getEmail();
            NexusEmailConverter nexusEmailConverter = new NexusEmailConverter(kubernetesClient);
            nexusEmailConverter.patchEmailconfiguratoin(email, mailConfig);
            nexusClient.setEmail(email);
        } catch (Exception e) {
            throw new ClientRuntimeError("Unable to set email config", e);
        }
    }

    private String updateAdminPassword() {
        try {
            String initialNexusAdminPassword = kubernetesClient.getInitialNexusAdminPassword();


            String adminPassword = kubernetesClient.resolveSecretValue(config.getAdminPassword());

            nexusClient.updateAdminPassword(initialNexusAdminPassword, adminPassword);
            return adminPassword;
        } catch (Exception e) {
            throw new RuntimeException("Unable to set amin password", e);
        }
    }

    @NotNull
    private NexusClient createNexusClient() {
        URI apiUri = Optional.ofNullable(config.getDeployment().getApiUri())
                .filter(s -> !s.isBlank())
                .map(URI::create)
                .orElseThrow(() -> new RuntimeException("No api uri"));
        LOG.log(Level.FINE, "Using nexus api uri " + apiUri);
        NexusClient initialClient = new NexusClient(apiUri, debug);
        return initialClient;
    }


    private KubernetesClient createKuberentesClient() {
        boolean kuberentesEnabled = Optional.ofNullable(config.getKubernetesClientEnabled())
                .orElse(true);
        if (!kuberentesEnabled) {
            return new KubernetesClient();
        }

        NexusDeploymentConfigModel depoymentConfig = Optional.ofNullable(config.getDeployment())
                .orElseThrow(() -> new RuntimeException("No depoyment config"));
        String nexusNamespace = Optional.ofNullable(depoymentConfig.getNamespace())
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new RuntimeException("No namespace"));
        String labelSelector = Optional.ofNullable(depoymentConfig.getLabelSelector())
                .filter(s -> !s.isBlank())
                .orElse("");
        try {
            return new KubernetesClient(nexusNamespace, labelSelector, debug);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create kubernetes client", e);
        }
    }

}
