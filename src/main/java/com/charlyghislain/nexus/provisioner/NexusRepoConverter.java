package com.charlyghislain.nexus.provisioner;

import com.charlyghislain.nexus.client.ClientRuntimeError;
import com.charlyghislain.nexus.client.KubernetesClient;
import com.charlyghislain.nexus.client.NexusClient;
import com.charlyghislain.nexus.config.*;
import com.charlyghislain.nexus.nexus.*;

import java.util.List;
import java.util.Optional;

public class NexusRepoConverter {

    private final KubernetesClient kubernetesClient;
    private final NexusClient nexusClient;
    private final CollectionReconciliator reconciliator;

    public NexusRepoConverter(KubernetesClient kubernetesClient, NexusClient nexusClient,
                              CollectionReconciliator reconciliator) {
        this.kubernetesClient = kubernetesClient;
        this.nexusClient = nexusClient;
        this.reconciliator = reconciliator;
    }

    public void patchHostedModel(NexusHostedRepoModel roleModel, RepositoryXO apiRepository) {

        String format = getFormat(roleModel.getFormat(), apiRepository);
        switch (format) {
            case "maven2": {
                patchMavenHosted(roleModel, apiRepository.getName());
                break;
            }
            case "docker": {
                patchDockerHosted(roleModel, apiRepository.getName());
                break;
            }
            case "npm": {
                patchNpmHosted(roleModel, apiRepository.getName());
                break;
            }
            case "helm": {
                patchHelmHosted(roleModel, apiRepository.getName());
                break;
            }
            case "raw": {
                patchRawHosted(roleModel, apiRepository.getName());
                break;
            }
            case "pypi": {
                patchPypiHosted(roleModel, apiRepository.getName());
                break;
            }
            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }

    }

    public void patchProxyModel(NexusProxyRepoModel roleModel, RepositoryXO apiRepository) {

        String format = getFormat(roleModel.getFormat(), apiRepository);
        switch (format) {
            case "maven2": {
                patchMavenProxy(roleModel, apiRepository.getName());
                break;
            }
            case "docker": {
                patchDockerProxy(roleModel, apiRepository.getName());
                break;
            }
            case "npm": {
                patchNpmProxy(roleModel, apiRepository.getName());
                break;
            }
            case "helm": {
                patchHelmProxy(roleModel, apiRepository.getName());
                break;
            }
            case "raw": {
                patchRawProxy(roleModel, apiRepository.getName());
                break;
            }
            case "pypi": {
                patchPypiProxy(roleModel, apiRepository.getName());
                break;
            }
            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }

    }

    public void patchGroupModel(NexusGroupRepoModel roleModel, RepositoryXO apiRepository) {

        String format = getFormat(roleModel.getFormat(), apiRepository);
        switch (format) {
            case "maven2": {
                patchMavenGroup(roleModel, apiRepository.getName());
                break;
            }
            case "docker": {
                patchDockerGroup(roleModel, apiRepository.getName());
                break;
            }
            case "npm": {
                patchNpmGroup(roleModel, apiRepository.getName());
                break;
            }
            case "helm": {
                throw new UnsupportedOperationException("Not yet implemented");
            }
            case "raw": {
                patchRawGroup(roleModel, apiRepository.getName());
                break;
            }
            case "pypi": {
                patchPypiGroup(roleModel, apiRepository.getName());
                break;
            }
            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }

    }

    private void patchNpmHosted(NexusHostedRepoModel hostedModel, String name) {
        SimpleApiHostedRepository npmHosted = nexusClient.getNpmHosted(name);
        hostedModel.setFormat(NexusRepoFormat.NPM);

        patchSimpleHosted(hostedModel, npmHosted);

    }

