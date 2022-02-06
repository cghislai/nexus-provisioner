package com.charlyghislain.nexus.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NexusLdapServerModel {

    private String name;
    private Boolean ldaps;
    private Boolean useTrustStore = true;
    private String host;
    private Integer port;

    private String searchBaseDn;
    private String authScheme = "SIMPLE";
    private String authUsername;
    private NexusSecretValueModel authPassword;

    private Integer connectionTimeoutSeconds = 30;
    private Integer connectionRetrySeconds = 300;
    private Integer maxIncident = 3;

    private String userBaseDn;
    private Boolean userSubtree;
    private String userObjectClass;
    private String userFilter;
    private String userIdAttr;
    private String userRealmNameAtr;
    private String userEmailAttr;
    private String userPasswordAttr;

    private Boolean groupAsRoles;
    private String groupType = "DYNAMIC";
    private String groupBaseDn;
    private Boolean groupSubtree;
    private String groupObjectClass;
    private String groupIdAttr;
    private String groupMemberAttr;
    private String groupMemberFormat;
    private String userMemberOfAttr;

    public void parseJsonTree(JsonNode jsonNode) {
        name = jsonNode.get("name").asText();
        ldaps = jsonNode.get("protocol").asText("LDAPS").equals("LDAPS");
        useTrustStore = jsonNode.get("useTrustStore").asBoolean();
        host = jsonNode.get("host").asText();
        port = jsonNode.get("port").asInt();

        searchBaseDn = jsonNode.get("searchBase").asText();
        authScheme = jsonNode.get("authScheme").asText();
        //authRealm
        authUsername = jsonNode.get("authUsername").asText();
        connectionTimeoutSeconds = jsonNode.get("connectionTimeoutSeconds").asInt();
        connectionRetrySeconds = jsonNode.get("connectionRetryDelaySeconds").asInt();
        maxIncident = jsonNode.get("maxIncidentsCount").asInt();
        userBaseDn = jsonNode.get("userBaseDn").asText();
        userSubtree = jsonNode.get("userSubtree").asBoolean();
        userObjectClass = jsonNode.get("userObjectClass").asText();
        userFilter = jsonNode.get("userLdapFilter").asText();
        userIdAttr = jsonNode.get("userIdAttribute").asText();
        userRealmNameAtr = jsonNode.get("userRealNameAttribute").asText();
        userEmailAttr = jsonNode.get("userEmailAddressAttribute").asText();
        userPasswordAttr = jsonNode.get("userPasswordAttribute").asText();

        groupAsRoles = jsonNode.get("ldapGroupsAsRoles").asBoolean();
        groupType = jsonNode.get("groupType").asText();
        groupBaseDn = jsonNode.get("groupBaseDn").asText();
        groupSubtree = jsonNode.get("groupSubtree").asBoolean();
        groupObjectClass = jsonNode.get("groupObjectClass").asText();
        groupIdAttr = jsonNode.get("groupIdAttribute").asText();
        groupMemberAttr = jsonNode.get("groupMemberAttribute").asText();
        groupMemberFormat = jsonNode.get("groupMemberFormat").asText();
        userMemberOfAttr = jsonNode.get("userMemberOfAttribute").asText();
    }

    public static NexusLdapServerModel fromJsonTree(JsonNode jsonNode) {
        NexusLdapServerModel serverModel = new NexusLdapServerModel();
        serverModel.parseJsonTree(jsonNode);
        return serverModel;
    }
}
