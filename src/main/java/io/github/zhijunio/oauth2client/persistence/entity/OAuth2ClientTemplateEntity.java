package io.github.zhijunio.oauth2client.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("oauth2_client_template")
public class OAuth2ClientTemplateEntity extends BaseAuditEntity {

    @TableId
    private String id;

    private String name;
    private String type;
    private String protocol;
    private String issuerUri;
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String jwkSetUri;
    private String userNameAttribute;
    private String defaultScopes;
    private Boolean allowUserCreation;
    private Boolean activateUser;
    private String basicEmailAttributeKey;
    private String basicFirstNameAttributeKey;
    private String basicLastNameAttributeKey;
    private String basicTenantNameStrategy;
    private String basicTenantNamePattern;
    private String basicCustomerNamePattern;
    private String customUrl;
    private String customUsername;
    private String customPassword;
    private Boolean customSendToken;
    private String clientAuthenticationMethod;
    private String comment;
    private String loginButtonIcon;
    private String loginButtonLabel;
    private String helpLink;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public String getDefaultScopes() {
        return defaultScopes;
    }

    public void setDefaultScopes(String defaultScopes) {
        this.defaultScopes = defaultScopes;
    }

    public Boolean getAllowUserCreation() {
        return allowUserCreation;
    }

    public void setAllowUserCreation(Boolean value) {
        this.allowUserCreation = value;
    }

    public Boolean getActivateUser() {
        return activateUser;
    }

    public void setActivateUser(Boolean value) {
        this.activateUser = value;
    }

    public String getClientAuthenticationMethod() {
        return clientAuthenticationMethod;
    }

    public void setClientAuthenticationMethod(String clientAuthenticationMethod) {
        this.clientAuthenticationMethod = clientAuthenticationMethod;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getLoginButtonIcon() {
        return loginButtonIcon;
    }

    public void setLoginButtonIcon(String loginButtonIcon) {
        this.loginButtonIcon = loginButtonIcon;
    }

    public String getLoginButtonLabel() {
        return loginButtonLabel;
    }

    public void setLoginButtonLabel(String loginButtonLabel) {
        this.loginButtonLabel = loginButtonLabel;
    }

    public String getHelpLink() {
        return helpLink;
    }

    public void setHelpLink(String helpLink) {
        this.helpLink = helpLink;
    }

    public String getBasicEmailAttributeKey() {
        return basicEmailAttributeKey;
    }

    public void setBasicEmailAttributeKey(String value) {
        this.basicEmailAttributeKey = value;
    }

    public String getBasicFirstNameAttributeKey() {
        return basicFirstNameAttributeKey;
    }

    public void setBasicFirstNameAttributeKey(String value) {
        this.basicFirstNameAttributeKey = value;
    }

    public String getBasicLastNameAttributeKey() {
        return basicLastNameAttributeKey;
    }

    public void setBasicLastNameAttributeKey(String value) {
        this.basicLastNameAttributeKey = value;
    }

    public String getBasicTenantNameStrategy() {
        return basicTenantNameStrategy;
    }

    public void setBasicTenantNameStrategy(String value) {
        this.basicTenantNameStrategy = value;
    }

    public String getBasicTenantNamePattern() {
        return basicTenantNamePattern;
    }

    public void setBasicTenantNamePattern(String value) {
        this.basicTenantNamePattern = value;
    }

    public String getBasicCustomerNamePattern() {
        return basicCustomerNamePattern;
    }

    public void setBasicCustomerNamePattern(String value) {
        this.basicCustomerNamePattern = value;
    }

    public String getCustomUrl() {
        return customUrl;
    }

    public void setCustomUrl(String value) {
        this.customUrl = value;
    }

    public String getCustomUsername() {
        return customUsername;
    }

    public void setCustomUsername(String value) {
        this.customUsername = value;
    }

    public String getCustomPassword() {
        return customPassword;
    }

    public void setCustomPassword(String value) {
        this.customPassword = value;
    }

    public Boolean getCustomSendToken() {
        return customSendToken;
    }

    public void setCustomSendToken(Boolean value) {
        this.customSendToken = value;
    }
}
