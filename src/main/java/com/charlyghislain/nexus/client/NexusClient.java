package com.charlyghislain.nexus.client;

import com.charlyghislain.nexus.api.LdapServer;
import com.charlyghislain.nexus.config.NexusAnonymousModel;
import com.charlyghislain.nexus.config.NexusGroupRepoModel;
import com.charlyghislain.nexus.config.NexusHostedRepoModel;
import com.charlyghislain.nexus.config.NexusProxyRepoModel;
import com.charlyghislain.nexus.config.NexusUserModel;
import com.charlyghislain.nexus.nexus.AnonymousAccessSettingsXO;
import com.charlyghislain.nexus.nexus.ApiCreateUser;
import com.charlyghislain.nexus.nexus.ApiEmailConfiguration;
import com.charlyghislain.nexus.nexus.ApiUser;
import com.charlyghislain.nexus.nexus.CleanupPolicyAttributes;
import com.charlyghislain.nexus.nexus.ComponentAttributes;
import com.charlyghislain.nexus.nexus.DockerAttributes;
import com.charlyghislain.nexus.nexus.DockerGroupApiRepository;
import com.charlyghislain.nexus.nexus.DockerGroupRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.DockerHostedApiRepository;
import com.charlyghislain.nexus.nexus.DockerHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.DockerProxyApiRepository;
import com.charlyghislain.nexus.nexus.DockerProxyRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.GroupAttributes;
import com.charlyghislain.nexus.nexus.GroupDeployAttributes;
import com.charlyghislain.nexus.nexus.HelmHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.HelmProxyRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.HostedStorageAttributes;
import com.charlyghislain.nexus.nexus.HttpClientAttributes;
import com.charlyghislain.nexus.nexus.HttpClientAttributesWithPreemptiveAuth;
import com.charlyghislain.nexus.nexus.MavenAttributes;
import com.charlyghislain.nexus.nexus.MavenGroupRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.MavenHostedApiRepository;
import com.charlyghislain.nexus.nexus.MavenHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.MavenProxyApiRepository;
import com.charlyghislain.nexus.nexus.MavenProxyRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.NegativeCacheAttributes;
import com.charlyghislain.nexus.nexus.NpmGroupRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.NpmHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.NpmProxyApiRepository;
import com.charlyghislain.nexus.nexus.NpmProxyRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.ProxyAttributes;
import com.charlyghislain.nexus.nexus.RawGroupRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.RawHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.RawProxyRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.RepositoryXO;
import com.charlyghislain.nexus.nexus.RoleXORequest;
import com.charlyghislain.nexus.nexus.RoleXOResponse;
import com.charlyghislain.nexus.nexus.SimpleApiGroupDeployRepository;
import com.charlyghislain.nexus.nexus.SimpleApiGroupRepository;
import com.charlyghislain.nexus.nexus.SimpleApiHostedRepository;
import com.charlyghislain.nexus.nexus.SimpleApiProxyRepository;
import com.charlyghislain.nexus.nexus.StorageAttributes;
import com.charlyghislain.nexus.nexus.V1Api;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.UriBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NexusClient {
    private final static Logger LOG = Logger.getLogger(NexusClient.class.getName());
    public static final String NX_ADMIN_ROLE = "nx-admin";
    public static final String NX_ANONYMOUS_ROLE = "nx-anonymous";

    public static final String NX_ANONYMOUS_USER = "anonymous";
    public static final String NX_ADMIN_USER = "admin";

    private final URI uri;
    private final Boolean debug;
    private final HttpClient httpClient;
    private final ObjectMapper jsonMapper;
    private V1Api v1Api;
    private String password;

    public NexusClient(URI uri, Boolean debug) {
        this.uri = uri;
        this.debug = debug;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.jsonMapper = createObjectMapper();
    }

    public void updateAdminPassword(String oldPassword, String adminPassword) throws ClientException, IOException {
        V1Api v1Api = createApi(uri, oldPassword);
        try {
            v1Api.changePassword("admin", adminPassword);
        } catch (Exception e) {
            throw new ClientException("Unable to change admin password", e);
        } finally {
            ((Closeable) v1Api).close();
        }
    }

    public void openApiClient(String adminPassword) {
        // Need to store auth for invalid spec requets
        v1Api = createApi(uri, adminPassword);
        this.password = adminPassword;
    }

    public void closeApiClient() throws ClientException {
        if (this.v1Api != null) {
            try {
                Closeable closeable = (Closeable) this.v1Api;
                closeable.close();
            } catch (IOException e) {
                throw new ClientException("Unable to close client", e);
            }
        }
        this.v1Api = null;
        this.password = null;
    }

    private V1Api createApi(URI uri, String adminPassword) {
        ResteasyJackson2Provider component = new ResteasyJackson2Provider();
        component.setMapper(jsonMapper);

        return RestClientBuilder.newBuilder()
                .register(component)
                .register(new DefaultTextPlain())
                .register(new StringTextStar())
                .register(addAuthHeader(adminPassword))
                .baseUri(uri)
                .build(V1Api.class);
    }

    private HttpRequest.Builder buildRequest(URI uri, String path, String adminPassword) {
        URI pathUri = UriBuilder.fromUri(uri)
                .path(path)
                .build();
        return HttpRequest.newBuilder()
                .header("Authorization", encodeBaseAuth(adminPassword))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .uri(pathUri);
    }


    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        mapper.disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        return mapper;
    }

    private ClientRequestFilter addAuthHeader(String adminInitialPassword) {
        return requestContext -> {
            String encodedCreds = encodeBaseAuth(adminInitialPassword);
            requestContext.getHeaders().putSingle("authorization", encodedCreds);
        };
    }

    private String encodeBaseAuth(String adminInitialPassword) {
        return "Basic " + Base64.getEncoder().encodeToString((NexusClient.NX_ADMIN_USER + ":" + adminInitialPassword).getBytes(StandardCharsets.UTF_8));
    }


    public ApiEmailConfiguration getEmail() {
        return v1Api.getEmailConfiguration();
    }

    public void setEmail(ApiEmailConfiguration mailConfig) {
        v1Api.setEmailConfiguration(mailConfig);
    }


    public List<LdapServer> getLdaps() throws ClientException {
        HttpRequest build = buildRequest(uri, "v1/security/ldap", password)
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(build, HttpResponse.BodyHandlers.ofInputStream());
            ;
            TypeReference<List<LdapServer>> typeReference = new TypeReference<>() {
            };
            JsonParser parser = jsonMapper.createParser(response.body());
            List<LdapServer> ldapServers = jsonMapper.reader().readValue(parser, typeReference);
            return ldapServers;
        } catch (IOException | InterruptedException e) {
            throw new ClientException("Unable to list ldaps", e);
        }
    }

    public void createLdapServer(LdapServer ldapServer) {
        String name = ldapServer.getName();
        try {
            String body = jsonMapper.writer().writeValueAsString(ldapServer);
            HttpRequest request = buildRequest(uri, "v1/security/ldap", password)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (IOException | InterruptedException e) {
            throw new ClientRuntimeError("Unable to update ldaps " + name, e);
        }

    }

    public void removeLdapServer(LdapServer ldapServer) {
        String name = ldapServer.getName();
        try {
            HttpRequest request = buildRequest(uri, "v1/security/ldap/" + name, password)
                    .DELETE()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (IOException | InterruptedException e) {
            throw new ClientRuntimeError("Unable to delete ldaps " + name, e);
        }
    }

    public void updateLdapServer(LdapServer ldapServer) {
        String name = ldapServer.getName();
        try {
            String body = jsonMapper.writer().writeValueAsString(ldapServer);
            HttpRequest request = buildRequest(uri, "v1/security/ldap/" + name, password)
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (IOException | InterruptedException e) {
            throw new ClientRuntimeError("Unable to create ldaps " + name, e);
        }
    }

    public List<String> getEnabledRealms() {
        return v1Api.getActiveRealms();
    }

    public void setAnonymous(NexusAnonymousModel nexusAnonymousModel) {
        v1Api.update(new AnonymousAccessSettingsXO()
                .enabled(nexusAnonymousModel.getEnabled())
                .realmName(nexusAnonymousModel.getRealmName())
                .userId(nexusAnonymousModel.getUserId())
        );
    }

    public List<RoleXOResponse> getRoles() {
        return v1Api.getRoles("default");
    }

    public void createRole(RoleXORequest roleXORequest) {
        v1Api.create(roleXORequest);
    }

    public void removeRole(RoleXORequest roleXORequest) {
        String roleId = roleXORequest.getId();
        if (Objects.equals(roleId, NX_ADMIN_ROLE) || roleId.equals(NX_ANONYMOUS_ROLE)) {
            LOG.finer("Ignoring pruning protected role " + roleId);
            return;
        }
        v1Api.delete(roleXORequest.getId());
    }

    public void updateRole(RoleXORequest roleXORequest) {
        v1Api.update1(roleXORequest.getId(), roleXORequest);
    }

    public void setActiveRealms(List<String> newNames) {
        v1Api.setActiveRealms(newNames);
    }

    public List<RepositoryXO> getHosted() {
        return v1Api.getRepositories1()
                .stream()
                .filter(r -> r.getType().equals("hosted"))
                .collect(Collectors.toList());
    }

    public List<RepositoryXO> getProxies() {
        return v1Api.getRepositories1()
                .stream()
                .filter(r -> r.getType().equals("proxy"))
                .collect(Collectors.toList());
    }


    public List<RepositoryXO> getGroupRepos() {
        return v1Api.getRepositories1()
                .stream()
                .filter(r -> r.getType().equals("group"))
                .collect(Collectors.toList());
    }

    public void removeReposiory(RepositoryXO repoModel) {
        v1Api.deleteRepository(repoModel.getName());
    }

    public void createHosted(NexusHostedRepoModel repoModel) {
        switch (repoModel.getFormat()) {
            case NPM: {
                NpmHostedRepositoryApiRequest repo = toNpmHostedRepository(repoModel);
                v1Api.createRepository9(repo);
                break;
            }
            case RAW: {
                RawHostedRepositoryApiRequest repo = toRawHostedRepository(repoModel);
                v1Api.createRepository6(repo);
                break;
            }
            case HELM: {
                HelmHostedRepositoryApiRequest repo = toHelmHostedRepostiory(repoModel);
                v1Api.createRepository23(repo);
                break;
            }
            case DOCKER: {
                DockerHostedRepositoryApiRequest repo = toDockerHostedRepository(repoModel);
                v1Api.createRepository18(repo);
                break;
            }
            case MAVEN: {
                MavenHostedRepositoryApiRequest repo = toMavenHostedRepository(repoModel);
                v1Api.createRepository1(repo);
                break;
            }
        }
    }


    public void updateHosted(NexusHostedRepoModel repoModel) {
        switch (repoModel.getFormat()) {
            case NPM: {
                NpmHostedRepositoryApiRequest repo = toNpmHostedRepository(repoModel);
                v1Api.updateRepository9(repoModel.getName(), repo);
                break;
            }
            case RAW: {
                RawHostedRepositoryApiRequest repo = toRawHostedRepository(repoModel);
                v1Api.updateRepository6(repo.getName(), repo);
                break;
            }
            case HELM: {
                HelmHostedRepositoryApiRequest repo = toHelmHostedRepostiory(repoModel);
                v1Api.updateRepository23(repo.getName(), repo);
                break;
            }
            case DOCKER: {
                DockerHostedRepositoryApiRequest repo = toDockerHostedRepository(repoModel);
                v1Api.updateRepository18(repo.getName(), repo);
                break;
            }
            case MAVEN: {
                MavenHostedRepositoryApiRequest repo = toMavenHostedRepository(repoModel);
                v1Api.updateRepository1(repo.getName(), repo);
                break;
            }
        }
    }

    public void createProxy(NexusProxyRepoModel repoModel) {
        switch (repoModel.getFormat()) {
            case NPM: {
                NpmProxyRepositoryApiRequest repo = toNpmProxyRepository(repoModel);
                v1Api.createRepository10(repo);
                break;
            }
            case RAW: {
                RawProxyRepositoryApiRequest repo = toRawProxyRepository(repoModel);
                v1Api.createRepository7(repo);
                break;
            }
            case HELM: {
                HelmProxyRepositoryApiRequest repo = toHelmRProxyepostiory(repoModel);
                v1Api.createRepository24(repo);
                break;
            }
            case DOCKER: {
                DockerProxyRepositoryApiRequest repo = toDockerProxyRepository(repoModel);
                v1Api.createRepository19(repo);
                break;
            }
            case MAVEN: {
                MavenProxyRepositoryApiRequest repo = toMavenProxyRepository(repoModel);
                v1Api.createRepository2(repo);
                break;
            }
        }
    }

    public void updateProxy(NexusProxyRepoModel repoModel) {
        switch (repoModel.getFormat()) {
            case NPM: {
                NpmProxyRepositoryApiRequest repo = toNpmProxyRepository(repoModel);
                v1Api.updateRepository10(repoModel.getName(), repo);
                break;
            }
            case RAW: {
                RawProxyRepositoryApiRequest repo = toRawProxyRepository(repoModel);
                v1Api.updateRepository7(repo.getName(), repo);
                break;
            }
            case HELM: {
                HelmProxyRepositoryApiRequest repo = toHelmRProxyepostiory(repoModel);
                v1Api.updateRepository24(repo.getName(), repo);
                break;
            }
            case DOCKER: {
                DockerProxyRepositoryApiRequest repo = toDockerProxyRepository(repoModel);
                v1Api.updateRepository19(repo.getName(), repo);
                break;
            }
            case MAVEN: {
                MavenProxyRepositoryApiRequest repo = toMavenProxyRepository(repoModel);
                v1Api.updateRepository2(repo.getName(), repo);
                break;
            }
        }
    }

    public void createGroup(NexusGroupRepoModel repoModel) {
        switch (repoModel.getFormat()) {
            case NPM: {
                NpmGroupRepositoryApiRequest repo = toNpmGroupRepository(repoModel);
                v1Api.createRepository8(repo);
                break;
            }
            case RAW: {
                RawGroupRepositoryApiRequest repo = toRawGroupRepository(repoModel);
                v1Api.createRepository5(repo);
                break;
            }
            case DOCKER: {
                DockerGroupRepositoryApiRequest repo = toDockerGroupRepository(repoModel);
                v1Api.createRepository17(repo);
                break;
            }
            case MAVEN: {
                MavenGroupRepositoryApiRequest repo = toMavenGroupRepository(repoModel);
                v1Api.createRepository(repo);
                break;
            }
        }
    }

    public void updateGroup(NexusGroupRepoModel repoModel) {
        switch (repoModel.getFormat()) {
            case NPM: {
                NpmGroupRepositoryApiRequest repo = toNpmGroupRepository(repoModel);
                v1Api.updateRepository8(repo.getName(), repo);
                break;
            }
            case RAW: {
                RawGroupRepositoryApiRequest repo = toRawGroupRepository(repoModel);
                v1Api.updateRepository5(repo.getName(), repo);
                break;
            }
            case DOCKER: {
                DockerGroupRepositoryApiRequest repo = toDockerGroupRepository(repoModel);
                v1Api.updateRepository17(repo.getName(), repo);
                break;
            }
            case MAVEN: {
                MavenGroupRepositoryApiRequest repo = toMavenGroupRepository(repoModel);
                v1Api.updateRepository(repo.getName(), repo);
                break;
            }
        }
    }


    public SimpleApiGroupRepository getMavenGroup(String name) {
        return v1Api.getRepository1(name);
    }

    public MavenHostedApiRepository getMavenHosted(String name) {
        return v1Api.getRepository2(name);
    }

    public MavenProxyApiRepository getMavenProxy(String name) {
        return v1Api.getRepository3(name);
    }

    public DockerGroupApiRepository getDockerGroup(String name) {
        return v1Api.getRepository18(name);
    }

    public DockerHostedApiRepository getDockerHosted(String name) {
        return v1Api.getRepository19(name);
    }

    public DockerProxyApiRepository getDockerProxy(String name) {
        return v1Api.getRepository20(name);
    }

    public SimpleApiGroupDeployRepository getNpmGroup(String name) {
        return v1Api.getRepository9(name);
    }

    public SimpleApiHostedRepository getNpmHosted(String name) {
        return v1Api.getRepository10(name);
    }

    public NpmProxyApiRepository getNpmProxy(String name) {
        return v1Api.getRepository11(name);
    }

    public SimpleApiGroupRepository getRawGroup(String name) {
        return v1Api.getRepository6(name);
    }

    public SimpleApiHostedRepository getRawHosted(String name) {
        return v1Api.getRepository7(name);
    }

    public SimpleApiProxyRepository getRawProxy(String name) {
        return v1Api.getRepository8(name);
    }

    public SimpleApiHostedRepository getHelmHosted(String name) {
        return v1Api.getRepository24(name);
    }

    public SimpleApiProxyRepository getHelmProxy(String name) {
        return v1Api.getRepository25(name);
    }

    private MavenHostedRepositoryApiRequest toMavenHostedRepository(NexusHostedRepoModel repoModel) {
        MavenHostedRepositoryApiRequest repo = new MavenHostedRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new HostedStorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                        .writePolicy(HostedStorageAttributes.WritePolicyEnum.valueOf(repoModel.getWritePolicy().name()))
                )
                .maven(new MavenAttributes()
                        .contentDisposition(MavenAttributes.ContentDispositionEnum.valueOf(repoModel.getContentDisposition()))
                        .layoutPolicy(repoModel.getStrictContentValidation() ? MavenAttributes.LayoutPolicyEnum.STRICT : MavenAttributes.LayoutPolicyEnum.PERMISSIVE)
                        .versionPolicy(MavenAttributes.VersionPolicyEnum.valueOf(repoModel.getMavenVersionPolicy()))
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .component(new ComponentAttributes());
        return repo;
    }

    private DockerHostedRepositoryApiRequest toDockerHostedRepository(NexusHostedRepoModel repoModel) {
        DockerHostedRepositoryApiRequest repo = new DockerHostedRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new HostedStorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                        .writePolicy(HostedStorageAttributes.WritePolicyEnum.valueOf(repoModel.getWritePolicy().name()))
                )
                .docker(new DockerAttributes()
                        .forceBasicAuth(repoModel.getForceBasicAuth())
                        .httpPort(repoModel.getHttpPort())
                        .httpsPort(repoModel.getHttpsPort())
                        .v1Enabled(repoModel.getV1Enabled())
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .component(new ComponentAttributes());
        return repo;
    }

    private HelmHostedRepositoryApiRequest toHelmHostedRepostiory(NexusHostedRepoModel repoModel) {
        HelmHostedRepositoryApiRequest repo = new HelmHostedRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new HostedStorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                        .writePolicy(HostedStorageAttributes.WritePolicyEnum.valueOf(repoModel.getWritePolicy().name()))
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .component(new ComponentAttributes());
        return repo;
    }

    private RawHostedRepositoryApiRequest toRawHostedRepository(NexusHostedRepoModel repoModel) {
        RawHostedRepositoryApiRequest repo = new RawHostedRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new HostedStorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                        .writePolicy(HostedStorageAttributes.WritePolicyEnum.valueOf(repoModel.getWritePolicy().name()))
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .component(new ComponentAttributes());
        return repo;
    }

    private NpmHostedRepositoryApiRequest toNpmHostedRepository(NexusHostedRepoModel repoModel) {
        NpmHostedRepositoryApiRequest repo = new NpmHostedRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new HostedStorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                        .writePolicy(HostedStorageAttributes.WritePolicyEnum.valueOf(repoModel.getWritePolicy().name()))
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .component(new ComponentAttributes());
        return repo;
    }

    private MavenGroupRepositoryApiRequest toMavenGroupRepository(NexusGroupRepoModel repoModel) {
        MavenGroupRepositoryApiRequest repo = new MavenGroupRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .group(new GroupAttributes()
                        .memberNames(repoModel.getMemberNames()));
        return repo;
    }

    private DockerGroupRepositoryApiRequest toDockerGroupRepository(NexusGroupRepoModel repoModel) {
        DockerGroupRepositoryApiRequest repo = new DockerGroupRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .group(new GroupDeployAttributes()
                        .memberNames(repoModel.getMemberNames()));
        return repo;
    }

    private RawGroupRepositoryApiRequest toRawGroupRepository(NexusGroupRepoModel repoModel) {
        RawGroupRepositoryApiRequest repo = new RawGroupRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .group(new GroupAttributes()
                        .memberNames(repoModel.getMemberNames()));
        return repo;
    }

    private NpmGroupRepositoryApiRequest toNpmGroupRepository(NexusGroupRepoModel repoModel) {
        NpmGroupRepositoryApiRequest repo = new NpmGroupRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .group(new GroupDeployAttributes()
                        .memberNames(repoModel.getMemberNames()));
        return repo;
    }

    private MavenProxyRepositoryApiRequest toMavenProxyRepository(NexusProxyRepoModel repoModel) {
        MavenProxyRepositoryApiRequest repo = new MavenProxyRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .maven(new MavenAttributes()
                        .contentDisposition(MavenAttributes.ContentDispositionEnum.valueOf(repoModel.getContentDisposition()))
                        .layoutPolicy(repoModel.getStrictContentValidation() ? MavenAttributes.LayoutPolicyEnum.STRICT : MavenAttributes.LayoutPolicyEnum.PERMISSIVE)
                        .versionPolicy(MavenAttributes.VersionPolicyEnum.valueOf(repoModel.getMavenVersionPolicy()))
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .negativeCache(new NegativeCacheAttributes()
                        .enabled(repoModel.getNegativeCache())
                        .timeToLive(repoModel.getNegativeCacheTTl())
                )
                .httpClient(new HttpClientAttributesWithPreemptiveAuth()
                        .autoBlock(repoModel.getHttpAutoBlock())
                        .blocked(repoModel.getHttpAuthBlocked())
                )
                .proxy(new ProxyAttributes()
                        .contentMaxAge(repoModel.getMaxAge())
                        .metadataMaxAge(repoModel.getMetadataMaxAge())
                        .remoteUrl(repoModel.getUri())
                );
        return repo;
    }

    private DockerProxyRepositoryApiRequest toDockerProxyRepository(NexusProxyRepoModel repoModel) {
        DockerProxyRepositoryApiRequest repo = new DockerProxyRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .docker(new DockerAttributes()
                        .forceBasicAuth(repoModel.getForceBasicAuth())
                        .httpPort(repoModel.getHttpPort())
                        .httpsPort(repoModel.getHttpsPort())
                        .v1Enabled(repoModel.getV1Enabled())
                )
                .negativeCache(new NegativeCacheAttributes()
                        .enabled(repoModel.getNegativeCache())
                        .timeToLive(repoModel.getNegativeCacheTTl())
                )
                .httpClient(new HttpClientAttributes()
                        .autoBlock(repoModel.getHttpAutoBlock())
                        .blocked(repoModel.getHttpAuthBlocked())
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .proxy(new ProxyAttributes()
                        .contentMaxAge(repoModel.getMaxAge())
                        .metadataMaxAge(repoModel.getMetadataMaxAge())
                        .remoteUrl(repoModel.getUri())
                );
        return repo;
    }

    private HelmProxyRepositoryApiRequest toHelmRProxyepostiory(NexusProxyRepoModel repoModel) {
        HelmProxyRepositoryApiRequest repo = new HelmProxyRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .negativeCache(new NegativeCacheAttributes()
                        .enabled(repoModel.getNegativeCache())
                        .timeToLive(repoModel.getNegativeCacheTTl())
                )
                .httpClient(new HttpClientAttributes()
                        .autoBlock(repoModel.getHttpAutoBlock())
                        .blocked(repoModel.getHttpAuthBlocked())
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .proxy(new ProxyAttributes()
                        .contentMaxAge(repoModel.getMaxAge())
                        .metadataMaxAge(repoModel.getMetadataMaxAge())
                        .remoteUrl(repoModel.getUri())
                );
        return repo;
    }

    private RawProxyRepositoryApiRequest toRawProxyRepository(NexusProxyRepoModel repoModel) {
        RawProxyRepositoryApiRequest repo = new RawProxyRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .negativeCache(new NegativeCacheAttributes()
                        .enabled(repoModel.getNegativeCache())
                        .timeToLive(repoModel.getNegativeCacheTTl())
                )
                .httpClient(new HttpClientAttributes()
                        .autoBlock(repoModel.getHttpAutoBlock())
                        .blocked(repoModel.getHttpAuthBlocked())
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .proxy(new ProxyAttributes()
                        .contentMaxAge(repoModel.getMaxAge())
                        .metadataMaxAge(repoModel.getMetadataMaxAge())
                        .remoteUrl(repoModel.getUri())
                );
        return repo;
    }

    private NpmProxyRepositoryApiRequest toNpmProxyRepository(NexusProxyRepoModel repoModel) {
        NpmProxyRepositoryApiRequest repo = new NpmProxyRepositoryApiRequest()
                .name(repoModel.getName())
                .online(repoModel.getOnline())
                .storage(new StorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                )
                .negativeCache(new NegativeCacheAttributes()
                        .enabled(repoModel.getNegativeCache())
                        .timeToLive(repoModel.getNegativeCacheTTl())
                )
                .httpClient(new HttpClientAttributes()
                        .autoBlock(repoModel.getHttpAutoBlock())
                        .blocked(repoModel.getHttpAuthBlocked())
                )
                .cleanup(new CleanupPolicyAttributes().policyNames(repoModel.getCleanupPolicies()))
                .proxy(new ProxyAttributes()
                        .contentMaxAge(repoModel.getMaxAge())
                        .metadataMaxAge(repoModel.getMetadataMaxAge())
                        .remoteUrl(repoModel.getUri())
                );
        return repo;
    }

    public List<ApiUser> getUsers() {
        List<ApiUser> users = v1Api.getUsers(null, "default");
        return users;
    }


    public void createUser(NexusUserModel model, Optional<String> password) {
        ApiCreateUser.StatusEnum status = Optional.ofNullable(model.getStatus())
                .map(ApiCreateUser.StatusEnum::fromValue)
                .orElse(ApiCreateUser.StatusEnum.ACTIVE);
        ApiCreateUser createUser = new ApiCreateUser()
                .userId(model.getId())
                .emailAddress(model.getEmail())
                .firstName(model.getFirstName())
                .lastName(model.getLastName())
                .status(status)
                .password(password.orElse(null))
                .roles(model.getRoles());
        v1Api.createUser(createUser);
    }

    public void updateUser(NexusUserModel model, Optional<String> password) {
        ApiUser apiUser = toApiUser(model);
        v1Api.updateUser(model.getId(), apiUser);

        password.ifPresent(p -> v1Api.changePassword(apiUser.getUserId(), p));
    }

    private ApiUser toApiUser(NexusUserModel model) {
        ApiUser.StatusEnum status = Optional.ofNullable(model.getStatus())
                .map(ApiUser.StatusEnum::fromValue)
                .orElse(ApiUser.StatusEnum.ACTIVE);

        return new ApiUser()
                .userId(model.getId())
                .emailAddress(model.getEmail())
                .firstName(model.getFirstName())
                .lastName(model.getLastName())
                .source(model.getSource())
                .status(status)
                .roles(model.getRoles());
    }

    public <API> void removeUser(ApiUser apiUser) {
        if (apiUser.getUserId().equalsIgnoreCase(NX_ADMIN_USER)
                || apiUser.getUserId().equalsIgnoreCase(NX_ANONYMOUS_ROLE)) {
            LOG.fine("Ignoring pruning protected user " + apiUser.getUserId());
            return;
        }
        v1Api.deleteUser(apiUser.getUserId());
    }
}
