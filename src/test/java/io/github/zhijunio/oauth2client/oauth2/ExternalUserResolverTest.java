package io.github.zhijunio.oauth2client.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.zhijunio.oauth2client.oauth2.strategy.MappedOAuth2User;
import io.github.zhijunio.oauth2client.oauth2.strategy.OAuth2ClientStrategy;
import io.github.zhijunio.oauth2client.oauth2.strategy.OAuth2ClientStrategyProvider;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientTemplateEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientDataMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientTemplateMapper;
import io.github.zhijunio.oauth2client.security.SecurityUser;
import io.github.zhijunio.oauth2client.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ExternalUserResolverTest {

    @Mock
    private OAuth2ClientDataMapper clients;

    @Mock
    private OAuth2ClientTemplateMapper templates;

    @Mock
    private UserService users;

    @Mock
    private OAuth2AuthorizedClientService authorizedClients;

    @Mock
    private OAuth2ClientStrategyProvider strategyProvider;

    @Mock
    private OAuth2ClientStrategy strategy;

    @Mock
    private OAuth2AuthenticationToken authentication;

    @Mock
    private HttpServletRequest request;

    private ExternalUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ExternalUserResolver(clients, templates, users, authorizedClients, strategyProvider);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("client-1");
    }

    @Test
    void rejectsMissingOrDisabledClientBeforeMappingIdentity() {
        when(clients.selectById("client-1")).thenReturn(null).thenReturn(client(false));

        assertThatThrownBy(() -> resolver.resolve(authentication, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The OAuth2 client is missing or disabled");
        assertThatThrownBy(() -> resolver.resolve(authentication, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The OAuth2 client is missing or disabled");

        verifyNoIdentityResolution();
    }

    @Test
    void rejectsUnavailableTemplateBeforeMappingIdentity() {
        OAuth2ClientEntity client = client(true);
        when(clients.selectById("client-1")).thenReturn(client);
        when(templates.selectById("template-1")).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(authentication, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The OAuth2 client template is unavailable");

        verifyNoIdentityResolution();
    }

    @Test
    void mapsIdentityAndPassesClientCreationPolicyToUserService() {
        OAuth2ClientEntity client = client(true);
        client.setType("basic");
        client.setAllowUserCreation(false);
        client.setActivateUser(false);
        when(clients.selectById("client-1")).thenReturn(client);
        when(templates.selectById("template-1")).thenReturn(new OAuth2ClientTemplateEntity());
        when(strategyProvider.getStrategyByType("BASIC")).thenReturn(strategy);
        MappedOAuth2User mapped = new MappedOAuth2User(
                "person@example.com", "First", "Last", "tenant-2", "Tenant", "customer-1", "Customer");
        when(strategy.map(request, authentication, null, client)).thenReturn(mapped);
        when(users.resolve(
                        "tenant-1",
                        "tenant-2",
                        "Tenant",
                        "customer-1",
                        "person@example.com",
                        "First",
                        "Last",
                        "Customer",
                        "Example Login",
                        false,
                        false))
                .thenReturn(new SecurityUser(
                        "user-1",
                        "tenant-2",
                        "customer-1",
                        "person@example.com",
                        "CUSTOMER_USER",
                        "First",
                        "Last",
                        "First Last",
                        "Tenant",
                        Instant.EPOCH));

        SecurityUser user = resolver.resolve(authentication, request);

        assertThat(user.email()).isEqualTo("person@example.com");
        verify(strategyProvider).getStrategyByType("BASIC");
        verify(strategy).map(request, authentication, null, client);
        verify(users)
                .resolve(
                        "tenant-1",
                        "tenant-2",
                        "Tenant",
                        "customer-1",
                        "person@example.com",
                        "First",
                        "Last",
                        "Customer",
                        "Example Login",
                        false,
                        false);
        verify(authorizedClients, never()).loadAuthorizedClient("client-1", null);
    }

    @Test
    void loadsAuthorizedClientForGithubMapper() {
        OAuth2ClientEntity client = client(true);
        client.setType("GITHUB");
        OAuth2AuthorizedClient authorizedClient = org.mockito.Mockito.mock(OAuth2AuthorizedClient.class);
        when(clients.selectById("client-1")).thenReturn(client);
        when(templates.selectById("template-1")).thenReturn(new OAuth2ClientTemplateEntity());
        when(authentication.getName()).thenReturn("provider-subject");
        when(authorizedClients.loadAuthorizedClient("client-1", "provider-subject"))
                .thenReturn(authorizedClient);
        when(strategyProvider.getStrategyByType("GITHUB")).thenReturn(strategy);
        when(strategy.map(request, authentication, authorizedClient, client))
                .thenReturn(new MappedOAuth2User("person@example.com", null, null, null, null, null, null));

        resolver.resolve(authentication, request);

        verify(strategy).map(request, authentication, authorizedClient, client);
        verify(authorizedClients).loadAuthorizedClient("client-1", "provider-subject");
    }

    private void verifyNoIdentityResolution() {
        verifyNoInteractions(strategyProvider, strategy, users, authorizedClients);
    }

    private static OAuth2ClientEntity client(boolean enabled) {
        OAuth2ClientEntity client = new OAuth2ClientEntity();
        client.setId("client-1");
        client.setName("Example Login");
        client.setTenantId("tenant-1");
        client.setTemplateId("template-1");
        client.setType("BASIC");
        client.setEnabled(enabled);
        return client;
    }
}
