package com.charlyghislain.nexus.provisioner;

import com.charlyghislain.nexus.client.KubernetesClient;
import com.charlyghislain.nexus.config.NexusEmailModel;
import com.charlyghislain.nexus.nexus.ApiEmailConfiguration;

import java.util.Optional;

public class NexusEmailConverter {

    private final KubernetesClient kubernetesClient;

    public NexusEmailConverter(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public void patchEmailconfiguratoin(ApiEmailConfiguration email, NexusEmailModel mailConfig) {
        Optional.ofNullable(mailConfig.getEnabled())
                .ifPresent(email::setEnabled);

        Optional.ofNullable(mailConfig.getHost())
                .ifPresent(email::setHost);

        Optional.ofNullable(mailConfig.getPort())
                .ifPresent(email::setPort);

        Optional.ofNullable(mailConfig.getUsername())
                .ifPresent(email::setUsername);

        Optional.ofNullable(mailConfig.getPassword())
                .map(kubernetesClient::resolveSecretValue)
                .ifPresent(email::setPassword);

        Optional.ofNullable(mailConfig.getFrom())
                .ifPresent(email::setFromAddress);

        Optional.ofNullable(mailConfig.getSubjectPrefix())
                .ifPresent(email::setSubjectPrefix);

        Optional.ofNullable(mailConfig.getStartTls())
                .ifPresent(email::setStartTlsEnabled);

        Optional.ofNullable(mailConfig.getStartTlsRequired())
                .ifPresent(email::setStartTlsRequired);

        Optional.ofNullable(mailConfig.getSsl())
                .ifPresent(email::setSslOnConnectEnabled);

        Optional.ofNullable(mailConfig.getSslServerCheck())
                .ifPresent(email::setSslServerIdentityCheckEnabled);

        Optional.ofNullable(mailConfig.getNexusTrustStore())
                .ifPresent(email::setNexusTrustStoreEnabled);

    }
}
