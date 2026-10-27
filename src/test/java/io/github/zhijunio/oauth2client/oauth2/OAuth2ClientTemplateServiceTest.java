package io.github.zhijunio.oauth2client.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientTemplateEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientDataMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientTemplateMapper;
import io.github.zhijunio.oauth2client.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientTemplateServiceTest {

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
    void savesBasicTemplateWithInferredProtocolAndMapperDefaults() {
        when(templates.selectById("template-1")).thenReturn(null);
        OAuth2ClientService.OAuth2MapperConfigRequest mapperConfig = new OAuth2ClientService.OAuth2MapperConfigRequest(
                "BASIC",
                false,
                true,
                new OAuth2ClientService.BasicMapperConfigRequest(
                        "email", "given_name", "family_name", null, null, null),
                null);

        OAuth2ClientService.OAuth2ClientTemplateInfo saved = service.saveTemplate(
                templateRequest("template-1", "Dex", null, List.of("openid", "email"), mapperConfig));

        ArgumentCaptor<OAuth2ClientTemplateEntity> template = ArgumentCaptor.forClass(OAuth2ClientTemplateEntity.class);
        verify(templates).insert(template.capture());
        assertThat(template.getValue().getType()).isEqualTo("BASIC");
        assertThat(template.getValue().getProtocol()).isEqualTo("OIDC");
        assertThat(template.getValue().getUserNameAttribute()).isEqualTo("sub");
        assertThat(template.getValue().getDefaultScopes()).isEqualTo("openid,email");
        assertThat(template.getValue().getAllowUserCreation()).isFalse();
        assertThat(template.getValue().getActivateUser()).isTrue();
        assertThat(template.getValue().getClientAuthenticationMethod()).isEqualTo("BASIC");
        assertThat(saved.name()).isEqualTo("Dex");
        assertThat(saved.mapperConfig().basic().emailAttributeKey()).isEqualTo("email");
    }

    @Test
    void rejectsCustomMapperWithoutHttpsUrl() {
        OAuth2ClientService.OAuth2MapperConfigRequest mapperConfig = new OAuth2ClientService.OAuth2MapperConfigRequest(
                "CUSTOM",
                null,
                null,
                null,
                new OAuth2ClientService.CustomMapperConfigRequest("http://mapper.example", null, null, false));

        assertThatThrownBy(() -> service.saveTemplate(
                        templateRequest("template-1", "Custom", "OAUTH2", List.of(), mapperConfig)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CUSTOM mapperConfig requires an HTTPS URL");

        verify(templates, never()).insert(any(OAuth2ClientTemplateEntity.class));
    }

    @Test
    void rejectsUnsupportedMapperAndAuthenticationMethods() {
        OAuth2ClientService.OAuth2MapperConfigRequest unsupported =
                new OAuth2ClientService.OAuth2MapperConfigRequest("DEX", null, null, null, null);
        assertThatThrownBy(() ->
                        service.saveTemplate(templateRequest("template-1", "Dex", "OIDC", List.of(), unsupported)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported mapperConfig.type");

        when(templates.selectById("template-2")).thenReturn(null);
        OAuth2ClientService.OAuth2ClientTemplateRequest invalidAuthentication =
                new OAuth2ClientService.OAuth2ClientTemplateRequest(
                        "template-2",
                        "Dex",
                        "OIDC",
                        null,
                        "https://idp.example/auth",
                        "https://idp.example/token",
                        List.of(),
                        "https://idp.example/userinfo",
                        "sub",
                        null,
                        "POST_BODY",
                        null,
                        null,
                        null,
                        null,
                        new OAuth2ClientService.OAuth2MapperConfigRequest("BASIC", null, null, null, null));
        assertThatThrownBy(() -> service.saveTemplate(invalidAuthentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("clientAuthenticationMethod must be BASIC or POST");
    }

    @Test
    void refusesToDeleteTemplateReferencedByClient() {
        when(clients.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteTemplate("template-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Template is referenced by an OAuth2 client");

        verify(templates, never()).deleteById("template-1");
    }

    private static OAuth2ClientService.OAuth2ClientTemplateRequest templateRequest(
            String id,
            String name,
            String protocol,
            List<String> scopes,
            OAuth2ClientService.OAuth2MapperConfigRequest mapperConfig) {
        return new OAuth2ClientService.OAuth2ClientTemplateRequest(
                id,
                name,
                protocol,
                null,
                "https://idp.example/authorize",
                "https://idp.example/token",
                scopes,
                "https://idp.example/userinfo",
                null,
                null,
                null,
                "template comment",
                "idp-icon",
                null,
                "https://docs.example/login",
                mapperConfig);
    }
}
