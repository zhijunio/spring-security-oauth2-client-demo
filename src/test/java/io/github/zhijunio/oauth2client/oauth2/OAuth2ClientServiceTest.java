package io.github.zhijunio.oauth2client.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientDataMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientTemplateMapper;
import io.github.zhijunio.oauth2client.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientServiceTest {

    @Mock
    private OAuth2ClientDataMapper clients;

    @Mock
    private OAuth2ClientTemplateMapper templates;

    @Mock
    private DomainService domains;

    @Mock
    private TenantService tenants;

    private OAuth2ClientService service;

    @BeforeEach
    void setUp() {
        service = new OAuth2ClientService(clients, templates, domains, tenants);
    }

    @Test
    void returnsNoLoginButtonsWhenDomainHasNoEnabledClientLinks() {
        when(domains.enabledClientIdsForDomain("login.example.com")).thenReturn(List.of());

        List<OAuth2ClientService.OAuth2LoginClient> result = service.enabledClientsForDomain("login.example.com");

        assertThat(result).isEmpty();
        verify(clients, never()).selectBatchIds(anyList());
    }

    @Test
    void showsOnlyEnabledClientsSortedByNameAndUsesConfiguredButtonLabel() {
        when(domains.enabledClientIdsForDomain("login.example.com"))
                .thenReturn(List.of("github", "disabled", "google"));
        when(clients.selectBatchIds(anyList()))
                .thenReturn(List.of(
                        client("github", "GitHub", true, "Continue with GitHub", "github-icon"),
                        client("disabled", "Apple", false, "", "apple-icon"),
                        client("google", "Google", true, " ", "google-icon")));

        List<OAuth2ClientService.OAuth2LoginClient> result = service.enabledClientsForDomain("login.example.com");

        assertThat(result)
                .containsExactly(
                        new OAuth2ClientService.OAuth2LoginClient("github", "Continue with GitHub", "github-icon"),
                        new OAuth2ClientService.OAuth2LoginClient("google", "Google", "google-icon"));
    }

    private static OAuth2ClientEntity client(String id, String name, boolean enabled, String label, String icon) {
        OAuth2ClientEntity client = new OAuth2ClientEntity();
        client.setId(id);
        client.setName(name);
        client.setEnabled(enabled);
        client.setLoginButtonLabel(label);
        client.setLoginButtonIcon(icon);
        return client;
    }
}
