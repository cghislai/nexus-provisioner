package com.charlyghislain.nexus.client;

import com.charlyghislain.nexus.api.LdapServer;
import com.charlyghislain.nexus.config.NexusAnonymousModel;
import com.charlyghislain.nexus.config.NexusGroupRepoModel;
import com.charlyghislain.nexus.config.NexusHostedRepoModel;
import com.charlyghislain.nexus.config.NexusProxyRepoModel;
import com.charlyghislain.nexus.config.NexusUserModel;
import com.charlyghislain.nexus.nexus.AbstractApiRepository;
import com.charlyghislain.nexus.nexus.AnonymousAccessSettingsXO;
import com.charlyghislain.nexus.nexus.ApiCreateUser;
import com.charlyghislain.nexus.nexus.ApiEmailConfiguration;
import com.charlyghislain.nexus.nexus.ApiUser;
import com.charlyghislain.nexus.nexus.CleanupPolicyAttributes;
import com.charlyghislain.nexus.nexus.ComponentAttributes;
import com.charlyghislain.nexus.nexus.CreateLdapServerXo;
import com.charlyghislain.nexus.nexus.DockerAttributes;
import com.charlyghislain.nexus.nexus.DockerGroupApiRepository;
import com.charlyghislain.nexus.nexus.DockerGroupRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.DockerHostedApiRepository;
import com.charlyghislain.nexus.nexus.DockerHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.DockerHostedStorageAttributes;
import com.charlyghislain.nexus.nexus.DockerProxyApiRepository;
import com.charlyghislain.nexus.nexus.DockerProxyRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.EmailApi;
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
import com.charlyghislain.nexus.nexus.RepositoryManagementApi;
import com.charlyghislain.nexus.nexus.RoleXORequest;
import com.charlyghislain.nexus.nexus.RoleXOResponse;
import com.charlyghislain.nexus.nexus.SecurityManagementAnonymousAccessApi;
import com.charlyghislain.nexus.nexus.SecurityManagementLdapApi;
import com.charlyghislain.nexus.nexus.SecurityManagementRealmsApi;
import com.charlyghislain.nexus.nexus.SecurityManagementRolesApi;
import com.charlyghislain.nexus.nexus.SecurityManagementUsersApi;
import com.charlyghislain.nexus.nexus.SimpleApiGroupDeployRepository;
import com.charlyghislain.nexus.nexus.SimpleApiGroupRepository;
import com.charlyghislain.nexus.nexus.SimpleApiHostedRepository;
import com.charlyghislain.nexus.nexus.SimpleApiProxyRepository;
import com.charlyghislain.nexus.nexus.StorageAttributes;
import com.charlyghislain.nexus.nexus.UpdateLdapServerXo;
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
    private String password;

    public NexusClient(URI uri, Boolean debug) {
        this.uri = uri;
        this.debug = debug;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.jsonMapper = createObjectMapper();
    }

    public void updateAdminPassword(String oldPassword, String adminPassword) {
        withApi(SecurityManagementUsersApi.class, oldPassword, api -> {
            api.changePassword("admin", adminPassword);
        }, "Unable to change admin password");
    }

    public void setAdminPassword(String adminPassword) {
        this.password = adminPassword;
    }

    public ApiEmailConfiguration getEmail() {
        return returnWithApi(EmailApi.class, api -> api.getEmailConfiguration(),
                "Unable to set email config");
    }

    public void setEmail(ApiEmailConfiguration mailConfig) {
        withApi(EmailApi.class, api -> api.setEmailConfiguration(mailConfig),
                "Unable to set email config");
    }


    public List<LdapServer> getLdaps() {
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

        CreateLdapServerXo createLdapServerXo = new CreateLdapServerXo();

        createLdapServerXo.setName(ldapServer.getName());
        createLdapServerXo.setProtocol(CreateLdapServerXo.ProtocolEnum.fromString(ldapServer.getProtocol()));
        createLdapServerXo.setUseTrustStore(ldapServer.getUseTrustStore());
        createLdapServerXo.setHost(ldapServer.getHost());
        createLdapServerXo.setPort(ldapServer.getPort());

        createLdapServerXo.setSearchBase(ldapServer.getSearchBase());
        createLdapServerXo.setAuthScheme(CreateLdapServerXo.AuthSchemeEnum.fromString(ldapServer.getAuthScheme()));
        createLdapServerXo.setAuthUsername(ldapServer.getAuthUsername());
        createLdapServerXo.setAuthRealm(ldapServer.getAuthRealm());
        createLdapServerXo.setAuthPassword(ldapServer.getAuthPassword());

        createLdapServerXo.setConnectionTimeoutSeconds(ldapServer.getConnectionTimeoutSeconds());
        createLdapServerXo.setConnectionRetryDelaySeconds(ldapServer.getConnectionRetryDelaySeconds());
        createLdapServerXo.setMaxIncidentsCount(ldapServer.getMaxIncidentsCount());

        createLdapServerXo.setUserBaseDn(ldapServer.getUserBaseDn());
        createLdapServerXo.setUserSubtree(ldapServer.getUserSubtree());
        createLdapServerXo.setUserObjectClass(ldapServer.getUserObjectClass());
        createLdapServerXo.setUserLdapFilter(ldapServer.getUserLdapFilter());
        createLdapServerXo.setUserIdAttribute(ldapServer.getUserIdAttribute());
        createLdapServerXo.setUserRealNameAttribute(ldapServer.getUserRealNameAttribute());
        createLdapServerXo.setUserEmailAddressAttribute(ldapServer.getUserEmailAddressAttribute());
        createLdapServerXo.setUserPasswordAttribute(ldapServer.getUserPasswordAttribute());

        createLdapServerXo.setLdapGroupsAsRoles(ldapServer.getLdapGroupsAsRoles());
        createLdapServerXo.setGroupType(CreateLdapServerXo.GroupTypeEnum.fromString(ldapServer.getGroupType()));
        createLdapServerXo.setGroupBaseDn(ldapServer.getGroupBaseDn());
        createLdapServerXo.setGroupSubtree(ldapServer.getGroupSubtree());
        createLdapServerXo.setGroupObjectClass(ldapServer.getGroupObjectClass());
        createLdapServerXo.setGroupIdAttribute(ldapServer.getGroupIdAttribute());
        createLdapServerXo.setGroupMemberAttribute(ldapServer.getGroupMemberAttribute());
        createLdapServerXo.setGroupMemberFormat(ldapServer.getGroupMemberFormat());
        createLdapServerXo.setUserMemberOfAttribute(ldapServer.getUserMemberOfAttribute());


        withApi(SecurityManagementLdapApi.class,
                api -> api.createLdapServer(createLdapServerXo),
                "Unable to delete ldap server");
    }

    public void removeLdapServer(LdapServer ldapServer) {
        withApi(SecurityManagementLdapApi.class,
                api -> api.deleteLdapServer(ldapServer.getName()),
                "Unable to delete ldap server");
    }

    public void updateLdapServer(LdapServer ldapServer) {
        String name = ldapServer.getName();

        UpdateLdapServerXo updateLdapServer = new UpdateLdapServerXo();
        updateLdapServer.setName(ldapServer.getName());
        updateLdapServer.setProtocol(UpdateLdapServerXo.ProtocolEnum.fromString(ldapServer.getProtocol()));
        updateLdapServer.setUseTrustStore(ldapServer.getUseTrustStore());
        updateLdapServer.setHost(ldapServer.getHost());
        updateLdapServer.setPort(ldapServer.getPort());

        updateLdapServer.setSearchBase(ldapServer.getSearchBase());
        updateLdapServer.setAuthScheme(UpdateLdapServerXo.AuthSchemeEnum.fromString(ldapServer.getAuthScheme()));
        updateLdapServer.setAuthUsername(ldapServer.getAuthUsername());
        updateLdapServer.setAuthRealm(ldapServer.getAuthRealm());
        updateLdapServer.setAuthPassword(ldapServer.getAuthPassword());

        updateLdapServer.setConnectionTimeoutSeconds(ldapServer.getConnectionTimeoutSeconds());
        updateLdapServer.setConnectionRetryDelaySeconds(ldapServer.getConnectionRetryDelaySeconds());
        updateLdapServer.setMaxIncidentsCount(ldapServer.getMaxIncidentsCount());

        updateLdapServer.setUserBaseDn(ldapServer.getUserBaseDn());
        updateLdapServer.setUserSubtree(ldapServer.getUserSubtree());
        updateLdapServer.setUserObjectClass(ldapServer.getUserObjectClass());
        updateLdapServer.setUserLdapFilter(ldapServer.getUserLdapFilter());
        updateLdapServer.setUserIdAttribute(ldapServer.getUserIdAttribute());
        updateLdapServer.setUserRealNameAttribute(ldapServer.getUserRealNameAttribute());
        updateLdapServer.setUserEmailAddressAttribute(ldapServer.getUserEmailAddressAttribute());
        updateLdapServer.setUserPasswordAttribute(ldapServer.getUserPasswordAttribute());

        updateLdapServer.setLdapGroupsAsRoles(ldapServer.getLdapGroupsAsRoles());
        updateLdapServer.setGroupType(UpdateLdapServerXo.GroupTypeEnum.fromString(ldapServer.getGroupType()));
        updateLdapServer.setGroupBaseDn(ldapServer.getGroupBaseDn());
        updateLdapServer.setGroupSubtree(ldapServer.getGroupSubtree());
        updateLdapServer.setGroupObjectClass(ldapServer.getGroupObjectClass());
        updateLdapServer.setGroupIdAttribute(ldapServer.getGroupIdAttribute());
        updateLdapServer.setGroupMemberAttribute(ldapServer.getGroupMemberAttribute());
        updateLdapServer.setGroupMemberFormat(ldapServer.getGroupMemberFormat());
        updateLdapServer.setUserMemberOfAttribute(ldapServer.getUserMemberOfAttribute());

        withApi(SecurityManagementLdapApi.class,
                api -> api.updateLdapServer(name, updateLdapServer),
                "Unable to update ldap server");
    }

    public List<String> getEnabledRealms() {
        return returnWithApi(SecurityManagementRealmsApi.class,
                api -> api.getActiveRealms(),
                "Unablt to list active realms");
    }

    public void setAnonymous(NexusAnonymousModel nexusAnonymousModel) {
        AnonymousAccessSettingsXO anonymousAccessSettingsXO = new AnonymousAccessSettingsXO()
                .enabled(nexusAnonymousModel.getEnabled())
                .realmName(nexusAnonymousModel.getRealmName())
                .userId(nexusAnonymousModel.getUserId());
        withApi(SecurityManagementAnonymousAccessApi.class,
                api -> api.update(anonymousAccessSettingsXO),
                "Unable to set anonymous access");
    }

    public List<RoleXOResponse> getRoles() {
        return returnWithApi(SecurityManagementRolesApi.class,
                api -> api.getRoles("default"),
                "Unable to get roles");
    }

    public void createRole(RoleXORequest roleXORequest) {
        withApi(SecurityManagementRolesApi.class,
                api -> api.create(roleXORequest),
                "Unable to create role %s".formatted(roleXORequest.getId()));
    }

    public void removeRole(RoleXORequest roleXORequest) {
        String roleId = roleXORequest.getId();
        if (Objects.equals(roleId, NX_ADMIN_ROLE) || roleId.equals(NX_ANONYMOUS_ROLE)) {
            LOG.finer("Ignoring pruning protected role " + roleId);
            return;
        }

        withApi(SecurityManagementRolesApi.class,
                api -> api.delete(roleId),
                "Unable to remove role %s".formatted(roleXORequest.getId()));
    }

    public void updateRole(RoleXORequest roleXORequest) {
        withApi(SecurityManagementRolesApi.class,
                api -> api.update1(roleXORequest.getId(), roleXORequest),
                "Unable to udpate role %s".formatted(roleXORequest.getId()));
    }

    public void setActiveRealms(List<String> newNames) {
        withApi(SecurityManagementRealmsApi.class,
                api -> api.setActiveRealms(newNames),
                "Unable to set active realms");
    }

    public List<AbstractApiRepository> getHosted() {
        return returnWithApi(RepositoryManagementApi.class,
                api -> api.getRepositories(),
                "Unable to list repositories")
                .stream()
                .filter(r -> r.getType().equals("hosted"))
                .collect(Collectors.toList());
    }

    public List<AbstractApiRepository> getProxies() {
        return returnWithApi(RepositoryManagementApi.class,
                api -> api.getRepositories(),
                "Unable to list repositories")
                .stream()
                .filter(r -> r.getType().equals("proxy"))
                .collect(Collectors.toList());
    }


    public List<AbstractApiRepository> getGroupRepos() {
        return returnWithApi(RepositoryManagementApi.class,
                api -> api.getRepositories(),
                "Unable to list repositories")
                .stream()
                .filter(r -> r.getType().equals("group"))
                .collect(Collectors.toList());
    }

    public void removeReposiory(AbstractApiRepository repoModel) {
        withApi(RepositoryManagementApi.class,
                api -> api.deleteRepository(repoModel.getName()),
                "Unable to delete rpository %s".formatted(repoModel.getName())
        );
    }

    public void createHosted(NexusHostedRepoModel repoModel) {
        withApi(RepositoryManagementApi.class,
                api -> {
                    switch (repoModel.getFormat()) {
                        case NPM: {
                            NpmHostedRepositoryApiRequest repo = toNpmHostedRepository(repoModel);
                            api.createRepository9(repo);
                            break;
                        }
                        case RAW: {
                            RawHostedRepositoryApiRequest repo = toRawHostedRepository(repoModel);
                            api.createRepository6(repo);
                            break;
                        }
                        case HELM: {
                            HelmHostedRepositoryApiRequest repo = toHelmHostedRepostiory(repoModel);
                            api.createRepository23(repo);
                            break;
                        }
                        case DOCKER: {
                            DockerHostedRepositoryApiRequest repo = toDockerHostedRepository(repoModel);
                            api.createRepository18(repo);
                            break;
                        }
                        case MAVEN: {
                            MavenHostedRepositoryApiRequest repo = toMavenHostedRepository(repoModel);
                            api.createRepository1(repo);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unhandled repository type %s".formatted(repoModel.getFormat()));
                    }
                },
                "Unable to create hosted repository %s".formatted(repoModel.getName())
        );

    }


    public void updateHosted(NexusHostedRepoModel repoModel) {
        withApi(RepositoryManagementApi.class,
                api -> {
                    switch (repoModel.getFormat()) {
                        case NPM: {
                            NpmHostedRepositoryApiRequest repo = toNpmHostedRepository(repoModel);
                            api.updateRepository9(repoModel.getName(), repo);
                            break;
                        }
                        case RAW: {
                            RawHostedRepositoryApiRequest repo = toRawHostedRepository(repoModel);
                            api.updateRepository6(repo.getName(), repo);
                            break;
                        }
                        case HELM: {
                            HelmHostedRepositoryApiRequest repo = toHelmHostedRepostiory(repoModel);
                            api.updateRepository23(repo.getName(), repo);
                            break;
                        }
                        case DOCKER: {
                            DockerHostedRepositoryApiRequest repo = toDockerHostedRepository(repoModel);
                            api.updateRepository18(repo.getName(), repo);
                            break;
                        }
                        case MAVEN: {
                            MavenHostedRepositoryApiRequest repo = toMavenHostedRepository(repoModel);
                            api.updateRepository1(repo.getName(), repo);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unhandled repository type %s".formatted(repoModel.getFormat()));
                    }
                },
                "Unable to update hosted repository %s".formatted(repoModel.getName())
        );

    }

    public void createProxy(NexusProxyRepoModel repoModel) {
        withApi(RepositoryManagementApi.class,
                api -> {
                    switch (repoModel.getFormat()) {
                        case NPM: {
                            NpmProxyRepositoryApiRequest repo = toNpmProxyRepository(repoModel);
                            api.createRepository10(repo);
                            break;
                        }
                        case RAW: {
                            RawProxyRepositoryApiRequest repo = toRawProxyRepository(repoModel);
                            api.createRepository7(repo);
                            break;
                        }
                        case HELM: {
                            HelmProxyRepositoryApiRequest repo = toHelmRProxyepostiory(repoModel);
                            api.createRepository24(repo);
                            break;
                        }
                        case DOCKER: {
                            DockerProxyRepositoryApiRequest repo = toDockerProxyRepository(repoModel);
                            api.createRepository19(repo);
                            break;
                        }
                        case MAVEN: {
                            MavenProxyRepositoryApiRequest repo = toMavenProxyRepository(repoModel);
                            api.createRepository2(repo);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unhandled repository type %s".formatted(repoModel.getFormat()));
                    }
                },
                "Unable to create proxy repository %s".formatted(repoModel.getName())
        );

    }

    public void updateProxy(NexusProxyRepoModel repoModel) {
        withApi(RepositoryManagementApi.class,
                api -> {
                    switch (repoModel.getFormat()) {
                        case NPM: {
                            NpmProxyRepositoryApiRequest repo = toNpmProxyRepository(repoModel);
                            api.updateRepository10(repoModel.getName(), repo);
                            break;
                        }
                        case RAW: {
                            RawProxyRepositoryApiRequest repo = toRawProxyRepository(repoModel);
                            api.updateRepository7(repo.getName(), repo);
                            break;
                        }
                        case HELM: {
                            HelmProxyRepositoryApiRequest repo = toHelmRProxyepostiory(repoModel);
                            api.updateRepository24(repo.getName(), repo);
                            break;
                        }
                        case DOCKER: {
                            DockerProxyRepositoryApiRequest repo = toDockerProxyRepository(repoModel);
                            api.updateRepository19(repo.getName(), repo);
                            break;
                        }
                        case MAVEN: {
                            MavenProxyRepositoryApiRequest repo = toMavenProxyRepository(repoModel);
                            api.updateRepository2(repo.getName(), repo);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unhandled repository type %s".formatted(repoModel.getFormat()));
                    }
                },
                "Unable to update proxy repository %s".formatted(repoModel.getName())
        );

    }

    public void createGroup(NexusGroupRepoModel repoModel) {
        withApi(RepositoryManagementApi.class,
                api -> {
                    switch (repoModel.getFormat()) {
                        case NPM: {
                            NpmGroupRepositoryApiRequest repo = toNpmGroupRepository(repoModel);
                            api.createRepository8(repo);
                            break;
                        }
                        case RAW: {
                            RawGroupRepositoryApiRequest repo = toRawGroupRepository(repoModel);
                            api.createRepository5(repo);
                            break;
                        }
                        case DOCKER: {
                            DockerGroupRepositoryApiRequest repo = toDockerGroupRepository(repoModel);
                            api.createRepository17(repo);
                            break;
                        }
                        case MAVEN: {
                            MavenGroupRepositoryApiRequest repo = toMavenGroupRepository(repoModel);
                            api.createRepository(repo);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unhandled repository type %s".formatted(repoModel.getFormat()));
                    }
                },
                "Unable to create group repository %s".formatted(repoModel.getName())
        );
    }

    public void updateGroup(NexusGroupRepoModel repoModel) {
        withApi(RepositoryManagementApi.class,
                api -> {
                    switch (repoModel.getFormat()) {
                        case NPM: {
                            NpmGroupRepositoryApiRequest repo = toNpmGroupRepository(repoModel);
                            api.updateRepository8(repo.getName(), repo);
                            break;
                        }
                        case RAW: {
                            RawGroupRepositoryApiRequest repo = toRawGroupRepository(repoModel);
                            api.updateRepository5(repo.getName(), repo);
                            break;
                        }
                        case DOCKER: {
                            DockerGroupRepositoryApiRequest repo = toDockerGroupRepository(repoModel);
                            api.updateRepository17(repo.getName(), repo);
                            break;
                        }
                        case MAVEN: {
                            MavenGroupRepositoryApiRequest repo = toMavenGroupRepository(repoModel);

                            api.updateRepository(repo.getName(), repo);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unhandled repository type %s".formatted(repoModel.getFormat()));
                    }
                },
                "Unable to update group repository %s".formatted(repoModel.getName()));
    }


    public SimpleApiGroupRepository getMavenGroup(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository1(name),
                "Unable to get maven group repository %s".formatted(name));
    }

    public MavenHostedApiRepository getMavenHosted(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository2(name),
                "Unable to get maven hosted repository %s".formatted(name));
    }

    public MavenProxyApiRepository getMavenProxy(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository3(name),
                "Unable to get maven proxy repository %s".formatted(name));
    }

    public DockerGroupApiRepository getDockerGroup(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository18(name),
                "Unable to get docker group repository %s".formatted(name));
    }

    public DockerHostedApiRepository getDockerHosted(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository19(name),
                "Unable to get docker hosted repository %s".formatted(name));
    }

    public DockerProxyApiRepository getDockerProxy(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository20(name),
                "Unable to get docker proxy repository %s".formatted(name));
    }

    public SimpleApiGroupDeployRepository getNpmGroup(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository9(name),
                "Unable to get npm group repository %s".formatted(name));
    }

    public SimpleApiHostedRepository getNpmHosted(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository10(name),
                "Unable to get npm hosted repository %s".formatted(name));
    }

    public NpmProxyApiRepository getNpmProxy(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository11(name),
                "Unable to get npm proxy repository %s".formatted(name));
    }

    public SimpleApiGroupRepository getRawGroup(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository6(name),
                "Unable to get raw group repository %s".formatted(name));
    }

    public SimpleApiHostedRepository getRawHosted(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository7(name),
                "Unable to get raw hosted repository %s".formatted(name));
    }

    public SimpleApiProxyRepository getRawProxy(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository8(name),
                "Unable to get raw proxy repository %s".formatted(name));
    }

    public SimpleApiHostedRepository getHelmHosted(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository24(name),
                "Unable to get helm hosted repository %s".formatted(name));
    }

    public SimpleApiProxyRepository getHelmProxy(String name) {
        return returnWithApi(RepositoryManagementApi.class, api -> api.getRepository25(name),
                "Unable to get helm proxy repository %s".formatted(name));
    }

    public List<ApiUser> getUsers() {
        return returnWithApi(SecurityManagementUsersApi.class, api -> api.getUsers(null, "default"),
                "Unable to get users");
    }


    public void createUser(NexusUserModel model, String password) {
        ApiCreateUser.StatusEnum status = Optional.ofNullable(model.getStatus())
                .map(ApiCreateUser.StatusEnum::fromValue)
                .orElse(ApiCreateUser.StatusEnum.ACTIVE);
        ApiCreateUser createUser = new ApiCreateUser()
                .userId(model.getId())
                .emailAddress(model.getEmail())
                .firstName(model.getFirstName())
                .lastName(model.getLastName())
                .status(status)
                .password(password)
                .roles(model.getRoles());

        withApi(SecurityManagementUsersApi.class, api -> api.createUser(createUser),
                "Unable to create user %s".formatted(createUser.getUserId()));
    }

    public void updateUser(NexusUserModel model, Optional<String> password) {
        ApiUser apiUser = toApiUser(model);
        withApi(SecurityManagementUsersApi.class, api -> api.updateUser(model.getId(), apiUser),
                "Unable to update user " + model.getId());

        String passwordNullable = password.orElse(null);
        if (passwordNullable != null) {
            withApi(SecurityManagementUsersApi.class, api -> api.changePassword(apiUser.getUserId(), passwordNullable),
                    "Unable to set user password for " + model.getId());
        }
    }

    public <API> void removeUser(ApiUser apiUser) {
        if (apiUser.getUserId().equalsIgnoreCase(NX_ADMIN_USER)
                || apiUser.getUserId().equalsIgnoreCase(NX_ANONYMOUS_ROLE)) {
            LOG.fine("Ignoring pruning protected user " + apiUser.getUserId());
            return;
        }
        withApi(SecurityManagementUsersApi.class, api -> api.deleteUser(apiUser.getUserId()),
                "Unable to delete user " + apiUser.getUserId());
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
                .storage(new DockerHostedStorageAttributes()
                        .blobStoreName(repoModel.getBlobStore())
                        .strictContentTypeValidation(repoModel.getStrictContentValidation())
                        .writePolicy(DockerHostedStorageAttributes.WritePolicyEnum.valueOf(repoModel.getWritePolicy().name()))
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

    private <T> void withApi(Class<T> apiType, ThrowingConsumer<T> consumer, String errorMessage) {
        withApi(apiType, this.password, consumer, errorMessage);
    }

    private <T, U> U returnWithApi(Class<T> apiType, ThrowingFunction<T, U> consumer, String errorMessage) {
        return returnWithApi(apiType, this.password, consumer, errorMessage);
    }

    private <T> void withApi(Class<T> apiType, String password, ThrowingConsumer<T> consumer, String errorMessage) {
        T api = createApi(apiType, uri, password);
        try {
            consumer.consume(api);
        } catch (Exception e) {
            throw new ClientException("%s: %s".formatted(errorMessage, e.getMessage()), e);
        }
        try {
            ((Closeable) api).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T, U> U returnWithApi(Class<T> apiType, String password, ThrowingFunction<T, U> consumer, String errorMessage) {
        T api = createApi(apiType, uri, password);
        U value;
        try {
            value = consumer.apply(api);
        } catch (Exception e) {
            throw new ClientException("%s: %s".formatted(errorMessage, e.getMessage()), e);
        }
        try {
            ((Closeable) api).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    private <T> T createApi(Class<T> apiType, URI uri, String adminPassword) {
        ResteasyJackson2Provider component = new ResteasyJackson2Provider();
        component.setMapper(jsonMapper);

        return RestClientBuilder.newBuilder()
                .register(component)
                .register(new DefaultTextPlain())
                .register(new StringTextStar())
                .register(addAuthHeader(adminPassword))
                .baseUri(uri)
                .build(apiType);
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

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void consume(T value) throws Exception;
    }

    @FunctionalInterface
    interface ThrowingFunction<T, U> {
        U apply(T value) throws Exception;
    }
}
