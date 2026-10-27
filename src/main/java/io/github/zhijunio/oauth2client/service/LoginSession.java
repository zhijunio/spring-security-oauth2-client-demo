package io.github.zhijunio.oauth2client.service;

import io.github.zhijunio.oauth2client.security.SecurityUser;
import java.time.Instant;

/** Server-side BFF state. The platform token is not rendered into the initial HTML. */
public record LoginSession(SecurityUser securityUser, String accessToken, Instant authenticatedAt, Instant expiresAt) {}
