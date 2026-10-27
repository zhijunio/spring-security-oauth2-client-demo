package io.github.zhijunio.oauth2client.security;

import io.github.zhijunio.oauth2client.oauth2.ExternalUserResolver;
import io.github.zhijunio.oauth2client.service.LoginSession;
import io.github.zhijunio.oauth2client.token.JwtTokenFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class Oauth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String SESSION_ATTRIBUTE =
            Oauth2AuthenticationSuccessHandler.class.getName() + ".PLATFORM_LOGIN";

    private final ExternalUserResolver accounts;
    private final JwtTokenFactory tokenIssuer;
    private final Clock clock;

    public Oauth2AuthenticationSuccessHandler(ExternalUserResolver accounts, JwtTokenFactory tokenIssuer, Clock clock) {
        this.accounts = accounts;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2)) {
            SecurityContextHolder.clearContext();
            response.sendRedirect("/login?error");
            return;
        }
        SecurityUser securityUser = accounts.resolve(oauth2, request);
        JwtTokenFactory.IssuedAccessToken token =
                tokenIssuer.createAccessToken(securityUser, oauth2.getAuthorizedClientRegistrationId());
        request.getSession(true)
                .setAttribute(
                        SESSION_ATTRIBUTE,
                        new LoginSession(securityUser, token.value(), clock.instant(), token.expiresAt()));
        response.sendRedirect("/");
    }
}
