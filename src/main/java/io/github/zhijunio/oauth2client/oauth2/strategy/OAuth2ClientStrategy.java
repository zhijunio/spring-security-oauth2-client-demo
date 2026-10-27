package io.github.zhijunio.oauth2client.oauth2.strategy;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public interface OAuth2ClientStrategy {

    String type();

    MappedOAuth2User map(
            HttpServletRequest request,
            OAuth2AuthenticationToken authentication,
            OAuth2AuthorizedClient authorizedClient,
            OAuth2ClientEntity client);
}
