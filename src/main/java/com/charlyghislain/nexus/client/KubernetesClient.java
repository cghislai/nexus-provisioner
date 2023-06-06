package com.charlyghislain.nexus.client;

import com.charlyghislain.nexus.StringUtils;
import com.charlyghislain.nexus.config.NexusSecretValueModel;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.ClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KubernetesClient {

    private final static String CREATED_RESOURCES_ANNOTATIONS = "com.charlyghislain.nexus/managed";

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

        String secretNamespace = Optional.ofNullable(secretValueModel.getSecretNamespace())
                .orElse(nexusNamespace);
        String secretName = Optional.ofNullable(secretValueModel.getSecretName())
                .orElseThrow(() -> new ClientRuntimeError("No secret name"));
        String secretKey = Optional.ofNullable(secretValueModel.getSecretKey())
                .orElseThrow(() -> new ClientRuntimeError("No secret key"));
        boolean generated = Optional.ofNullable(secretValueModel.getGenerated())
                .orElse(false);

        V1Secret v1SecretNullable;
        try {
            v1SecretNullable = api.readNamespacedSecret(secretName, secretNamespace, null);
            if (v1SecretNullable == null && !generated) {
                throw new ClientRuntimeError("No secret found for name " + secretName);
            }
        } catch (ApiException e) {
            if (e.getCode() == 404 && !generated) {
                throw new ClientRuntimeError("No secret found for name " + secretName);
            } else if (e.getCode() != 404) {
                throw new ClientRuntimeError("Unable to get secret " + secretName + ": error " + e.getCode());
            } else {
                // generated is true, we will create the missing secret
                v1SecretNullable = null;
            }
        }

        String secretData = getOrGenerateSecretData(v1SecretNullable, secretNamespace, secretName, secretKey, generated);
        return secretData;
    }

    private String getOrGenerateSecretData(V1Secret secretNullable, String namespace, String secretName, String key, boolean generated) {
        if (secretNullable != null) {
            if (generated) {
                return tryReadSecretData(secretNullable, key)
                        .orElseGet(() -> generateSecretData(secretNullable, key));
            } else {
                return tryReadSecretData(secretNullable, key)
                        .orElseThrow(() -> new RuntimeException("Unable to find secret key " + key + " in secret " + secretName));
            }
        } else {
            if (generated) {
                return createSecretWithGeneratedData(namespace, secretName, key);
            } else {
                throw new RuntimeException("Secret with name " + secretName + " not found in namespace " + namespace);
            }
        }
    }

    private String generateSecretData(V1Secret secret, String secretKey) {
        String secretText = StringUtils.getRandomAlphanumericString(24);
        byte[] secretBytes = secretText.getBytes(StandardCharsets.UTF_8);
        Map<String, byte[]> secretData = secret.getData();
        HashMap<String, byte[]> newSecretData = new HashMap<>(secretData);
        newSecretData.put(secretKey, secretBytes);
        secret.setData(newSecretData);

        V1ObjectMeta metadata = secret.getMetadata();
        try {
            V1Secret updatedSecret = api.replaceNamespacedSecret(metadata.getName(), metadata.getNamespace(), secret, null, null, null, null);
            LOG.fine("Updated secret " + updatedSecret.getMetadata().getName() + " with new key " + secretText);
            return secretText;
        } catch (Exception e) {
            throw new ClientRuntimeError("Unable to update secret " + metadata.getName() + " to append key " + secretKey);
        }
    }

    private String createSecretWithGeneratedData(String namespace, String secretName, String secretKey) {
        String secretText = StringUtils.getRandomAlphanumericString(24);
        byte[] secretBytes = secretText.getBytes(StandardCharsets.UTF_8);
        HashMap<String, byte[]> newSecretData = new HashMap<>();
        newSecretData.put(secretKey, secretBytes);

        V1Secret secret = new V1Secret()
                .metadata(new V1ObjectMeta()
                        .namespace(namespace)
                        .name(secretName)
                        .annotations(Map.of(
                                CREATED_RESOURCES_ANNOTATIONS, "true"
                        ))
                )
                .data(newSecretData)
                .type("Opaque");

        V1ObjectMeta metadata = secret.getMetadata();
        try {
            V1Secret createdSecret = api.createNamespacedSecret(namespace, secret, null, null, null, null);
            LOG.fine("Created secret " + createdSecret.getMetadata().getName() + " with new key " + secretText);
            return secretText;
        } catch (Exception e) {
            throw new ClientRuntimeError("Unable to update secret " + metadata.getName() + " to append key " + secretKey);
        }
    }

    private Optional<String> tryReadSecretData(V1Secret secret, String key) {
        String data = null;
        String secretName = secret.getMetadata().getName();

        try {
            // see https://github.com/kubernetes-client/java/issues/1377
            if (secret.getData() != null && secret.getData().containsKey(key)) {
                data = new String(secret.getData().get(key));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new ClientRuntimeError("Unable to read secretKey " + key + " in secret " + secretName);
        }

        if (data.length() > 0) {
            LOG.fine("Found secret data of length " + data.length() + " for secret key " + key);
        }
        return Optional.of(data);
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

