package com.charlyghislain.nexus.provisioner;

import com.charlyghislain.nexus.client.KubernetesClient;
import com.charlyghislain.nexus.config.NexusRoleModel;
import com.charlyghislain.nexus.config.NexusUserModel;
import com.charlyghislain.nexus.nexus.ApiUser;
import com.charlyghislain.nexus.nexus.RoleXORequest;
import com.charlyghislain.nexus.nexus.RoleXOResponse;

import java.util.List;
import java.util.Optional;

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
                .map(s -> s == ApiUser.StatusEnum.ACTIVE)
                .ifPresent(nexusUserModel::setActive);

        Optional.ofNullable(apiUser.getEmailAddress())
                .ifPresent(nexusUserModel::setEmail);

        Optional.ofNullable(apiUser.getFirstName())
                .ifPresent(nexusUserModel::setFirstName);

        Optional.ofNullable(apiUser.getLastName())
                .ifPresent(nexusUserModel::setLastName);

        Optional.ofNullable(apiUser.getSource())
                .ifPresent(nexusUserModel::setSource);

        List<String> curRoles = nexusUserModel.getRoles();
        Optional.ofNullable(apiUser.getRoles())
                .map(r -> reconciliator.reconcileNames("Role", r, curRoles))
                .ifPresent(nexusUserModel::setRoles);

    }
}