    private void patchNpmProxy(NexusProxyRepoModel proxyModel, String name) {
        NpmProxyApiRepository npmProxy = nexusClient.getNpmProxy(name);
        proxyModel.setFormat(NexusRepoFormat.NPM);

        Optional.ofNullable(npmProxy.getName())
                .ifPresent(proxyModel::setName);

        Optional.ofNullable(npmProxy.getOnline())
                .ifPresent(proxyModel::setOnline);

        Optional.ofNullable(npmProxy.getStorage())
                .map(StorageAttributes::getBlobStoreName)
                .ifPresent(proxyModel::setBlobStore);

        Optional.ofNullable(npmProxy.getStorage())
                .map(StorageAttributes::getStrictContentTypeValidation)
                .ifPresent(proxyModel::setStrictContentValidation);

        Optional.ofNullable(npmProxy.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(proxyModel::setCleanupPolicies);


        Optional.ofNullable(npmProxy.getProxy())
                .map(ProxyAttributes::getContentMaxAge)
                .ifPresent(proxyModel::setMaxAge);

        Optional.ofNullable(npmProxy.getProxy())
                .map(ProxyAttributes::getRemoteUrl)
                .ifPresent(proxyModel::setUri);

        Optional.ofNullable(npmProxy.getProxy())
                .map(ProxyAttributes::getMetadataMaxAge)
                .ifPresent(proxyModel::setMetadataMaxAge);

        Optional.ofNullable(npmProxy.getNegativeCache())
                .map(NegativeCacheAttributes::getEnabled)
                .ifPresent(proxyModel::setNegativeCache);

        Optional.ofNullable(npmProxy.getNegativeCache())
                .map(NegativeCacheAttributes::getTimeToLive)
                .ifPresent(proxyModel::setNegativeCacheTTl);


        Optional.ofNullable(npmProxy.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(proxyModel::setCleanupPolicies);
    }

    private void patchNpmGroup(NexusGroupRepoModel groupModel, String name) {
        SimpleApiGroupDeployRepository npmGroup = nexusClient.getNpmGroup(name);
        groupModel.setFormat(NexusRepoFormat.NPM);

        Optional.ofNullable(npmGroup.getName())
                .ifPresent(groupModel::setName);

        Optional.ofNullable(npmGroup.getOnline())
                .ifPresent(groupModel::setOnline);

        Optional.ofNullable(npmGroup.getStorage())
                .map(StorageAttributes::getBlobStoreName)
                .ifPresent(groupModel::setBlobStore);

        Optional.ofNullable(npmGroup.getStorage())
                .map(StorageAttributes::getStrictContentTypeValidation)
                .ifPresent(groupModel::setStrictContentValidation);

        List<String> memberNames = groupModel.getMemberNames();
        Optional.ofNullable(npmGroup.getGroup())
                .map(GroupDeployAttributes::getMemberNames)
                .map(list -> reconciliator.reconcileNames("Group members", memberNames, list))
                .ifPresent(groupModel::setMemberNames);

    }

    private void patchRawHosted(NexusHostedRepoModel hostedModel, String name) {
        SimpleApiHostedRepository rawHosted = nexusClient.getRawHosted(name);
        hostedModel.setFormat(NexusRepoFormat.RAW);

        patchSimpleHosted(hostedModel, rawHosted);

    }

    private void patchRawProxy(NexusProxyRepoModel proxyModel, String name) {
        SimpleApiProxyRepository rawProxy = nexusClient.getRawProxy(name);
        proxyModel.setFormat(NexusRepoFormat.RAW);

        patchSimpleProxy(proxyModel, rawProxy);
    }

    private void patchRawGroup(NexusGroupRepoModel groupModel, String name) {
        SimpleApiGroupRepository rawGroup = nexusClient.getRawGroup(name);
        groupModel.setFormat(NexusRepoFormat.RAW);

        patchSimpleGroup(groupModel, rawGroup);

    }

    private void patchPypiHosted(NexusHostedRepoModel hostedModel, String name) {
        SimpleApiHostedRepository pypiHosted = nexusClient.getPypiHosted(name);
        hostedModel.setFormat(NexusRepoFormat.PYPI);

        patchSimpleHosted(hostedModel, pypiHosted);

    }

    private void patchPypiProxy(NexusProxyRepoModel proxyModel, String name) {
        SimpleApiProxyRepository pypiProxy = nexusClient.getPypiProxy(name);
        proxyModel.setFormat(NexusRepoFormat.PYPI);

        patchSimpleProxy(proxyModel, pypiProxy);
    }

    private void patchPypiGroup(NexusGroupRepoModel groupModel, String name) {
        SimpleApiGroupRepository pypiGroup = nexusClient.getPypiGroup(name);
        groupModel.setFormat(NexusRepoFormat.PYPI);

        patchSimpleGroup(groupModel, pypiGroup);

    }

    private void patchDockerGroup(NexusGroupRepoModel groupModel, String name) {
        DockerGroupApiRepository dockerGroup = nexusClient.getDockerGroup(name);
        groupModel.setFormat(NexusRepoFormat.DOCKER);

        Optional.ofNullable(dockerGroup.getName())
                .ifPresent(groupModel::setName);

        Optional.ofNullable(dockerGroup.getOnline())
                .ifPresent(groupModel::setOnline);

        Optional.ofNullable(dockerGroup.getStorage())
                .map(StorageAttributes::getBlobStoreName)
                .ifPresent(groupModel::setBlobStore);

        Optional.ofNullable(dockerGroup.getStorage())
                .map(StorageAttributes::getStrictContentTypeValidation)
                .ifPresent(groupModel::setStrictContentValidation);

        List<String> memberNames = groupModel.getMemberNames();
        Optional.ofNullable(dockerGroup.getGroup())
                .map(GroupDeployAttributes::getMemberNames)
                .map(list -> reconciliator.reconcileNames("Group members", memberNames, list))
                .ifPresent(groupModel::setMemberNames);

    }

    private void patchMavenGroup(NexusGroupRepoModel groupModel, String name) {
        SimpleApiGroupRepository mavenGroup = nexusClient.getMavenGroup(name);
        groupModel.setFormat(NexusRepoFormat.MAVEN);

        patchSimpleGroup(groupModel, mavenGroup);

    }

    private void patchSimpleGroup(NexusGroupRepoModel groupModel, SimpleApiGroupRepository rawGroup) {
        Optional.ofNullable(rawGroup.getName())
                .ifPresent(groupModel::setName);

        Optional.ofNullable(rawGroup.getOnline())
                .ifPresent(groupModel::setOnline);


        Optional.ofNullable(rawGroup.getStorage())
                .map(StorageAttributes::getBlobStoreName)
                .ifPresent(groupModel::setBlobStore);

        Optional.ofNullable(rawGroup.getStorage())
                .map(StorageAttributes::getStrictContentTypeValidation)
                .ifPresent(groupModel::setStrictContentValidation);

        List<String> memberNames = groupModel.getMemberNames();
        Optional.ofNullable(rawGroup.getGroup())
                .map(GroupAttributes::getMemberNames)
                .map(list -> reconciliator.reconcileNames("Group members", memberNames, list))
                .ifPresent(groupModel::setMemberNames);
    }


    private void patchHelmHosted(NexusHostedRepoModel hostedModel, String name) {
        SimpleApiHostedRepository helmHosted = nexusClient.getHelmHosted(name);
        hostedModel.setFormat(NexusRepoFormat.HELM);

        patchSimpleHosted(hostedModel, helmHosted);

    }

    private void patchSimpleHosted(NexusHostedRepoModel hostedModel, SimpleApiHostedRepository helmHosted) {
        Optional.ofNullable(helmHosted.getName())
                .ifPresent(hostedModel::setName);

        Optional.ofNullable(helmHosted.getOnline())
                .ifPresent(hostedModel::setOnline);

        Optional.ofNullable(helmHosted.getStorage())
                .map(HostedStorageAttributes::getBlobStoreName)
                .ifPresent(hostedModel::setBlobStore);

        Optional.ofNullable(helmHosted.getStorage())
                .map(HostedStorageAttributes::getStrictContentTypeValidation)
                .ifPresent(hostedModel::setStrictContentValidation);

        Optional.ofNullable(helmHosted.getStorage())
                .map(HostedStorageAttributes::getWritePolicy)
                .map(p -> WritePolicy.valueOf(p.name()))
                .ifPresent(hostedModel::setWritePolicy);

        Optional.ofNullable(helmHosted.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(hostedModel::setCleanupPolicies);
    }

    private void patchHelmProxy(NexusProxyRepoModel proxyModel, String name) {
        SimpleApiProxyRepository helmProxy = nexusClient.getHelmProxy(name);
        proxyModel.setFormat(NexusRepoFormat.HELM);

        patchSimpleProxy(proxyModel, helmProxy);
    }

    private void patchSimpleProxy(NexusProxyRepoModel proxyModel, SimpleApiProxyRepository helmProxy) {
        Optional.ofNullable(helmProxy.getName())
                .ifPresent(proxyModel::setName);

        Optional.ofNullable(helmProxy.getOnline())
                .ifPresent(proxyModel::setOnline);

        Optional.ofNullable(helmProxy.getStorage())
                .map(StorageAttributes::getBlobStoreName)
                .ifPresent(proxyModel::setBlobStore);

        Optional.ofNullable(helmProxy.getStorage())
                .map(StorageAttributes::getStrictContentTypeValidation)
                .ifPresent(proxyModel::setStrictContentValidation);

        Optional.ofNullable(helmProxy.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(proxyModel::setCleanupPolicies);


        Optional.ofNullable(helmProxy.getProxy())
                .map(ProxyAttributes::getContentMaxAge)
                .ifPresent(proxyModel::setMaxAge);

        Optional.ofNullable(helmProxy.getProxy())
                .map(ProxyAttributes::getRemoteUrl)
                .ifPresent(proxyModel::setUri);

        Optional.ofNullable(helmProxy.getProxy())
                .map(ProxyAttributes::getMetadataMaxAge)
                .ifPresent(proxyModel::setMetadataMaxAge);

        Optional.ofNullable(helmProxy.getNegativeCache())
                .map(NegativeCacheAttributes::getEnabled)
                .ifPresent(proxyModel::setNegativeCache);

        Optional.ofNullable(helmProxy.getNegativeCache())
                .map(NegativeCacheAttributes::getTimeToLive)
                .ifPresent(proxyModel::setNegativeCacheTTl);


        Optional.ofNullable(helmProxy.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(proxyModel::setCleanupPolicies);
    }


    private void patchDockerHosted(NexusHostedRepoModel hostedModel, String name) {
        DockerHostedApiRepository dockerHosted = nexusClient.getDockerHosted(name);
        hostedModel.setFormat(NexusRepoFormat.DOCKER);


        Optional.ofNullable(dockerHosted.getName())
                .ifPresent(hostedModel::setName);

        Optional.ofNullable(dockerHosted.getOnline())
                .ifPresent(hostedModel::setOnline);

        Optional.ofNullable(dockerHosted.getStorage())
                .map(HostedStorageAttributes::getBlobStoreName)
                .ifPresent(hostedModel::setBlobStore);

        Optional.ofNullable(dockerHosted.getStorage())
                .map(HostedStorageAttributes::getStrictContentTypeValidation)
                .ifPresent(hostedModel::setStrictContentValidation);

        Optional.ofNullable(dockerHosted.getStorage())
                .map(HostedStorageAttributes::getWritePolicy)
                .map(p -> WritePolicy.valueOf(p.name()))
                .ifPresent(hostedModel::setWritePolicy);

        Optional.ofNullable(dockerHosted.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(hostedModel::setCleanupPolicies);

        Optional.ofNullable(dockerHosted.getDocker())
                .map(DockerAttributes::getHttpPort)
                .ifPresent(hostedModel::setHttpPort);

        Optional.ofNullable(dockerHosted.getDocker())
                .map(DockerAttributes::getHttpsPort)
                .ifPresent(hostedModel::setHttpsPort);

        Optional.ofNullable(dockerHosted.getDocker())
                .map(DockerAttributes::getForceBasicAuth)
                .ifPresent(hostedModel::setForceBasicAuth);

        Optional.ofNullable(dockerHosted.getDocker())
                .map(DockerAttributes::getV1Enabled)
                .ifPresent(hostedModel::setV1Enabled);

    }

    private void patchDockerProxy(NexusProxyRepoModel proxyModel, String name) {
        DockerProxyApiRepository dockerProxy = nexusClient.getDockerProxy(name);
        proxyModel.setFormat(NexusRepoFormat.DOCKER);

        Optional.ofNullable(dockerProxy.getName())
                .ifPresent(proxyModel::setName);

        Optional.ofNullable(dockerProxy.getOnline())
                .ifPresent(proxyModel::setOnline);

        Optional.ofNullable(dockerProxy.getStorage())
                .map(StorageAttributes::getBlobStoreName)
                .ifPresent(proxyModel::setBlobStore);

        Optional.ofNullable(dockerProxy.getStorage())
                .map(StorageAttributes::getStrictContentTypeValidation)
                .ifPresent(proxyModel::setStrictContentValidation);

        Optional.ofNullable(dockerProxy.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(proxyModel::setCleanupPolicies);


        Optional.ofNullable(dockerProxy.getProxy())
                .map(ProxyAttributes::getContentMaxAge)
                .ifPresent(proxyModel::setMaxAge);

        Optional.ofNullable(dockerProxy.getProxy())
                .map(ProxyAttributes::getRemoteUrl)
                .ifPresent(proxyModel::setUri);

        Optional.ofNullable(dockerProxy.getProxy())
                .map(ProxyAttributes::getMetadataMaxAge)
                .ifPresent(proxyModel::setMetadataMaxAge);

        Optional.ofNullable(dockerProxy.getNegativeCache())
                .map(NegativeCacheAttributes::getEnabled)
                .ifPresent(proxyModel::setNegativeCache);

        Optional.ofNullable(dockerProxy.getNegativeCache())
                .map(NegativeCacheAttributes::getTimeToLive)
                .ifPresent(proxyModel::setNegativeCacheTTl);

        Optional.ofNullable(dockerProxy.getDocker())
                .map(DockerAttributes::getHttpPort)
                .ifPresent(proxyModel::setHttpPort);

        Optional.ofNullable(dockerProxy.getDocker())
                .map(DockerAttributes::getHttpsPort)
                .ifPresent(proxyModel::setHttpsPort);

        Optional.ofNullable(dockerProxy.getDocker())
                .map(DockerAttributes::getForceBasicAuth)
                .ifPresent(proxyModel::setForceBasicAuth);

        Optional.ofNullable(dockerProxy.getDocker())
                .map(DockerAttributes::getV1Enabled)
                .ifPresent(proxyModel::setV1Enabled);

    }

    private void patchMavenHosted(NexusHostedRepoModel hostedModel, String name) {
        MavenHostedApiRepository mavenHosted = nexusClient.getMavenHosted(name);
        hostedModel.setFormat(NexusRepoFormat.MAVEN);

        Optional.ofNullable(mavenHosted.getName())
                .ifPresent(hostedModel::setName);

        Optional.ofNullable(mavenHosted.getOnline())
                .ifPresent(hostedModel::setOnline);

        Optional.ofNullable(mavenHosted.getStorage())
                .map(HostedStorageAttributes::getBlobStoreName)
                .ifPresent(hostedModel::setBlobStore);

        Optional.ofNullable(mavenHosted.getStorage())
                .map(HostedStorageAttributes::getStrictContentTypeValidation)
                .ifPresent(hostedModel::setStrictContentValidation);

        Optional.ofNullable(mavenHosted.getStorage())
                .map(HostedStorageAttributes::getWritePolicy)
                .map(p -> WritePolicy.valueOf(p.name()))
                .ifPresent(hostedModel::setWritePolicy);

        Optional.ofNullable(mavenHosted.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(hostedModel::setCleanupPolicies);

        Optional.ofNullable(mavenHosted.getMaven())
                .map(MavenAttributes::getVersionPolicy)
                .map(MavenAttributes.VersionPolicyEnum::value)
                .ifPresent(hostedModel::setMavenVersionPolicy);

        Optional.ofNullable(mavenHosted.getMaven())
                .map(MavenAttributes::getLayoutPolicy)
                .map(p -> p == MavenAttributes.LayoutPolicyEnum.STRICT)
                .ifPresent(hostedModel::setMavenLayoutStrict);

        Optional.ofNullable(mavenHosted.getMaven())
                .map(MavenAttributes::getContentDisposition)
                .map(MavenAttributes.ContentDispositionEnum::value)
                .ifPresent(hostedModel::setContentDisposition);
    }

    private void patchMavenProxy(NexusProxyRepoModel proxyModel, String name) {
        MavenProxyApiRepository mavenProxy = nexusClient.getMavenProxy(name);
        proxyModel.setFormat(NexusRepoFormat.MAVEN);

        Optional.ofNullable(mavenProxy.getName())
                .ifPresent(proxyModel::setName);

        Optional.ofNullable(mavenProxy.getOnline())
                .ifPresent(proxyModel::setOnline);

        Optional.ofNullable(mavenProxy.getProxy())
                .map(ProxyAttributes::getContentMaxAge)
                .ifPresent(proxyModel::setMaxAge);

        Optional.ofNullable(mavenProxy.getProxy())
                .map(ProxyAttributes::getRemoteUrl)
                .ifPresent(proxyModel::setUri);

        Optional.ofNullable(mavenProxy.getProxy())
                .map(ProxyAttributes::getMetadataMaxAge)
                .ifPresent(proxyModel::setMetadataMaxAge);

        Optional.ofNullable(mavenProxy.getNegativeCache())
                .map(NegativeCacheAttributes::getEnabled)
                .ifPresent(proxyModel::setNegativeCache);

        Optional.ofNullable(mavenProxy.getNegativeCache())
                .map(NegativeCacheAttributes::getTimeToLive)
                .ifPresent(proxyModel::setNegativeCacheTTl);

        Optional.ofNullable(mavenProxy.getStorage())
                .map(StorageAttributes::getBlobStoreName)
                .ifPresent(proxyModel::setBlobStore);

        Optional.ofNullable(mavenProxy.getStorage())
                .map(StorageAttributes::getStrictContentTypeValidation)
                .ifPresent(proxyModel::setStrictContentValidation);

        Optional.ofNullable(mavenProxy.getCleanup())
                .map(CleanupPolicyAttributes::getPolicyNames)
                .ifPresent(proxyModel::setCleanupPolicies);

        Optional.ofNullable(mavenProxy.getMaven())
                .map(MavenAttributes::getVersionPolicy)
                .map(MavenAttributes.VersionPolicyEnum::value)
                .ifPresent(proxyModel::setMavenVersionPolicy);

        Optional.ofNullable(mavenProxy.getMaven())
                .map(MavenAttributes::getLayoutPolicy)
                .map(p -> p == MavenAttributes.LayoutPolicyEnum.STRICT)
                .ifPresent(proxyModel::setMavenLayoutStrict);

        Optional.ofNullable(mavenProxy.getMaven())
                .map(MavenAttributes::getContentDisposition)
                .map(MavenAttributes.ContentDispositionEnum::value)
                .ifPresent(proxyModel::setContentDisposition);
    }

    private String getFormat(NexusRepoFormat modelFormat, RepositoryXO apiRepository) {
        return Optional.ofNullable(apiRepository.getFormat())
                .orElse(
                        Optional.ofNullable(modelFormat)
                                .map(NexusRepoFormat::getValue)
                                .orElseThrow(() -> new ClientRuntimeError("No repo format for " + apiRepository.getName()))
                );
    }

}
