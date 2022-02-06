package com.charlyghislain.nexus.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LdapServer {

    private String name;
    private String protocol;
    private Boolean useTrustStore = true;
    private String host;
    private Integer port;

    private String searchBase;
    private String authScheme = "SIMPLE";
    private String authUsername;
    private String authRealm;
    private String authPassword;

    private Integer connectionTimeoutSeconds = 30;
    private Integer connectionRetryDelaySeconds = 300;
    private Integer maxIncidentsCount = 3;

    private String userBaseDn;
    private Boolean userSubtree;
    private String userObjectClass;
    private String userLdapFilter;
    private String userIdAttribute;
    private String userRealNameAttribute;
    private String userEmailAddressAttribute;
    private String userPasswordAttribute;

    private Boolean ldapGroupsAsRoles;
    private String groupType = "DYNAMIC";
    private String groupBaseDn;
    private Boolean groupSubtree;
    private String groupObjectClass;
    private String groupIdAttribute;
    private String groupMemberAttribute;
    private String groupMemberFormat;
    private String userMemberOfAttribute;

}
