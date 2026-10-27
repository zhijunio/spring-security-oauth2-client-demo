package io.github.zhijunio.oauth2client.oauth2.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class AppleOAuth2ClientStrategyTest {

    private final AppleOAuth2ClientStrategy strategy = new AppleOAuth2ClientStrategy();

    @Test
    void mapsNameFromAppleUserParameterAlongsideVerifiedClaims() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user", "{\"name\":{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}}");

        MappedOAuth2User mapped = strategy.map(
                request,
                authentication(Map.of("sub", "subject-1", "email", "ada@example.com", "email_verified", true)),
                null,
                client());

        assertThat(mapped.email()).isEqualTo("ada@example.com");
        assertThat(mapped.firstName()).isEqualTo("Ada");
        assertThat(mapped.lastName()).isEqualTo("Lovelace");
    }

    @Test
    void keepsTokenClaimsWhenAppleUserParameterIsMalformed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user", "not-json");

        MappedOAuth2User mapped = strategy.map(
                request,
                authentication(Map.of(
                        "sub", "subject-1",
                        "email", "ada@example.com",
                        "email_verified", true,
                        "firstName", "Ada")),
                null,
                client());

        assertThat(mapped.email()).isEqualTo("ada@example.com");
        assertThat(mapped.firstName()).isEqualTo("Ada");
        assertThat(mapped.lastName()).isNull();
    }

    @Test
    void doesNotAcceptUnverifiedAppleEmail() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        MappedOAuth2User mapped = strategy.map(
                request,
                authentication(Map.of("sub", "subject-1", "email", "ada@example.com", "email_verified", false)),
                null,
                client());

        assertThat(mapped.email()).isNull();
    }

    private static OAuth2ClientEntity client() {
        OAuth2ClientEntity client = new OAuth2ClientEntity();
        client.setTenantId("tenant-1");
        client.setBasicEmailAttributeKey("email");
        client.setBasicFirstNameAttributeKey("firstName");
        client.setBasicLastNameAttributeKey("lastName");
        return client;
    }

    private static OAuth2AuthenticationToken authentication(Map<String, Object> claims) {
        DefaultOAuth2User principal =
                new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), claims, "sub");
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "apple");
    }
}
