package io.github.zhijunio.oauth2client.oauth2.strategy;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AppleOAuth2ClientStrategy extends AbstractOAuth2ClientStrategy {

    @Override
    public String type() {
        return "APPLE";
    }

    @Override
    public MappedOAuth2User map(
            HttpServletRequest request,
            OAuth2AuthenticationToken authentication,
            OAuth2AuthorizedClient authorizedClient,
            OAuth2ClientEntity client) {
        Map<String, Object> attributes =
                appleAttributes(request, authentication.getPrincipal().getAttributes());
        return userMapping(
                client,
                attributes,
                verifiedEmail(attributes, client.getBasicEmailAttributeKey()),
                attribute(attributes, client.getBasicFirstNameAttributeKey()),
                attribute(attributes, client.getBasicLastNameAttributeKey()));
    }

    private static Map<String, Object> appleAttributes(HttpServletRequest request, Map<String, Object> claims) {
        String userParameter = request.getParameter("user");
        if (userParameter == null || userParameter.isBlank()) return claims;
        try {
            Map<String, Object> user = JsonParserFactory.getJsonParser().parseMap(userParameter);
            Object nameValue = user.get("name");
            if (!(nameValue instanceof Map<?, ?> name)) return claims;
            Map<String, Object> attributes = new HashMap<>(claims);
            copyStringAttribute(name, attributes, "firstName");
            copyStringAttribute(name, attributes, "lastName");
            return attributes;
        } catch (RuntimeException ignored) {
            return claims;
        }
    }

    private static void copyStringAttribute(Map<?, ?> source, Map<String, Object> destination, String key) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) destination.put(key, text);
    }
}
