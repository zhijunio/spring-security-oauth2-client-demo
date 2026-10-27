package io.github.zhijunio.oauth2client.oauth2.strategy;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class BasicOAuth2ClientStrategy extends AbstractOAuth2ClientStrategy {

    @Override
    public String type() {
        return "BASIC";
    }

    @Override
    public MappedOAuth2User map(
            HttpServletRequest request,
            OAuth2AuthenticationToken authentication,
            OAuth2AuthorizedClient authorizedClient,
            OAuth2ClientEntity client) {
        Map<String, Object> attributes = authentication.getPrincipal().getAttributes();
        return userMapping(
                client,
                attributes,
                verifiedEmail(attributes, client.getBasicEmailAttributeKey()),
                attribute(attributes, client.getBasicFirstNameAttributeKey()),
                attribute(attributes, client.getBasicLastNameAttributeKey()));
    }
}
