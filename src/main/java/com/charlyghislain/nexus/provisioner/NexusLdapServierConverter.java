package com.charlyghislain.nexus.provisioner;

import com.charlyghislain.nexus.api.LdapServer;
import com.charlyghislain.nexus.client.KubernetesClient;
import com.charlyghislain.nexus.config.NexusLdapServerModel;

import java.util.Optional;

public class NexusLdapServierConverter {

    private final KubernetesClient kubernetesClient;

    public NexusLdapServierConverter(KubernetesClient kubernetesClient, CollectionReconciliator reconciliator) {
        this.kubernetesClient = kubernetesClient;
    }

    public void patchLdapServerModel(LdapServer serverModel, NexusLdapServerModel configModel) {
        Optional.ofNullable(configModel.getName())
                .ifPresent(serverModel::setName);

        Optional.ofNullable(configModel.getLdaps())
                .map(s -> s ? "LDAPS" : "LDAP")
                .ifPresent(serverModel::setProtocol);

        Optional.ofNullable(configModel.getUseTrustStore())
                .ifPresent(serverModel::setUseTrustStore);

        Optional.ofNullable(configModel.getHost())
                .ifPresent(serverModel::setHost);

        Optional.ofNullable(configModel.getPort())
                .ifPresent(serverModel::setPort);

        Optional.ofNullable(configModel.getSearchBaseDn())
                .ifPresent(serverModel::setSearchBase);

        Optional.ofNullable(configModel.getAuthScheme())
                .ifPresent(serverModel::setAuthScheme);

        Optional.ofNullable(configModel.getAuthUsername())
                .ifPresent(serverModel::setAuthUsername);

        Optional.ofNullable(configModel.getAuthPassword())
                .map(kubernetesClient::resolveSecretValue)
                .ifPresent(serverModel::setAuthPassword);

        Optional.ofNullable(configModel.getConnectionTimeoutSeconds())
                .ifPresent(serverModel::setConnectionTimeoutSeconds);

        Optional.ofNullable(configModel.getConnectionRetrySeconds())
                .ifPresent(serverModel::setConnectionRetryDelaySeconds);

        Optional.ofNullable(configModel.getMaxIncident())
                .ifPresent(serverModel::setMaxIncidentsCount);

        Optional.ofNullable(configModel.getUserBaseDn())
                .ifPresent(serverModel::setUserBaseDn);

        Optional.ofNullable(configModel.getUserSubtree())
                .ifPresent(serverModel::setUserSubtree);

        Optional.ofNullable(configModel.getUserObjectClass())
                .ifPresent(serverModel::setUserObjectClass);

        Optional.ofNullable(configModel.getUserFilter())
                .ifPresent(serverModel::setUserLdapFilter);

        Optional.ofNullable(configModel.getUserIdAttr())
                .ifPresent(serverModel::setUserIdAttribute);

        Optional.ofNullable(configModel.getUserRealmNameAtr())
                .ifPresent(serverModel::setUserRealNameAttribute);

        Optional.ofNullable(configModel.getUserEmailAttr())
                .ifPresent(serverModel::setUserEmailAddressAttribute);

        Optional.ofNullable(configModel.getUserPasswordAttr())
                .ifPresent(serverModel::setUserPasswordAttribute);

        Optional.ofNullable(configModel.getGroupAsRoles())
                .ifPresent(serverModel::setLdapGroupsAsRoles);

        Optional.ofNullable(configModel.getGroupType())
                .ifPresent(serverModel::setGroupType);

        Optional.ofNullable(configModel.getGroupBaseDn())
                .ifPresent(serverModel::setGroupBaseDn);

        Optional.ofNullable(configModel.getGroupSubtree())
                .ifPresent(serverModel::setGroupSubtree);

        Optional.ofNullable(configModel.getGroupObjectClass())
                .ifPresent(serverModel::setGroupObjectClass);

        Optional.ofNullable(configModel.getGroupIdAttr())
                .ifPresent(serverModel::setGroupIdAttribute);

        Optional.ofNullable(configModel.getGroupMemberAttr())
                .ifPresent(serverModel::setGroupMemberAttribute);

        Optional.ofNullable(configModel.getGroupMemberFormat())
                .ifPresent(serverModel::setGroupMemberFormat);

        Optional.ofNullable(configModel.getUserMemberOfAttr())
                .ifPresent(serverModel::setUserMemberOfAttribute);

    }
}
