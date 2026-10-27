package io.github.zhijunio.oauth2client.oauth2;

import io.github.zhijunio.oauth2client.oauth2.strategy.MappedOAuth2User;
import io.github.zhijunio.oauth2client.oauth2.strategy.OAuth2ClientStrategyProvider;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientTemplateEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientDataMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientTemplateMapper;
import io.github.zhijunio.oauth2client.security.SecurityUser;
import io.github.zhijunio.oauth2client.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class ExternalUserResolver {

    private final OAuth2ClientDataMapper clients;
    private final OAuth2ClientTemplateMapper templates;
    private final UserService users;
    private final OAuth2AuthorizedClientService authorizedClients;
    private final OAuth2ClientStrategyProvider strategyProvider;

    public ExternalUserResolver(
            OAuth2ClientDataMapper clients,
            OAuth2ClientTemplateMapper templates,
            UserService users,
            OAuth2AuthorizedClientService authorizedClients,
            OAuth2ClientStrategyProvider strategyProvider) {
        this.clients = clients;
        this.templates = templates;
        this.users = users;
        this.authorizedClients = authorizedClients;
        this.strategyProvider = strategyProvider;
    }

    public SecurityUser resolve(OAuth2AuthenticationToken authentication, HttpServletRequest request) {
        OAuth2ClientEntity client = clients.selectById(authentication.getAuthorizedClientRegistrationId());
        if (client == null || !Boolean.TRUE.equals(client.getEnabled())) {
            throw new IllegalArgumentException("The OAuth2 client is missing or disabled");
        }
        OAuth2ClientTemplateEntity template = templates.selectById(client.getTemplateId());
        if (template == null) {
            throw new IllegalArgumentException("The OAuth2 client template is unavailable");
        }

        String mapperType = client.getType().toUpperCase(java.util.Locale.ROOT);
        OAuth2AuthorizedClient authorizedClient = "CUSTOM".equals(mapperType) || "GITHUB".equals(mapperType)
                ? authorizedClients.loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(), authentication.getName())
                : null;
        MappedOAuth2User mappedUser =
                strategyProvider.getStrategyByType(mapperType).map(request, authentication, authorizedClient, client);
        return users.resolve(
                client.getTenantId(),
                mappedUser.tenantId(),
                mappedUser.tenantName(),
                mappedUser.customerId(),
                mappedUser.email(),
                mappedUser.firstName(),
                mappedUser.lastName(),
                mappedUser.customerName(),
                client.getName(),
                !Boolean.FALSE.equals(client.getAllowUserCreation()),
                !Boolean.FALSE.equals(client.getActivateUser()));
    }
}
