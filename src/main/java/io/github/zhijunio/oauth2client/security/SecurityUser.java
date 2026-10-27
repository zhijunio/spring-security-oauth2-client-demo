package io.github.zhijunio.oauth2client.security;

import java.time.Instant;

/** Authenticated local platform identity used by the OAuth2 flow and platform token factory. */
public record SecurityUser(
        String id,
        String tenantId,
        String customerId,
        String email,
        String authority,
        String firstName,
        String lastName,
        String displayName,
        String tenantName,
        Instant createdAt) {}
