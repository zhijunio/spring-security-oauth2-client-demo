package io.github.zhijunio.oauth2client.oauth2.strategy;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GithubOAuth2ClientStrategy extends AbstractOAuth2ClientStrategy {

    @Override
    public String type() {
        return "GITHUB";
    }

    @Override
    public MappedOAuth2User map(
            HttpServletRequest request,
            OAuth2AuthenticationToken authentication,
            OAuth2AuthorizedClient authorizedClient,
            OAuth2ClientEntity client) {
        if (authorizedClient == null) throw new IllegalArgumentException("GitHub authorization token is unavailable");
        String email;
        try {
            List<GithubEmail> emails = RestClient.create()
                    .get()
                    .uri("https://api.github.com/user/emails")
                    .headers(headers -> {
                        headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
                        headers.set("Accept", "application/vnd.github+json");
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            email = emails == null
                    ? null
                    : emails.stream()
                            .filter(GithubEmail::primary)
                            .filter(GithubEmail::verified)
                            .map(GithubEmail::email)
                            .filter(value -> value != null && !value.isBlank())
                            .findFirst()
                            .orElse(null);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unable to retrieve a verified primary GitHub email", exception);
        }
        Map<String, Object> attributes = authentication.getPrincipal().getAttributes();
        return userMapping(
                client,
                attributes,
                email,
                attribute(attributes, client.getBasicFirstNameAttributeKey()),
                attribute(attributes, client.getBasicLastNameAttributeKey()));
    }

    private record GithubEmail(String email, boolean primary, boolean verified) {}
}
