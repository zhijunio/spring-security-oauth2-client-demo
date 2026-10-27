package io.github.zhijunio.oauth2client.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientDataMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@ExtendWith(MockitoExtension.class)
class DatabaseClientRegistrationRepositoryTest {

    @Mock
    private OAuth2ClientDataMapper clients;

    @Test
    void returnsNullWhenClientDoesNotExistOrIsDisabled() {
        DatabaseClientRegistrationRepository repository = new DatabaseClientRegistrationRepository(clients);
        when(clients.selectById("missing")).thenReturn(null);
        when(clients.selectById("disabled")).thenReturn(client("disabled", false));

        assertThat(repository.findByRegistrationId("missing")).isNull();
        assertThat(repository.findByRegistrationId("disabled")).isNull();
        verify(clients).selectById("missing");
        verify(clients).selectById("disabled");
    }

    @Test
    void buildsEnabledRegistrationFromDatabaseFields() {
        OAuth2ClientEntity client = client("github", true);
        client.setName("GitHub Login");
        client.setClientId("client-id");
        client.setClientSecret("client-secret");
        client.setScopes("openid, email, , profile ");
        client.setAuthorizationUri("https://idp.example/authorize");
        client.setTokenUri("https://idp.example/token");
        client.setUserInfoUri("https://idp.example/userinfo");
        client.setUserNameAttribute("email");
        client.setClientAuthenticationMethod("POST");
        client.setIssuerUri("https://idp.example");
        client.setJwkSetUri("https://idp.example/keys");
        when(clients.selectById("github")).thenReturn(client);

        ClientRegistration registration =
                new DatabaseClientRegistrationRepository(clients).findByRegistrationId("github");

        assertThat(registration.getRegistrationId()).isEqualTo("github");
        assertThat(registration.getClientId()).isEqualTo("client-id");
        assertThat(registration.getClientSecret()).isEqualTo("client-secret");
        assertThat(registration.getClientAuthenticationMethod())
                .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        assertThat(registration.getRedirectUri()).isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
        assertThat(registration.getScopes()).containsExactlyInAnyOrder("openid", "email", "profile");
        assertThat(registration.getProviderDetails().getAuthorizationUri()).isEqualTo("https://idp.example/authorize");
        assertThat(registration.getProviderDetails().getTokenUri()).isEqualTo("https://idp.example/token");
        assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUri())
                .isEqualTo("https://idp.example/userinfo");
        assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                .isEqualTo("email");
        assertThat(registration.getProviderDetails().getIssuerUri()).isEqualTo("https://idp.example");
        assertThat(registration.getProviderDetails().getJwkSetUri()).isEqualTo("https://idp.example/keys");
    }

    @Test
    void defaultsClientAuthenticationMethodToBasic() {
        OAuth2ClientEntity client = client("custom", true);
        client.setClientAuthenticationMethod("UNKNOWN");
        when(clients.selectById("custom")).thenReturn(client);

        ClientRegistration registration =
                new DatabaseClientRegistrationRepository(clients).findByRegistrationId("custom");

        assertThat(registration.getClientAuthenticationMethod())
                .isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        assertThat(registration.getProviderDetails().getIssuerUri()).isNull();
        assertThat(registration.getProviderDetails().getJwkSetUri()).isNull();
    }

    private static OAuth2ClientEntity client(String id, boolean enabled) {
        OAuth2ClientEntity client = new OAuth2ClientEntity();
        client.setId(id);
        client.setEnabled(enabled);
        client.setName(id);
        client.setClientId("id");
        client.setClientSecret("secret");
        client.setScopes("openid");
        client.setAuthorizationUri("https://idp.example/authorize");
        client.setTokenUri("https://idp.example/token");
        client.setUserInfoUri("https://idp.example/userinfo");
        client.setUserNameAttribute("sub");
        return client;
    }
}
