package com.charlyghislain.nexus.client;

import com.charlyghislain.nexus.config.NexusSecretValueModel;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.util.ClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KubernetesClient {

    private final static Logger LOG = Logger.getLogger(KubernetesClient.class.getName());
    private final String nexusNamespace;
    private final String labelSelector;
    private final boolean debug;

    private CoreV1Api api;


    public KubernetesClient() {
        nexusNamespace = "";
        labelSelector = "";
        debug = true;
        LOG.info("Kubernetes client disabled");
    }

    public KubernetesClient(String nexusNamespace, String labelSelector, boolean debug) throws IOException {
        this.nexusNamespace = nexusNamespace;
        this.labelSelector = labelSelector;
        this.debug = debug;
        this.api = initApi();
    }


    public String getInitialNexusAdminPassword() throws ClientException {
        checkEnabled();
        try {
            LOG.log(Level.FINE, MessageFormat.format("Feching initial password in namespace {0} using label selector {1}",
                    nexusNamespace, labelSelector));

            V1PodList nexusPodList = api.listNamespacedPod(nexusNamespace, null, null, null, null,
                    labelSelector, 1, null, null, null, false);
            List<V1Pod> items = nexusPodList.getItems();
            if (items.isEmpty()) {
                throw new ClientException("No nexus pod found in ns " + nexusNamespace + " with label " + labelSelector);
            }
            V1Pod nexusPod = items.get(0);

            Exec exec = new Exec();
            Process process = exec.exec(nexusPod, new String[]{"cat", "/nexus-data/admin.password"}, false);
            CompletableFuture<InputStream> processStreamFuture = CompletableFuture.supplyAsync(process::getInputStream);
            process.waitFor();
            InputStream processInputStream = processStreamFuture.join();
            byte[] outputBytes = processInputStream.readAllBytes();
            String adminPassword = new String(outputBytes, StandardCharsets.UTF_8);

            LOG.fine("Found admin.password file content");

            return adminPassword;
        } catch (IOException | InterruptedException | ApiException e) {
            throw new ClientException("Unable to find  initial admin password", e);
        }
    }

    public String resolveSecretValue(NexusSecretValueModel secretValueModel) throws ClientRuntimeError {
        Optional<String> clearTextOptional = Optional.ofNullable(secretValueModel.getClearText())
                .filter(s -> !s.isBlank());
        if (clearTextOptional.isPresent()) {
            return clearTextOptional.get();
        }
        checkEnabled();

        String secretName = Optional.ofNullable(secretValueModel.getSecretKey())
                .orElseThrow(() -> new ClientRuntimeError("No secret name"));
        String secretKey = Optional.ofNullable(secretValueModel.getSecretKey())
                .orElseThrow(() -> new ClientRuntimeError("No secret key"));

        V1Secret v1Secret;
        try {
            V1SecretList v1SecretList = api.listNamespacedSecret(nexusNamespace, null, null, null, "name=" + secretName, labelSelector,
                    1, null, null, null, null);
            if (v1SecretList.getItems().isEmpty()) {
                throw new ClientRuntimeError("No secret found for name " + secretName);
            }
            v1Secret = v1SecretList.getItems().get(0);
        } catch (ApiException e) {
            throw new ClientRuntimeError("Unable to get secret " + secretName);
        }

        String secretData = Optional.ofNullable(v1Secret.getData())
                .map(s -> s.get(secretKey))
                .map((byte[] bytes) -> Base64.getDecoder().decode(bytes))
                .map(decoded -> new String(decoded, StandardCharsets.UTF_8))
                .orElseThrow(() -> new ClientRuntimeError("Unable to find secretKey " + secretKey + " in secret " + secretName));
        return secretData;
    }

    private CoreV1Api initApi() throws IOException {
        // loading the in-cluster config, including:
        //   1. service-account CA
        //   2. service-account bearer-token
        //   3. service-account namespace
        //   4. master endpoints(ip, port) from pre-set environment variables
        ApiClient client = ClientBuilder.cluster().build();

        if (debug) {
            client.setDebugging(true);
        }

        // if you prefer not to refresh service account token, please use:
        // ApiClient client = ClientBuilder.oldCluster().build();

        // set the global default api-client to the in-cluster one from above
        Configuration.setDefaultApiClient(client);

        // the CoreV1Api loads default api-client from global configuration.
        return new CoreV1Api();
    }

    private void checkEnabled() {
        if (this.api == null) {
            throw new RuntimeException("Kubernetes disabled");
        }
    }

}

