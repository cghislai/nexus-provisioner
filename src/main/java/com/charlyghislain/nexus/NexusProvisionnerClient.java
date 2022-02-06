package com.charlyghislain.nexus;

import com.charlyghislain.nexus.nexus.AnonymousAccessSettingsXO;
import com.charlyghislain.nexus.nexus.ApiCreateUser;
import com.charlyghislain.nexus.nexus.ApiUser;
import com.charlyghislain.nexus.nexus.DockerAttributes;
import com.charlyghislain.nexus.nexus.DockerHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.GroupAttributes;
import com.charlyghislain.nexus.nexus.HelmHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.HostedStorageAttributes;
import com.charlyghislain.nexus.nexus.HttpClientAttributesWithPreemptiveAuth;
import com.charlyghislain.nexus.nexus.MavenAttributes;
import com.charlyghislain.nexus.nexus.MavenGroupRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.MavenProxyRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.NegativeCacheAttributes;
import com.charlyghislain.nexus.nexus.NpmHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.ProxyAttributes;
import com.charlyghislain.nexus.nexus.RawAttributes;
import com.charlyghislain.nexus.nexus.RawHostedRepositoryApiRequest;
import com.charlyghislain.nexus.nexus.RoleXORequest;
import com.charlyghislain.nexus.nexus.RoleXOResponse;
import com.charlyghislain.nexus.nexus.SimpleApiGroupRepository;
import com.charlyghislain.nexus.nexus.StorageAttributes;
import com.charlyghislain.nexus.nexus.V1Api;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NexusProvisionnerClient {
    private final static Logger LOG = Logger.getLogger(NexusProvisionnerClient.class.getName());

    private NexusProvisionerConfig clientConfig;

    public NexusProvisionnerClient(NexusProvisionerConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public void provision(String initalPassword) throws IOException {
        V1Api apiInitialAUth = getApi(initalPassword);
        String adminPassword = clientConfig.getAdminPassword();
        try {
            apiInitialAUth.changePassword("admin", adminPassword);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Ignoring change initial admin pw error " + e.getMessage());
        } finally {
            ((Closeable) apiInitialAUth).close();
        }

        V1Api api = getApi(adminPassword);
        try {
            initNexus(api);

            createUserRoles(api);

            createRepositories(api);

            updateRepoAccesses(api);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ((Closeable) api).close();
        }

    }

    private void initNexus(V1Api api) {
        api.update(new AnonymousAccessSettingsXO()
                .enabled(true)
                .userId("anonymous")
                .realmName("NexusAuthorizingRealm")
        );

        api.setActiveRealms(List.of(
                "NexusAuthenticatingRealm",
                "NexusAuthorizingRealm",
                "NpmToken"
        ));
    }

    private void createUserRoles(V1Api api) {
        try {
            api.getRole("jenkins", "default");
        } catch (WebApplicationException e) {
            boolean roleNotFound = Optional.ofNullable(e.getResponse())
                    .map(Response::getStatus)
                    .filter(s -> s == 404)
                    .isPresent();
            if (roleNotFound) {
                api.create(new RoleXORequest()
                        .id("jenkins")
                        .name("jenkins")
                        .description("jenkins")
                        .privileges(List.of(
                                "nx-repository-view-*-*-*",
                                "nx-component-upload"
                        ))
                        .roles(List.of(
                                "nx-anonymous"
                        ))
                );
            } else {
                throw e;
            }
        }

        try {
            api.getRole("public-repo-view", "default");
        } catch (WebApplicationException e) {
            boolean roleNotFound = Optional.ofNullable(e.getResponse())
                    .map(Response::getStatus)
                    .filter(s -> s == 404)
                    .isPresent();
            if (roleNotFound) {
                api.create(new RoleXORequest()
                        .id("public-repo-view")
                        .name("public-repo-view")
                        .description("Public repo view")
                        .privileges(List.of(
                                "nx-repository-view-helm-*-read"
                        ))
                        .roles(List.of(
                        ))
                );
            } else {
                throw e;
            }
        }


        // Create jenkins users as required
        String mainDomain = clientConfig.getMainDomain();
        Map<String, String> jenkinsRoleAccounts = clientConfig.getJenkinsRoleAccounts();
        for (String userName : jenkinsRoleAccounts.keySet()) {
            String userPass = jenkinsRoleAccounts.get(userName);
            List<ApiUser> foundUsers = api.getUsers(userName, "default");
            if (foundUsers.isEmpty()) {
                api.createUser(new ApiCreateUser()
                        .userId(userName)
                        .firstName(userName)
                        .lastName("Jenkins")
                        .emailAddress(userName + "@" + mainDomain)
                        .password(userPass)
                        .status(ApiCreateUser.StatusEnum.ACTIVE)
                        .roles(List.of("jenkins"))
                );
            }
        }


        api.updateUser("anonymous", new ApiUser()
                .userId("anonymous")
                .firstName("anon")
                .lastName("ymous")
                .emailAddress("anonymous@" + mainDomain)
                .status(ApiUser.StatusEnum.ACTIVE)
                .source("default")
                .roles(List.of(
                        "public-repo-view"
                ))
        );

    }

    private void updateRepoAccesses(V1Api api) {
        RoleXOResponse publicRole = api.getRole("public-repo-view", "default");

        List<String> privleges = new ArrayList<>(publicRole.getPrivileges());
        if (clientConfig.isRepoRawPublic()) {
            privleges.add("nx-repository-view-raw-rawPublic-read");
            privleges.add("nx-repository-view-raw-rawPublic-browse");
        }
        api.update1("public-repo-view", new RoleXORequest()
                .privileges(privleges)
                .id("public-repo-view")
                .name("public-repo-view")
                .description("Public repo view")
        );
    }

    private void createRepositories(V1Api api) {
        tryDeleteRepository(api, "nuget-group");
        tryDeleteRepository(api, "nuget-hosted");
        tryDeleteRepository(api, "nuget.org-proxy");

        if (clientConfig.isRepoNpm()) {
            tryCreateRepository("npm", api::getRepository9,
                    () -> api.createRepository9(new NpmHostedRepositoryApiRequest()
                            .name("npm")
                            .online(true)
                            .storage(new HostedStorageAttributes()
                                    .blobStoreName("default")
                                    .strictContentTypeValidation(true)
                                    .writePolicy(HostedStorageAttributes.WritePolicyEnum.ALLOW)
                            ))
            );
        }

        if (clientConfig.isRepoRaw()) {
            tryCreateRepository("raw", api::getRepository6,
                    () -> api.createRepository6(new RawHostedRepositoryApiRequest()
                            .name("raw")
                            .online(true)
                            .storage(new HostedStorageAttributes()
                                    .blobStoreName("default")
                                    .strictContentTypeValidation(true)
                                    .writePolicy(HostedStorageAttributes.WritePolicyEnum.ALLOW)
                            )
                            .raw(new RawAttributes()
                                    .contentDisposition(RawAttributes.ContentDispositionEnum.ATTACHMENT)
                            )
                    ));
        }

        if (clientConfig.isRepoRawPublic()) {
            tryCreateRepository("rawPublic", api::getRepository6,
                    () -> api.createRepository6(new RawHostedRepositoryApiRequest()
                            .name("rawPublic")
                            .online(true)
                            .storage(new HostedStorageAttributes()
                                    .blobStoreName("default")
                                    .strictContentTypeValidation(true)
                                    .writePolicy(HostedStorageAttributes.WritePolicyEnum.ALLOW)
                            )
                            .raw(new RawAttributes()
                                    .contentDisposition(RawAttributes.ContentDispositionEnum.ATTACHMENT)
                            )
                    ));
        }

        if (clientConfig.isRepoHelm()) {
            tryCreateRepository("helm", api::getRepository23,
                    () -> api.createRepository23(new HelmHostedRepositoryApiRequest()
                            .name("helm")
                            .online(true)
                            .storage(new HostedStorageAttributes()
                                    .blobStoreName("default")
                                    .strictContentTypeValidation(true)
                                    .writePolicy(HostedStorageAttributes.WritePolicyEnum.ALLOW)
                            )
                    )
            );
        }

        if (clientConfig.isRepoDocker()) {
            tryCreateRepository("docker", api::getRepository18,
                    () -> api.createRepository18(new DockerHostedRepositoryApiRequest()
                            .name("docker")
                            .online(true)
                            .storage(new HostedStorageAttributes()
                                    .blobStoreName("default")
                                    .strictContentTypeValidation(true)
                                    .writePolicy(HostedStorageAttributes.WritePolicyEnum.ALLOW)
                            )
                            .docker(new DockerAttributes()
                                    .httpPort(1080)
                                    .forceBasicAuth(true)
                                    .v1Enabled(false)
                            )
                    ));
        }

        List<String> mavenProxies = new ArrayList<>();
        if (clientConfig.isRepoMavenProxyPrevious()) {
            String previousNexusUrl = Optional.ofNullable(clientConfig.getMavenPreviousProxyUrl())
                    .orElse("https://nexus.valuya.com/nexus/repository/maven-public");
            String repoName = "maven-previous-nexus";
            mavenProxies.add(repoName);

            tryCreateMavenProxyRepo(api, repoName, previousNexusUrl);
        }
        if (clientConfig.isRepoMavenProxyRedHat()) {
            String repoName = "wildfly-redhat-proxy";
            mavenProxies.add(repoName);
            tryCreateMavenProxyRepo(api, repoName, "https://maven.repository.redhat.com/ga/");
        }
        if (clientConfig.isRepoMavenProxyItext()) {
            String repoName = "itext-proxy";
            mavenProxies.add(repoName);
            tryCreateMavenProxyRepo(api, repoName, "http://maven.icm.edu.pl/artifactory/repo/");
        }
        if (clientConfig.isRepoMavenProxyJasperReport()) {
            String repoName = "jasperreport-proxy";
            mavenProxies.add(repoName);
            tryCreateMavenProxyRepo(api, repoName, "https://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts/");
        }
        if (clientConfig.isRepoMavenProxyCefDigital()) {
            String repoName = "ac-europa-cefdigital";
            mavenProxies.add(repoName);
            tryCreateMavenProxyRepo(api, repoName, "https://ec.europa.eu/cefdigital/artifact/content/repositories/esignaturedss");
        }

        if (clientConfig.isRepoMavenProxyJbossPublic()) {
            String repoName = "jboss-public-repository-group";
            mavenProxies.add(repoName);
            tryCreateMavenProxyRepo(api, repoName, "https://repository.jboss.org/nexus/content/groups/public/");
        }


        // Ensure all repos are available at maven-public
        Set<String> mavenPublicRepos = new HashSet<>();
        mavenPublicRepos.add("maven-releases");
        mavenPublicRepos.add("maven-snapshots");
        mavenPublicRepos.add("maven-central");
        mavenPublicRepos.addAll(mavenProxies);

        SimpleApiGroupRepository mavenPublicGroup = api.getRepository("maven-public");
        List<String> currentMembersNames = mavenPublicGroup.getGroup().getMemberNames();
        mavenPublicRepos.addAll(currentMembersNames);

        api.updateRepository("maven-public", new MavenGroupRepositoryApiRequest()
                .name("maven-public")
                .online(true)
                .storage(new StorageAttributes()
                        .strictContentTypeValidation(true)
                        .blobStoreName("default")
                )
                .group(new GroupAttributes()
                        .memberNames(new ArrayList<>(mavenPublicRepos))
                )
        );
    }

    private void tryCreateMavenProxyRepo(V1Api api, String repoName, String remoteUrl) {
        tryCreateRepository(repoName, api::getRepository2,
                () -> api.createRepository2(new MavenProxyRepositoryApiRequest()
                        .name(repoName)
                        .online(true)
                        .proxy(new ProxyAttributes()
                                .remoteUrl(remoteUrl)
                                .contentMaxAge(-1)
                                .metadataMaxAge(1440)
                        )
                        .storage(new StorageAttributes()
                                .blobStoreName("default")
                                .strictContentTypeValidation(true)
                        )
                        .negativeCache(new NegativeCacheAttributes()
                                .timeToLive(1440)
                                .enabled(true)
                        )
                        .maven(new MavenAttributes()
                                .layoutPolicy(MavenAttributes.LayoutPolicyEnum.STRICT)
                                .versionPolicy(MavenAttributes.VersionPolicyEnum.MIXED)
                        )
                        .httpClient(new HttpClientAttributesWithPreemptiveAuth()
                                .autoBlock(true)
                                .blocked(false)
                        )
                )
        );
    }

    private <R> void tryCreateRepository(String name, Function<String, R> repoGetter, Runnable repoFactory) {
        try {
            R existingRepo = repoGetter.apply(name);
            LOG.log(Level.FINE, "Skipping exising repo " + name);
        } catch (WebApplicationException e) {
            boolean repoNotFound = Optional.ofNullable(e.getResponse())
                    .map(Response::getStatus)
                    .filter(s -> s == 404)
                    .isPresent();
            if (repoNotFound) {
                LOG.log(Level.INFO, "Creating repo " + name);
                repoFactory.run();
            } else {
                throw e;
            }
        }
    }

    private void tryDeleteRepository(V1Api api, String repoName) {
        try {
            api.deleteRepository(repoName);
        } catch (WebApplicationException e) {
            boolean notFound = Optional.ofNullable(e.getResponse())
                    .map(Response::getStatus)
                    .filter(s -> s == 404)
                    .isPresent();
            if (notFound) {
                return;
            } else {
                throw e;
            }
        }
        return;
    }

    private V1Api getApi(String adminInitialPassword) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        mapper.disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        ResteasyJackson2Provider component = new ResteasyJackson2Provider();
        component.setMapper(mapper);

        return RestClientBuilder.newBuilder()
                .register(component)
                .register(new DefaultTextPlain())
                .register(new StringTextStar())
                .register(addAuthHeader(adminInitialPassword))
                .baseUri(clientConfig.getNexusUri())
                .build(V1Api.class);
    }

    private ClientRequestFilter addAuthHeader(String adminInitialPassword) {
        return requestContext -> {
            String encodedCreds = Base64.getEncoder().encodeToString(("admin:" + adminInitialPassword).getBytes(StandardCharsets.UTF_8));
            requestContext.getHeaders().putSingle("authorization", "Basic " + encodedCreds);
        };
    }
}
