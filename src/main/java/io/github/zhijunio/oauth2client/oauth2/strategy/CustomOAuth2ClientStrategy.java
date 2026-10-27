package io.github.zhijunio.oauth2client.oauth2.strategy;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CustomOAuth2ClientStrategy implements OAuth2ClientStrategy {

    @Override
    public String type() {
        return "CUSTOM";
    }

    @Override
    public MappedOAuth2User map(
            HttpServletRequest request,
            OAuth2AuthenticationToken authentication,
            OAuth2AuthorizedClient authorizedClient,
            OAuth2ClientEntity client) {
        URI uri = safeUri(client.getCustomUrl());
        Map<String, Object> attributes = authentication.getPrincipal().getAttributes();
        CustomMapperResult result = RestClient.create()
                .post()
                .uri(uri)
                .headers(headers -> {
                    if (client.getCustomUsername() != null && client.getCustomPassword() != null) {
                        headers.setBasicAuth(client.getCustomUsername(), client.getCustomPassword());
                    }
                    if (Boolean.TRUE.equals(client.getCustomSendToken()) && authorizedClient != null) {
                        headers.set(
                                "provider-access-token",
                                authorizedClient.getAccessToken().getTokenValue());
                    }
                })
                .body(attributes)
                .retrieve()
                .body(CustomMapperResult.class);
        if (result == null || result.email() == null || result.email().isBlank()) {
            throw new IllegalArgumentException("Custom mapper did not return an email address");
        }
        return new MappedOAuth2User(
                result.email(),
                result.firstName(),
                result.lastName(),
                result.tenantId(),
                result.tenantName(),
                result.customerId(),
                result.customerName());
    }

    private static URI safeUri(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Custom mapper URL must be an HTTPS URL without user info");
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new IllegalArgumentException("Custom mapper URL must resolve to a public address");
                }
            }
            return uri;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Custom mapper URL is invalid or unsafe", exception);
        }
    }

    private record CustomMapperResult(
            String email,
            String firstName,
            String lastName,
            String tenantId,
            String tenantName,
            String customerId,
            String customerName) {}
}
