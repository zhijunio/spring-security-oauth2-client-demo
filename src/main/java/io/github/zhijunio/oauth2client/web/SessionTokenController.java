package io.github.zhijunio.oauth2client.web;

import io.github.zhijunio.oauth2client.security.Oauth2AuthenticationSuccessHandler;
import io.github.zhijunio.oauth2client.service.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionTokenController {

    @PostMapping("/api/session/platform-token")
    ResponseEntity<TokenResponse> platformToken(HttpServletRequest request) {
        Object value = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(Oauth2AuthenticationSuccessHandler.SESSION_ATTRIBUTE);
        if (!(value instanceof LoginSession session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TokenResponse(session.accessToken(), session.expiresAt(), "Bearer"));
    }

    public record TokenResponse(String accessToken, Instant expiresAt, String tokenType) {}
}
