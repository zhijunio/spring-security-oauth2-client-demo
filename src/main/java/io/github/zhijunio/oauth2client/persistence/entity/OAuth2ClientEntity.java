package io.github.zhijunio.oauth2client.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("oauth2_client")
public class OAuth2ClientEntity extends BaseAuditEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String tenantId;
    private Boolean allowUserCreation;
    private Boolean activateUser;
    private String type;
    private String templateId;
    private String clientId;
    private String clientSecret;
    private String scopes;
    private String issuerUri;
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String jwkSetUri;
    private String userNameAttribute;
    private String clientAuthenticationMethod;
    private String loginButtonLabel;
    private String loginButtonIcon;
    private Boolean enabled;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
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

    public String getClientAuthenticationMethod() {
        return clientAuthenticationMethod;
    }

    public void setClientAuthenticationMethod(String clientAuthenticationMethod) {
        this.clientAuthenticationMethod = clientAuthenticationMethod;
    }

    public String getLoginButtonLabel() {
        return loginButtonLabel;
    }

    public void setLoginButtonLabel(String loginButtonLabel) {
        this.loginButtonLabel = loginButtonLabel;
    }

    public String getLoginButtonIcon() {
        return loginButtonIcon;
    }

    public void setLoginButtonIcon(String loginButtonIcon) {
        this.loginButtonIcon = loginButtonIcon;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
