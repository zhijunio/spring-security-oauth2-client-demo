package io.github.zhijunio.oauth2client.oauth2.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.service.TenantService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class BasicOAuth2ClientStrategyTest {

    private final BasicOAuth2ClientStrategy strategy = new BasicOAuth2ClientStrategy();

    @Test
    void mapsVerifiedIdentityAndSystemTenantClaims() {
        OAuth2ClientEntity client = new OAuth2ClientEntity();
        client.setTenantId(TenantService.SYSTEM_TENANT_ID);
        client.setBasicEmailAttributeKey("email");
        client.setBasicFirstNameAttributeKey("given_name");
        client.setBasicLastNameAttributeKey("family_name");
        client.setBasicTenantNameStrategy("DOMAIN");
        client.setBasicCustomerNamePattern("%{preferred_username}");

        MappedOAuth2User mapped = strategy.map(
                null,
                authentication(Map.of(
                        "sub", "subject-1",
                        "email", "Alice@Acme.example",
                        "email_verified", true,
                        "given_name", "Alice",
                        "family_name", "Example",
                        "preferred_username", "acme-ops")),
                null,
                client);

        assertThat(mapped.email()).isEqualTo("Alice@Acme.example");
        assertThat(mapped.firstName()).isEqualTo("Alice");
        assertThat(mapped.lastName()).isEqualTo("Example");
        assertThat(mapped.tenantName()).isEqualTo("acme.example");
        assertThat(mapped.customerName()).isEqualTo("acme-ops");
    }

    @Test
    void doesNotMapUnverifiedEmail() {
        OAuth2ClientEntity client = new OAuth2ClientEntity();
        client.setTenantId("tenant-1");

        MappedOAuth2User mapped = strategy.map(
                null,
                authentication(Map.of("sub", "subject-1", "email", "user@example.com", "email_verified", false)),
                null,
                client);

        assertThat(mapped.email()).isNull();
    }

    @Test
    void acceptsEmailWhenProviderOmitsVerificationClaim() {
        OAuth2ClientEntity client = new OAuth2ClientEntity();
        client.setTenantId("tenant-1");

        MappedOAuth2User mapped = strategy.map(
                null, authentication(Map.of("sub", "subject-1", "email", "user@example.com")), null, client);

        assertThat(mapped.email()).isEqualTo("user@example.com");
    }

    private static OAuth2AuthenticationToken authentication(Map<String, Object> claims) {
        DefaultOAuth2User principal =
                new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), claims, "sub");
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "dex-local");
    }
}
