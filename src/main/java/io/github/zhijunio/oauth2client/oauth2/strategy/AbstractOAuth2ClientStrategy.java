package io.github.zhijunio.oauth2client.oauth2.strategy;

import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.service.TenantService;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractOAuth2ClientStrategy implements OAuth2ClientStrategy {

    private static final Pattern ATTRIBUTE_PLACEHOLDER = Pattern.compile("%\\{([^}]+)}");

    protected MappedOAuth2User userMapping(
            OAuth2ClientEntity client,
            Map<String, Object> attributes,
            String email,
            String firstName,
            String lastName) {
        return new MappedOAuth2User(
                email,
                firstName,
                lastName,
                null,
                TenantService.isSystemTenant(client.getTenantId()) ? tenantName(client, email, attributes) : null,
                null,
                customerName(client, attributes));
    }

    protected static String attribute(Map<String, Object> attributes, String key) {
        Object value = key == null ? null : attributes.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    protected static String verifiedEmail(Map<String, Object> attributes, String emailAttributeKey) {
        Object verified = attributes.get("email_verified");
        String key = emailAttributeKey == null ? "email" : emailAttributeKey;
        return verified == null || Boolean.TRUE.equals(verified) ? attribute(attributes, key) : null;
    }

    private static String tenantName(OAuth2ClientEntity client, String email, Map<String, Object> attributes) {
        String strategy = client.getBasicTenantNameStrategy();
        if ("EMAIL".equalsIgnoreCase(strategy)) return email;
        if ("DOMAIN".equalsIgnoreCase(strategy)) {
            int separator = email == null ? -1 : email.lastIndexOf('@');
            if (separator <= 0 || separator == email.length() - 1) {
                throw new IllegalArgumentException("Tenant DOMAIN mapping requires an email with a domain");
            }
            return email.substring(separator + 1).toLowerCase(java.util.Locale.ROOT);
        }
        if (!"CUSTOM".equalsIgnoreCase(strategy)) {
            throw new IllegalArgumentException(
                    "System-tenant BASIC mapper requires EMAIL, DOMAIN, or CUSTOM tenant mapping");
        }
        String pattern = client.getBasicTenantNamePattern();
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Tenant CUSTOM mapping requires a tenant name pattern");
        }
        return replaceAttributes(pattern, attributes, true);
    }

    private static String customerName(OAuth2ClientEntity client, Map<String, Object> attributes) {
        String pattern = client.getBasicCustomerNamePattern();
        if (pattern == null || pattern.isBlank()) return null;
        String name = replaceAttributes(pattern, attributes, false).trim();
        return name.isBlank() ? null : name;
    }

    private static String replaceAttributes(String pattern, Map<String, Object> attributes, boolean required) {
        return ATTRIBUTE_PLACEHOLDER.matcher(pattern).replaceAll(match -> {
            String key = match.group(1);
            Object value = attributes.get(key);
            if (required && (value == null || value.toString().isBlank())) {
                throw new IllegalArgumentException("Tenant mapping attribute is missing: " + key);
            }
            return Matcher.quoteReplacement(value == null ? "" : value.toString());
        });
    }
}
