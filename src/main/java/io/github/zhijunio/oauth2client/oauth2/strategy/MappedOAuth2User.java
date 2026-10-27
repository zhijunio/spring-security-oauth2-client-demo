package io.github.zhijunio.oauth2client.oauth2.strategy;

public record MappedOAuth2User(
        String email,
        String firstName,
        String lastName,
        String tenantId,
        String tenantName,
        String customerId,
        String customerName) {}
