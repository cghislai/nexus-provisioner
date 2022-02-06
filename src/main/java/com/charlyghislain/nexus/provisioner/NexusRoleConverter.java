package com.charlyghislain.nexus.provisioner;

import com.charlyghislain.nexus.client.KubernetesClient;
import com.charlyghislain.nexus.config.NexusRoleModel;
import com.charlyghislain.nexus.nexus.RoleXORequest;
import com.charlyghislain.nexus.nexus.RoleXOResponse;

import java.util.List;
import java.util.Optional;

public class NexusRoleConverter {

    private final KubernetesClient kubernetesClient;
    private final CollectionReconciliator reconciliator;

    public NexusRoleConverter(KubernetesClient kubernetesClient, CollectionReconciliator reconciliator) {
        this.kubernetesClient = kubernetesClient;
        this.reconciliator = reconciliator;
    }

    public NexusRoleModel convertResponse(RoleXOResponse roleXOResponse) {

        NexusRoleModel nexusRoleModel = new NexusRoleModel();
        nexusRoleModel.setId(roleXOResponse.getId());
        nexusRoleModel.setDescription(roleXOResponse.getDescription());
        nexusRoleModel.setName(roleXOResponse.getName());
        nexusRoleModel.setPrivileges(roleXOResponse.getPrivileges());
        nexusRoleModel.setRoles(roleXOResponse.getRoles());
        return nexusRoleModel;

    }

    public void pathRequest(RoleXORequest roleXORequest, NexusRoleModel roleModel) {
        Optional.ofNullable(roleModel.getId())
                .ifPresent(roleXORequest::id);

        Optional.ofNullable(roleModel.getName())
                .ifPresent(roleXORequest::name);

        Optional.ofNullable(roleModel.getDescription())
                .ifPresent(roleXORequest::description);

        List<String> serverPrivileges = roleXORequest.getPrivileges();
        Optional.ofNullable(roleModel.getPrivileges())
                .map(r -> reconciliator.reconcileNames("Role privileges", serverPrivileges, r))
                .ifPresent(roleXORequest::setPrivileges);

        List<String> roles = roleXORequest.getRoles();
        Optional.ofNullable(roleModel.getRoles())
                .map(r -> reconciliator.reconcileNames("Role roles", roles, r))
                .ifPresent(roleXORequest::setRoles);

    }
}
