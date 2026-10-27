package io.github.zhijunio.oauth2client.oauth2;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.github.zhijunio.oauth2client.api.PageData;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientTemplateEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientDataMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientTemplateMapper;
import io.github.zhijunio.oauth2client.service.TenantService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OAuth2ClientService {

    private final OAuth2ClientDataMapper clients;
    private final OAuth2ClientTemplateMapper templates;
    private final DomainService domains;
    private final TenantService tenants;

    public OAuth2ClientService(
            OAuth2ClientDataMapper clients,
            OAuth2ClientTemplateMapper templates,
            DomainService domains,
            TenantService tenants) {
        this.clients = clients;
        this.templates = templates;
        this.domains = domains;
        this.tenants = tenants;
    }

    public List<OAuth2LoginClient> enabledClientsForDomain(String domainName) {
        List<String> clientIds = domains.enabledClientIdsForDomain(domainName);
        if (clientIds.isEmpty()) return List.of();
        return clients.selectBatchIds(clientIds).stream()
                .filter(client -> Boolean.TRUE.equals(client.getEnabled()))
                .sorted(Comparator.comparing(OAuth2ClientEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::loginClient)
                .toList();
    }

    private OAuth2LoginClient loginClient(OAuth2ClientEntity client) {
        return new OAuth2LoginClient(
                client.getId(),
                client.getLoginButtonLabel() == null
                                || client.getLoginButtonLabel().isBlank()
                        ? client.getName()
                        : client.getLoginButtonLabel(),
                client.getLoginButtonIcon());
    }

    public List<OAuth2ClientInfo> allClients() {
        return clientInfos(clients.selectList(
                Wrappers.<OAuth2ClientEntity>lambdaQuery().orderByDesc(OAuth2ClientEntity::getCreatedAt)));
    }

    public PageData<OAuth2ClientInfo> clientPage(
            int pageSize, int page, String textSearch, String sortProperty, String sortOrder) {
        List<OAuth2ClientInfo> all = allClients();
        if (textSearch != null && !textSearch.isBlank()) {
            String needle = textSearch.toLowerCase(Locale.ROOT);
            all = all.stream()
                    .filter(client -> client.name().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }
        if (sortProperty != null && !sortProperty.isBlank()) {
            Comparator<OAuth2ClientInfo> comparator = switch (sortProperty) {
                case "name" -> Comparator.comparing(OAuth2ClientInfo::name, String.CASE_INSENSITIVE_ORDER);
                case "createdTime", "createdAt" -> Comparator.comparing(OAuth2ClientInfo::createdTime);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sortProperty");
            };
            all = all.stream()
                    .sorted("DESC".equalsIgnoreCase(sortOrder) ? comparator.reversed() : comparator)
                    .toList();
        }
        return PageData.from(all, pageSize, page);
    }

    public List<OAuth2ClientInfo> clientsByIds(List<String> ids) {
        return clientInfos(clients.selectBatchIds(ids));
    }

    public List<OAuth2ClientTemplateInfo> templates() {
        return templates
                .selectList(Wrappers.<OAuth2ClientTemplateEntity>lambdaQuery()
                        .orderByAsc(OAuth2ClientTemplateEntity::getName))
                .stream()
                .map(this::templateInfo)
                .toList();
    }

    @Transactional
    public OAuth2ClientTemplateInfo saveTemplate(OAuth2ClientTemplateRequest input) {
        String id =
                input.id() == null || input.id().isBlank() ? UUID.randomUUID().toString() : input.id();
        OAuth2ClientTemplateEntity template = templates.selectById(id);
        boolean create = template == null;
        if (create) {
            template = new OAuth2ClientTemplateEntity();
            template.setId(id);
        }
        OAuth2MapperConfigRequest mapperConfig = input.mapperConfig();
        String mapperType = mapperConfig == null
                        || mapperConfig.type() == null
                        || mapperConfig.type().isBlank()
                ? null
                : mapperConfig.type().toUpperCase(Locale.ROOT);
        if (mapperType != null && !List.of("BASIC", "CUSTOM", "GITHUB", "APPLE").contains(mapperType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported mapperConfig.type");
        }
        mapperType = mapperType == null ? "BASIC" : mapperType;
        String protocol = input.protocol() == null || input.protocol().isBlank()
                ? (input.scope() != null && input.scope().contains("openid") ? "OIDC" : "OAUTH2")
                : input.protocol();
        if (!List.of("OIDC", "OAUTH2").contains(protocol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "protocol must be OIDC or OAUTH2");
        }
        template.setName(input.name().trim());
        template.setProtocol(protocol);
        template.setIssuerUri(input.issuerUri());
        template.setAuthorizationUri(input.authorizationUri());
        template.setTokenUri(input.accessTokenUri());
        template.setUserInfoUri(input.userInfoUri());
        template.setJwkSetUri(input.jwkSetUri());
        template.setUserNameAttribute(input.userNameAttributeName() == null ? "sub" : input.userNameAttributeName());
        template.setDefaultScopes(input.scope() == null ? "" : String.join(",", input.scope()));
        template.setAllowUserCreation(mapperConfig == null || !Boolean.FALSE.equals(mapperConfig.allowUserCreation()));
        template.setActivateUser(mapperConfig == null || !Boolean.FALSE.equals(mapperConfig.activateUser()));
        template.setClientAuthenticationMethod(authenticationMethod(input.clientAuthenticationMethod()));
        template.setComment(input.comment());
        template.setLoginButtonIcon(input.loginButtonIcon());
        template.setLoginButtonLabel(input.loginButtonLabel() == null ? input.name() : input.loginButtonLabel());
        template.setHelpLink(input.helpLink());
        template.setType(mapperType);
        BasicMapperConfigRequest basic = mapperConfig == null ? null : mapperConfig.basic();
        CustomMapperConfigRequest custom = mapperConfig == null ? null : mapperConfig.custom();
        if ("CUSTOM".equals(mapperType)) {
            if (custom == null || custom.url() == null || !custom.url().startsWith("https://")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CUSTOM mapperConfig requires an HTTPS URL");
            }
            template.setBasicEmailAttributeKey(null);
            template.setBasicFirstNameAttributeKey(null);
            template.setBasicLastNameAttributeKey(null);
            template.setBasicTenantNameStrategy(null);
            template.setBasicTenantNamePattern(null);
            template.setBasicCustomerNamePattern(null);
            template.setCustomUrl(custom.url());
            template.setCustomUsername(custom.username());
            if (custom.password() != null && !custom.password().isBlank()) {
                template.setCustomPassword(custom.password());
            }
            template.setCustomSendToken(Boolean.TRUE.equals(custom.sendToken()));
        } else {
            template.setCustomUrl(null);
            template.setCustomUsername(null);
            template.setCustomPassword(null);
            template.setCustomSendToken(false);
            template.setBasicEmailAttributeKey(basic == null ? null : basic.emailAttributeKey());
            template.setBasicFirstNameAttributeKey(basic == null ? null : basic.firstNameAttributeKey());
            template.setBasicLastNameAttributeKey(basic == null ? null : basic.lastNameAttributeKey());
            template.setBasicTenantNameStrategy(basic == null ? null : basic.tenantNameStrategy());
            template.setBasicTenantNamePattern(basic == null ? null : basic.tenantNamePattern());
            template.setBasicCustomerNamePattern(basic == null ? null : basic.customerNamePattern());
        }
        if (create) templates.insert(template);
        else templates.updateById(template);
        return templateInfo(template);
    }

    @Transactional
    public void deleteTemplate(String id) {
        if (clients.selectCount(Wrappers.<OAuth2ClientEntity>lambdaQuery().eq(OAuth2ClientEntity::getTemplateId, id))
                > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Template is referenced by an OAuth2 client");
        }
        if (templates.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OAuth2 client template not found");
        }
    }

    private OAuth2ClientTemplateInfo templateInfo(OAuth2ClientTemplateEntity template) {
        return new OAuth2ClientTemplateInfo(
                template.getId(),
                template.getCreatedAt() == null ? null : template.getCreatedAt().toEpochMilli(),
                template.getUpdatedAt() == null ? null : template.getUpdatedAt().toEpochMilli(),
                template.getName(),
                template.getProtocol(),
                template.getIssuerUri(),
                template.getAuthorizationUri(),
                template.getTokenUri(),
                scopes(template.getDefaultScopes()),
                template.getUserInfoUri(),
                template.getUserNameAttribute(),
                template.getJwkSetUri(),
                template.getClientAuthenticationMethod(),
                template.getComment(),
                template.getLoginButtonIcon(),
                template.getLoginButtonLabel(),
                template.getHelpLink(),
                new MapperConfigInfo(
                        template.getType(),
                        Boolean.TRUE.equals(template.getAllowUserCreation()),
                        Boolean.TRUE.equals(template.getActivateUser()),
                        "CUSTOM".equals(template.getType())
                                ? null
                                : new BasicMapperConfigInfo(
                                        template.getBasicEmailAttributeKey(),
                                        template.getBasicFirstNameAttributeKey(),
                                        template.getBasicLastNameAttributeKey(),
                                        template.getBasicTenantNameStrategy(),
                                        template.getBasicTenantNamePattern(),
                                        template.getBasicCustomerNamePattern()),
                        "CUSTOM".equals(template.getType())
                                ? new CustomMapperConfigInfo(
                                        template.getCustomUrl(),
                                        template.getCustomUsername(),
                                        "",
                                        Boolean.TRUE.equals(template.getCustomSendToken()))
                                : null));
    }

    private List<String> scopes(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.split(","));
    }

    private String authenticationMethod(String value) {
        String method = value == null || value.isBlank() ? "BASIC" : value;
        if (!List.of("BASIC", "POST").contains(method)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "clientAuthenticationMethod must be BASIC or POST");
        }
        return method;
    }

    private static String valueOr(String value, String fallback) {
        return value == null ? fallback : value;
    }

    @Transactional
    public OAuth2ClientInfo create(OAuth2ClientRequest input) {
        String registrationId = null;
        if (input.id() != null && !input.id().isBlank()) {
            try {
                registrationId = UUID.fromString(input.id()).toString();
                if (clients.selectById(registrationId) != null) {
                    return update(registrationId, input);
                }
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be a UUID");
            }
        }
        OAuth2ClientTemplateEntity template = requireTemplate(input.templateId());
        String mapperType = requireClientMapperType(template, input.mapperConfig());
        if (input.clientSecret() == null || input.clientSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client secret is required");
        }
        OAuth2ClientEntity client = new OAuth2ClientEntity();
        if (registrationId == null) registrationId = UUID.randomUUID().toString();
        if (clients.selectById(registrationId) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "OAuth2 registration ID already exists");
        }
        client.setId(registrationId);
        apply(client, input, template, mapperType);
        clients.insert(client);
        return info(client);
    }

    @Transactional
    public OAuth2ClientInfo update(String id, OAuth2ClientRequest input) {
        OAuth2ClientEntity client = requireClient(id);
        OAuth2ClientTemplateEntity template = requireTemplate(input.templateId());
        String mapperType = requireClientMapperType(template, input.mapperConfig());
        apply(client, input, template, mapperType);
        clients.updateById(client);
        return info(client);
    }

    @Transactional
    public void delete(String id) {
        if (clients.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OAuth2 client not found");
        }
    }

    public OAuth2ClientEntity requireClient(String id) {
        OAuth2ClientEntity client = clients.selectById(id);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OAuth2 client not found");
        }
        return client;
    }

    public OAuth2ClientDetail detail(String id) {
        OAuth2ClientEntity client = requireClient(id);
        OAuth2ClientTemplateEntity template = templates.selectById(client.getTemplateId());
        return new OAuth2ClientDetail(
                client.getId(),
                client.getCreatedAt() == null ? 0 : client.getCreatedAt().toEpochMilli(),
                client.getUpdatedAt() == null ? 0 : client.getUpdatedAt().toEpochMilli(),
                client.getName(),
                "",
                client.getClientId(),
                client.getTemplateId(),
                template == null ? null : template.getProtocol(),
                client.getIssuerUri(),
                client.getAuthorizationUri(),
                client.getTokenUri(),
                scopes(client.getScopes()),
                client.getUserInfoUri(),
                client.getUserNameAttribute(),
                client.getJwkSetUri(),
                client.getClientAuthenticationMethod(),
                client.getLoginButtonLabel(),
                client.getLoginButtonIcon(),
                client.getTenantId(),
                Boolean.TRUE.equals(client.getEnabled()),
                mapperConfig(client));
    }

    public OAuth2ClientTemplateEntity requireTemplate(String id) {
        OAuth2ClientTemplateEntity template = templates.selectById(id);
        if (template == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown OAuth2 client template");
        }
        return template;
    }

    private String requireClientMapperType(OAuth2ClientTemplateEntity template, OAuth2MapperConfigRequest config) {
        String mapperType =
                config == null || config.type() == null || config.type().isBlank()
                        ? template.getType()
                        : config.type().toUpperCase(Locale.ROOT);
        if (mapperType == null || !List.of("BASIC", "CUSTOM", "GITHUB", "APPLE").contains(mapperType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported mapperConfig.type");
        }
        if (!mapperType.equalsIgnoreCase(template.getType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Client mapper type must match the selected template mapperConfig.type");
        }
        return mapperType;
    }

    private void apply(
            OAuth2ClientEntity client,
            OAuth2ClientRequest input,
            OAuth2ClientTemplateEntity template,
            String mapperType) {
        client.setName(input.name().trim());
        String tenantId = input.tenantId().trim();
        tenants.require(tenantId);
        client.setTenantId(tenantId);
        OAuth2MapperConfigRequest mapperConfig = input.mapperConfig();
        client.setAllowUserCreation(
                mapperConfig == null || mapperConfig.allowUserCreation() == null
                        ? Boolean.TRUE.equals(template.getAllowUserCreation())
                        : mapperConfig.allowUserCreation());
        client.setActivateUser(
                mapperConfig == null || mapperConfig.activateUser() == null
                        ? Boolean.TRUE.equals(template.getActivateUser())
                        : mapperConfig.activateUser());
        client.setType(mapperType);
        client.setTemplateId(template.getId());
        client.setClientId(input.clientId().trim());
        if (input.clientSecret() != null && !input.clientSecret().isBlank()) {
            client.setClientSecret(input.clientSecret());
        }
        BasicMapperConfigRequest basic = mapperConfig == null ? null : mapperConfig.basic();
        CustomMapperConfigRequest custom = mapperConfig == null ? null : mapperConfig.custom();
        client.setBasicEmailAttributeKey(
                valueOr(basic == null ? null : basic.emailAttributeKey(), template.getBasicEmailAttributeKey()));
        client.setBasicFirstNameAttributeKey(valueOr(
                basic == null ? null : basic.firstNameAttributeKey(), template.getBasicFirstNameAttributeKey()));
        client.setBasicLastNameAttributeKey(
                valueOr(basic == null ? null : basic.lastNameAttributeKey(), template.getBasicLastNameAttributeKey()));
        client.setBasicTenantNameStrategy(
                valueOr(basic == null ? null : basic.tenantNameStrategy(), template.getBasicTenantNameStrategy()));
        client.setBasicTenantNamePattern(
                valueOr(basic == null ? null : basic.tenantNamePattern(), template.getBasicTenantNamePattern()));
        validateTenantMapping(client, mapperType);
        client.setBasicCustomerNamePattern(
                valueOr(basic == null ? null : basic.customerNamePattern(), template.getBasicCustomerNamePattern()));
        if ("CUSTOM".equalsIgnoreCase(client.getType())) {
            client.setCustomUrl(valueOr(custom == null ? null : custom.url(), template.getCustomUrl()));
            client.setCustomUsername(valueOr(custom == null ? null : custom.username(), template.getCustomUsername()));
            if (client.getCustomUrl() == null || !client.getCustomUrl().startsWith("https://")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom mapper requires an HTTPS URL");
            }
            if (custom != null
                    && custom.password() != null
                    && !custom.password().isBlank()) {
                client.setCustomPassword(custom.password());
            } else if (client.getCustomPassword() == null) {
                client.setCustomPassword(template.getCustomPassword());
            }
            client.setCustomSendToken(
                    custom == null || custom.sendToken() == null
                            ? Boolean.TRUE.equals(template.getCustomSendToken())
                            : custom.sendToken());
        } else {
            client.setCustomUrl(null);
            client.setCustomUsername(null);
            client.setCustomPassword(null);
            client.setCustomSendToken(false);
        }
        client.setScopes(
                input.scope() == null || input.scope().isEmpty()
                        ? template.getDefaultScopes()
                        : String.join(
                                ",",
                                input.scope().stream()
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .distinct()
                                        .toList()));
        client.setEnabled(input.enabled() == null || input.enabled());
        client.setIssuerUri(template.getIssuerUri());
        client.setAuthorizationUri(template.getAuthorizationUri());
        client.setTokenUri(template.getTokenUri());
        client.setUserInfoUri(template.getUserInfoUri());
        client.setJwkSetUri(template.getJwkSetUri());
        client.setUserNameAttribute(template.getUserNameAttribute());
        client.setClientAuthenticationMethod(template.getClientAuthenticationMethod());
        client.setLoginButtonLabel(template.getLoginButtonLabel());
        client.setLoginButtonIcon(template.getLoginButtonIcon());
        if (input.authorizationUri() != null) client.setAuthorizationUri(input.authorizationUri());
        if (input.accessTokenUri() != null) client.setTokenUri(input.accessTokenUri());
        if (input.userInfoUri() != null) client.setUserInfoUri(input.userInfoUri());
        if (input.jwkSetUri() != null) client.setJwkSetUri(input.jwkSetUri());
        if (input.userNameAttributeName() != null) client.setUserNameAttribute(input.userNameAttributeName());
        if (input.clientAuthenticationMethod() != null) {
            client.setClientAuthenticationMethod(authenticationMethod(input.clientAuthenticationMethod()));
        }
        if (input.loginButtonLabel() != null) client.setLoginButtonLabel(input.loginButtonLabel());
        if (input.loginButtonIcon() != null) client.setLoginButtonIcon(input.loginButtonIcon());
    }

    private static void validateTenantMapping(OAuth2ClientEntity client, String mapperType) {
        if (!TenantService.isSystemTenant(client.getTenantId()) || "CUSTOM".equalsIgnoreCase(mapperType)) return;
        String strategy = client.getBasicTenantNameStrategy();
        if (!List.of("EMAIL", "DOMAIN", "CUSTOM").contains(strategy == null ? "" : strategy.toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "System-tenant clients require an EMAIL, DOMAIN, or CUSTOM tenant mapping");
        }
        if ("CUSTOM".equalsIgnoreCase(strategy)
                && (client.getBasicTenantNamePattern() == null
                        || client.getBasicTenantNamePattern().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "CUSTOM tenant mapping requires a tenant name pattern");
        }
    }

    private OAuth2ClientInfo info(OAuth2ClientEntity client) {
        return info(client, templates.selectById(client.getTemplateId()));
    }

    private List<OAuth2ClientInfo> clientInfos(List<OAuth2ClientEntity> clients) {
        if (clients.isEmpty()) return List.of();
        Map<String, OAuth2ClientTemplateEntity> templatesById =
                templates
                        .selectBatchIds(clients.stream()
                                .map(OAuth2ClientEntity::getTemplateId)
                                .distinct()
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(OAuth2ClientTemplateEntity::getId, template -> template));
        return clients.stream()
                .map(client -> info(client, templatesById.get(client.getTemplateId())))
                .toList();
    }

    private OAuth2ClientInfo info(OAuth2ClientEntity client, OAuth2ClientTemplateEntity template) {
        return new OAuth2ClientInfo(
                client.getId(),
                client.getCreatedAt() == null ? null : client.getCreatedAt().toEpochMilli(),
                client.getName(),
                template == null ? null : template.getName());
    }

    private MapperConfigInfo mapperConfig(OAuth2ClientEntity client) {
        boolean custom = "CUSTOM".equalsIgnoreCase(client.getType());
        return new MapperConfigInfo(
                client.getType(),
                Boolean.TRUE.equals(client.getAllowUserCreation()),
                Boolean.TRUE.equals(client.getActivateUser()),
                custom
                        ? null
                        : new BasicMapperConfigInfo(
                                client.getBasicEmailAttributeKey(),
                                client.getBasicFirstNameAttributeKey(),
                                client.getBasicLastNameAttributeKey(),
                                client.getBasicTenantNameStrategy(),
                                client.getBasicTenantNamePattern(),
                                client.getBasicCustomerNamePattern()),
                custom
                        ? new CustomMapperConfigInfo(
                                client.getCustomUrl(),
                                client.getCustomUsername(),
                                "",
                                Boolean.TRUE.equals(client.getCustomSendToken()))
                        : null);
    }

    public record OAuth2ClientRequest(
            @JsonAlias("registrationId") String id,

            @jakarta.validation.constraints.NotBlank String name,

            @jakarta.validation.constraints.NotBlank String templateId,
            @jakarta.validation.constraints.NotBlank String clientId,
            @jakarta.validation.constraints.Size(max = 2048) String clientSecret,

            @JsonAlias("scopes")
            List<@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String> scope,

            String authorizationUri,
            String accessTokenUri,
            String userInfoUri,
            String userNameAttributeName,
            String jwkSetUri,
            String clientAuthenticationMethod,
            String loginButtonLabel,
            String loginButtonIcon,

            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 36)
            String tenantId,

            Boolean enabled,
            String issuerUri,
            OAuth2MapperConfigRequest mapperConfig) {}

    public record OAuth2ClientTemplateRequest(
            String id,
            @jakarta.validation.constraints.NotBlank String name,
            String protocol,
            String issuerUri,
            String authorizationUri,
            String accessTokenUri,
            List<@jakarta.validation.constraints.NotBlank String> scope,
            String userInfoUri,
            String userNameAttributeName,
            String jwkSetUri,
            String clientAuthenticationMethod,
            String comment,
            String loginButtonIcon,
            String loginButtonLabel,
            String helpLink,
            OAuth2MapperConfigRequest mapperConfig) {}

    public record OAuth2MapperConfigRequest(
            String type,
            Boolean allowUserCreation,
            Boolean activateUser,
            BasicMapperConfigRequest basic,
            CustomMapperConfigRequest custom) {}

    public record BasicMapperConfigRequest(
            String emailAttributeKey,
            String firstNameAttributeKey,
            String lastNameAttributeKey,
            String tenantNameStrategy,
            String tenantNamePattern,
            String customerNamePattern) {}

    public record CustomMapperConfigRequest(String url, String username, String password, Boolean sendToken) {}

    public record OAuth2ClientInfo(String id, Long createdTime, String name, String providerName) {}

    public record OAuth2LoginClient(String id, String loginButtonLabel, String loginButtonIcon) {}

    public record OAuth2ClientDetail(
            String id,
            long createdTime,
            long updatedTime,
            String name,
            String clientSecret,
            String clientId,
            String templateId,
            String protocol,
            String issuerUri,
            String authorizationUri,
            String accessTokenUri,
            List<String> scope,
            String userInfoUri,
            String userNameAttributeName,
            String jwkSetUri,
            String clientAuthenticationMethod,
            String loginButtonLabel,
            String loginButtonIcon,
            String tenantId,
            boolean enabled,
            MapperConfigInfo mapperConfig) {}

    public record OAuth2ClientTemplateInfo(
            String id,
            Long createdTime,
            Long updatedTime,
            String name,
            String protocol,
            String issuerUri,
            String authorizationUri,
            String accessTokenUri,
            List<String> scope,
            String userInfoUri,
            String userNameAttributeName,
            String jwkSetUri,
            String clientAuthenticationMethod,
            String comment,
            String loginButtonIcon,
            String loginButtonLabel,
            String helpLink,
            MapperConfigInfo mapperConfig) {}

    public record MapperConfigInfo(
            String type,
            boolean allowUserCreation,
            boolean activateUser,
            BasicMapperConfigInfo basic,
            CustomMapperConfigInfo custom) {}

    public record BasicMapperConfigInfo(
            String emailAttributeKey,
            String firstNameAttributeKey,
            String lastNameAttributeKey,
            String tenantNameStrategy,
            String tenantNamePattern,
            String customerNamePattern) {}

    public record CustomMapperConfigInfo(String url, String username, String password, boolean sendToken) {}
}
