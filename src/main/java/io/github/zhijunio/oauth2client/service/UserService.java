package io.github.zhijunio.oauth2client.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.zhijunio.oauth2client.persistence.entity.TenantEntity;
import io.github.zhijunio.oauth2client.persistence.entity.UserCredentialsEntity;
import io.github.zhijunio.oauth2client.persistence.entity.UserEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.UserCredentialsMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.UserMapper;
import io.github.zhijunio.oauth2client.security.SecurityUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserMapper users;
    private final TenantService tenants;
    private final CustomerService customers;
    private final UserCredentialsMapper credentials;
    private final Clock clock;

    public UserService(
            UserMapper users,
            UserCredentialsMapper credentials,
            TenantService tenants,
            CustomerService customers,
            Clock clock) {
        this.users = users;
        this.credentials = credentials;
        this.tenants = tenants;
        this.customers = customers;
        this.clock = clock;
    }

    @Transactional
    public SecurityUser resolve(
            String clientTenantId,
            String mappedTenantId,
            String mappedTenantName,
            String mappedCustomerId,
            String email,
            String firstName,
            String lastName,
            String customerName,
            String providerName,
            boolean allowUserCreation,
            boolean activateUser) {
        TenantEntity clientTenant = tenants.require(clientTenantId);
        boolean systemClient = TenantService.isSystemTenant(clientTenant.getId());
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identity provider did not return an email");
        }
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        UserEntity user = users.selectOne(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, normalizedEmail));
        if (user != null) {
            if (!systemClient && !clientTenantId.equals(user.getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User belongs to another tenant");
            }
            return securityUser(user);
        }
        if (!allowUserCreation) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User creation is disabled for this client");
        }

        String tenantId = tenants.resolveOAuth2Tenant(clientTenant, mappedTenantId, mappedTenantName);

        long now = clock.millis();
        UserEntity candidate = new UserEntity();
        candidate.setId(UUID.randomUUID().toString());
        candidate.setTenantId(tenantId);
        candidate.setEmail(normalizedEmail);
        if (mappedCustomerId != null && !mappedCustomerId.isBlank()) {
            candidate.setCustomerId(customers
                    .requireForTenant(tenantId, mappedCustomerId.trim())
                    .getId());
        } else if (customerName != null && !customerName.isBlank()) {
            candidate.setCustomerId(
                    customers.findOrCreate(tenantId, customerName.trim()).getId());
        }
        candidate.setAuthority(candidate.getCustomerId() == null ? "TENANT_ADMIN" : "CUSTOMER_USER");
        candidate.setFirstName(blankToNull(firstName));
        candidate.setLastName(blankToNull(lastName));
        candidate.setAdditionalInfo(additionalInfo(providerName));
        candidate.setCreatedTime(now);
        candidate.setVersion(1L);
        int inserted = users.insertIfAbsent(candidate);
        if (inserted == 1) {
            UserCredentialsEntity userCredentials = new UserCredentialsEntity();
            userCredentials.setId(UUID.randomUUID().toString());
            userCredentials.setCreatedTime(now);
            userCredentials.setUserId(candidate.getId());
            userCredentials.setEnabled(activateUser);
            credentials.insert(userCredentials);
        }
        user = users.selectOne(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, normalizedEmail));
        if (user == null) {
            throw new IllegalStateException("User insert did not produce a user");
        }
        if (!systemClient && !tenantId.equals(user.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User belongs to another tenant");
        }
        return securityUser(user);
    }

    public List<UserInfo> list() {
        return users
                .selectList(Wrappers.<UserEntity>lambdaQuery()
                        .orderByDesc(UserEntity::getCreatedTime)
                        .last("LIMIT 100"))
                .stream()
                .map(this::info)
                .toList();
    }

    public UserInfo get(String id) {
        return info(require(id));
    }

    public UserEntity require(String id) {
        UserEntity user = users.selectById(id);
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        return user;
    }

    private static String additionalInfo(String providerName) {
        return org.springframework.boot.json.JsonWriter.standard()
                .writeToString(Map.of("authProviderName", providerName));
    }

    private SecurityUser securityUser(UserEntity user) {
        String name = java.util.stream.Stream.of(user.getFirstName(), user.getLastName())
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
        if (name.isBlank()) name = user.getEmail();
        String tenantName = tenants.get(user.getTenantId()).name();
        return new SecurityUser(
                user.getId(),
                user.getTenantId(),
                user.getCustomerId(),
                user.getEmail(),
                user.getAuthority(),
                user.getFirstName(),
                user.getLastName(),
                name,
                tenantName,
                Instant.ofEpochMilli(user.getCreatedTime()));
    }

    private UserInfo info(UserEntity user) {
        return new UserInfo(
                user.getId(),
                user.getCreatedTime(),
                user.getTenantId(),
                user.getCustomerId(),
                user.getEmail(),
                user.getAuthority(),
                user.getFirstName(),
                user.getLastName(),
                parseAdditionalInfo(user.getAdditionalInfo()),
                user.getVersion());
    }

    private static Map<String, Object> parseAdditionalInfo(String value) {
        if (value == null || value.isBlank()) return null;
        return org.springframework.boot.json.JsonParserFactory.getJsonParser().parseMap(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record UserInfo(
            String id,
            long createdTime,
            String tenantId,
            String customerId,
            String email,
            String authority,
            String firstName,
            String lastName,
            Map<String, Object> additionalInfo,
            Long version) {}
}
