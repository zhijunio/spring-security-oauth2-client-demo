package io.github.zhijunio.oauth2client.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.zhijunio.oauth2client.persistence.entity.TenantEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.TenantMapper;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantService {

    public static final String SYSTEM_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    private final TenantMapper tenants;
    private final Clock clock;

    public TenantService(TenantMapper tenants, Clock clock) {
        this.tenants = tenants;
        this.clock = clock;
    }

    public List<TenantInfo> list() {
        return tenants.selectList(Wrappers.<TenantEntity>lambdaQuery().orderByAsc(TenantEntity::getName)).stream()
                .map(this::info)
                .toList();
    }

    public TenantInfo get(String id) {
        return info(require(id));
    }

    @Transactional
    public TenantInfo create(TenantRequest input) {
        String id = input.id() == null || input.id().isBlank()
                ? UUID.randomUUID().toString()
                : input.id().trim();
        if (tenants.selectById(id) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant ID already exists");
        }
        String name = input.name().trim();
        if (findByName(name) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant name already exists");
        }
        TenantEntity tenant = new TenantEntity();
        tenant.setId(id);
        tenant.setCreatedTime(clock.millis());
        tenant.setName(name);
        tenant.setEmail(input.email());
        tenant.setAdditionalInfo(input.additionalInfo());
        tenant.setVersion(1L);
        tenants.insert(tenant);
        return info(tenant);
    }

    public TenantEntity require(String id) {
        TenantEntity tenant = tenants.selectById(id);
        if (tenant == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tenant");
        }
        return tenant;
    }

    public String resolveOAuth2Tenant(TenantEntity clientTenant, String mappedTenantId, String mappedTenantName) {
        String clientTenantId = clientTenant.getId();
        if (!isSystemTenant(clientTenantId)) return clientTenantId;
        if (mappedTenantId != null && !mappedTenantId.isBlank()) {
            String tenantId = mappedTenantId.trim();
            if (isSystemTenant(tenantId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Mapped user tenant cannot be the system tenant");
            }
            return require(tenantId).getId();
        }
        if (mappedTenantName == null || mappedTenantName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "OAuth2 mapper did not provide a tenant name or ID");
        }
        String name = mappedTenantName.trim();
        if (name.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mapped tenant name exceeds 255 characters");
        }
        TenantEntity tenant = findByName(name);
        if (tenant != null) return tenant.getId();

        TenantEntity created = new TenantEntity();
        created.setId(UUID.randomUUID().toString());
        created.setCreatedTime(clock.millis());
        created.setName(name);
        created.setVersion(1L);
        tenants.insert(created);
        return created.getId();
    }

    public static boolean isSystemTenant(String id) {
        return SYSTEM_TENANT_ID.equals(id);
    }

    private TenantEntity findByName(String name) {
        return tenants.selectOne(Wrappers.<TenantEntity>lambdaQuery().eq(TenantEntity::getName, name));
    }

    private TenantInfo info(TenantEntity tenant) {
        return new TenantInfo(
                tenant.getId(),
                tenant.getCreatedTime(),
                tenant.getName(),
                tenant.getEmail(),
                tenant.getAdditionalInfo(),
                isSystemTenant(tenant.getId()));
    }

    public record TenantRequest(
            @jakarta.validation.constraints.Size(max = 36) String id,

            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 255)
            String name,

            @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max = 255)
            String email,

            String additionalInfo) {}

    public record TenantInfo(
            String id, long createdTime, String name, String email, String additionalInfo, boolean systemTenant) {}
}
