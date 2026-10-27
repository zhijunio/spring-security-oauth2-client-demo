package io.github.zhijunio.oauth2client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.zhijunio.oauth2client.persistence.entity.TenantEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.TenantMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantMapper tenants;

    private TenantService service;

    @BeforeEach
    void setUp() {
        service = new TenantService(tenants, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void keepsOrdinaryClientTenantRegardlessOfMappedValues() {
        TenantEntity clientTenant = tenant("tenant-1", "Default");

        String resolved = service.resolveOAuth2Tenant(clientTenant, "tenant-2", "other.example");

        assertThat(resolved).isEqualTo("tenant-1");
    }

    @Test
    void resolvesSystemClientMappingByTenantId() {
        when(tenants.selectById("tenant-2")).thenReturn(tenant("tenant-2", "Mapped"));

        String resolved = service.resolveOAuth2Tenant(systemTenant(), " tenant-2 ", null);

        assertThat(resolved).isEqualTo("tenant-2");
    }

    @Test
    void reusesTenantFoundByMappedName() {
        when(tenants.selectOne(any())).thenReturn(tenant("tenant-2", "acme.example"));

        String resolved = service.resolveOAuth2Tenant(systemTenant(), null, " acme.example ");

        assertThat(resolved).isEqualTo("tenant-2");
    }

    @Test
    void createsTenantWhenMappedNameDoesNotExist() {
        when(tenants.selectOne(any())).thenReturn(null);

        String resolved = service.resolveOAuth2Tenant(systemTenant(), null, " acme.example ");

        ArgumentCaptor<TenantEntity> created = ArgumentCaptor.forClass(TenantEntity.class);
        verify(tenants).insert(created.capture());
        assertThat(created.getValue().getId()).isEqualTo(resolved);
        assertThat(created.getValue().getName()).isEqualTo("acme.example");
        assertThat(created.getValue().getCreatedTime())
                .isEqualTo(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli());
    }

    @Test
    void rejectsMissingOrSystemTenantMapping() {
        assertThatThrownBy(() -> service.resolveOAuth2Tenant(systemTenant(), null, " "))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThatThrownBy(() -> service.resolveOAuth2Tenant(systemTenant(), TenantService.SYSTEM_TENANT_ID, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static TenantEntity tenant(String id, String name) {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(id);
        tenant.setName(name);
        return tenant;
    }

    private static TenantEntity systemTenant() {
        return tenant(TenantService.SYSTEM_TENANT_ID, "System");
    }
}
