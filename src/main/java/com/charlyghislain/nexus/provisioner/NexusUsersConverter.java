package com.charlyghislain.nexus.provisioner;

import com.charlyghislain.nexus.client.KubernetesClient;
import com.charlyghislain.nexus.config.NexusUserModel;
import com.charlyghislain.nexus.nexus.ApiUser;

import java.util.Optional;
import java.util.Set;

public class NexusUsersConverter {

    private final KubernetesClient kubernetesClient;
    private final CollectionReconciliator reconciliator;

    public NexusUsersConverter(KubernetesClient kubernetesClient, CollectionReconciliator reconciliator) {
        this.kubernetesClient = kubernetesClient;
        this.reconciliator = reconciliator;
    }

    public void patchUser(NexusUserModel nexusUserModel, ApiUser apiUser) {
        Optional.ofNullable(apiUser.getUserId())
                .ifPresent(nexusUserModel::setId);

        Optional.ofNullable(apiUser.getStatus())
                .map(ApiUser.StatusEnum::value)
                .ifPresent(nexusUserModel::setStatus);

        Optional.ofNullable(apiUser.getEmailAddress())
                .ifPresent(nexusUserModel::setEmail);

        Optional.ofNullable(apiUser.getFirstName())
                .ifPresent(nexusUserModel::setFirstName);

        Optional.ofNullable(apiUser.getLastName())
                .ifPresent(nexusUserModel::setLastName);

        Optional.ofNullable(apiUser.getSource())
                .ifPresent(nexusUserModel::setSource);

        Set<String> curRoles = nexusUserModel.getRoles();
        Optional.ofNullable(apiUser.getRoles())
                .map(r -> reconciliator.reconcileNames("Role", r, curRoles))
                .ifPresent(nexusUserModel::setRoles);

    }
}
