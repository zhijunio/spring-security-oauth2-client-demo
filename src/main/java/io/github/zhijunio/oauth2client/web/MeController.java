package io.github.zhijunio.oauth2client.web;

import java.time.Instant;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(
                jwt.getSubject(),
                jwt.getIssuer().toString(),
                jwt.getAudience(),
                jwt.getClaimAsStringList("roles"),
                jwt.getIssuedAt(),
                jwt.getExpiresAt());
    }

    public record MeResponse(
            String userId,
            String issuer,
            List<String> audience,
            List<String> roles,
            Instant issuedAt,
            Instant expiresAt) {}
}
