package io.github.zhijunio.oauth2client.oauth2;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientDataMapper;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

@Component
public class DatabaseClientRegistrationRepository implements ClientRegistrationRepository {

    private final OAuth2ClientDataMapper clients;

    public DatabaseClientRegistrationRepository(OAuth2ClientDataMapper clients) {
        this.clients = clients;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        OAuth2ClientEntity client = clients.selectById(registrationId);
        if (client == null || !Boolean.TRUE.equals(client.getEnabled())) {
            return null;
        }
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(client.getId())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethod(
                        "POST".equals(client.getClientAuthenticationMethod())
                                ? ClientAuthenticationMethod.CLIENT_SECRET_POST
                                : ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(Arrays.stream(client.getScopes().split(","))
                        .map(String::trim)
                        .filter(scope -> !scope.isEmpty())
                        .collect(Collectors.toSet()))
                .authorizationUri(client.getAuthorizationUri())
                .tokenUri(client.getTokenUri())
                .userInfoUri(client.getUserInfoUri())
                .userNameAttributeName(client.getUserNameAttribute())
                .clientName(client.getName());
        if (client.getIssuerUri() != null && !client.getIssuerUri().isBlank()) {
            builder.issuerUri(client.getIssuerUri());
        }
        if (client.getJwkSetUri() != null && !client.getJwkSetUri().isBlank()) {
            builder.jwkSetUri(client.getJwkSetUri());
        }
        return builder.build();
    }
}
