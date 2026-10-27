package io.github.zhijunio.oauth2client.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.zhijunio.oauth2client.security.SecurityUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtTokenFactoryTest {

    @Mock
    private JwtEncoder encoder;

    private JwtTokenFactory factory;

    @BeforeEach
    void setUp() {
        factory = new JwtTokenFactory(encoder, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        when(encoder.encode(any()))
                .thenReturn(Jwt.withTokenValue("signed-token")
                        .header("alg", "HS256")
                        .claim("sub", "user-1")
                        .build());
    }

    @Test
    void issuesShortLivedPlatformTokenWithIdentityClaims() {
        SecurityUser user = user("tenant-1", "customer-1");

        JwtTokenFactory.IssuedAccessToken token = factory.createAccessToken(user, "dex-local");

        ArgumentCaptor<JwtEncoderParameters> parameters = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(parameters.capture());
        var claims = parameters.getValue().getClaims();
        assertThat(parameters.getValue().getJwsHeader().getAlgorithm().getName())
                .isEqualTo("HS256");
        assertThat(claims.getIssuer().toString()).isEqualTo(JwtTokenFactory.ISSUER);
        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.getAudience()).containsExactly(JwtTokenFactory.AUDIENCE);
        assertThat(claims.getClaims().get("roles")).isEqualTo(List.of("CUSTOMER_USER"));
        assertThat(claims.getClaims().get("identity_provider")).isEqualTo("dex-local");
        assertThat(claims.getClaims().get("tenant_id")).isEqualTo("tenant-1");
        assertThat(claims.getClaims().get("customer_id")).isEqualTo("customer-1");
        assertThat(token.value()).isEqualTo("signed-token");
        assertThat(token.issuedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(token.expiresAt()).isEqualTo(Instant.parse("2026-01-01T00:05:00Z"));
    }

    @Test
    void omitsTenantAndCustomerClaimsWhenAbsent() {
        SecurityUser user = user(null, null);

        factory.createAccessToken(user, "github");

        ArgumentCaptor<JwtEncoderParameters> parameters = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(parameters.capture());
        assertThat(parameters.getValue().getClaims().getClaims()).doesNotContainKeys("tenant_id", "customer_id");
    }

    private static SecurityUser user(String tenantId, String customerId) {
        return new SecurityUser(
                "user-1",
                tenantId,
                customerId,
                "user@example.com",
                "CUSTOMER_USER",
                "User",
                "Example",
                "User Example",
                "Tenant",
                Instant.parse("2025-12-31T00:00:00Z"));
    }
}
