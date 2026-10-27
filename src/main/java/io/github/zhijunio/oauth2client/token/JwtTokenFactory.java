package io.github.zhijunio.oauth2client.token;

import io.github.zhijunio.oauth2client.security.SecurityUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenFactory {

    public static final String ISSUER = "http://localhost:8080";
    public static final String AUDIENCE = "oauth2-client-platform-api";
    public static final Duration LIFETIME = Duration.ofMinutes(5);

    private final JwtEncoder encoder;
    private final Clock clock;

    public JwtTokenFactory(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    public IssuedAccessToken createAccessToken(SecurityUser securityUser, String identityProvider) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(LIFETIME);
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(securityUser.id())
                .audience(List.of(AUDIENCE))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of(securityUser.authority()))
                .claim("identity_provider", identityProvider);
        if (securityUser.tenantId() != null) claimsBuilder.claim("tenant_id", securityUser.tenantId());
        if (securityUser.customerId() != null) claimsBuilder.claim("customer_id", securityUser.customerId());
        JwtClaimsSet claims = claimsBuilder.build();
        Jwt jwt = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
        return new IssuedAccessToken(jwt.getTokenValue(), issuedAt, expiresAt);
    }

    public record IssuedAccessToken(String value, Instant issuedAt, Instant expiresAt) {}
}
