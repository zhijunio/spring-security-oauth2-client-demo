package io.github.zhijunio.oauth2client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.zhijunio.oauth2client.persistence.entity.CustomerEntity;
import io.github.zhijunio.oauth2client.persistence.entity.TenantEntity;
import io.github.zhijunio.oauth2client.persistence.entity.UserCredentialsEntity;
import io.github.zhijunio.oauth2client.persistence.entity.UserEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.TenantMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.UserCredentialsMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.UserMapper;
import io.github.zhijunio.oauth2client.security.SecurityUser;
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
class UserServiceTest {

    @Mock
    private UserMapper users;

    @Mock
    private UserCredentialsMapper credentials;

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private CustomerService customers;

    private UserService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new UserService(users, credentials, new TenantService(tenantMapper, clock), customers, clock);
    }

    @Test
    void requiresEmailBeforeCreatingUser() {
        when(tenantMapper.selectById("tenant-1")).thenReturn(tenant("tenant-1", "Default"));

        assertStatus(() -> resolve("tenant-1", null, null, null, true, true), HttpStatus.UNAUTHORIZED);

        verify(users, never()).insertIfAbsent(any());
    }

    @Test
    void rejectsUnknownUserWhenCreationIsDisabled() {
        when(tenantMapper.selectById("tenant-1")).thenReturn(tenant("tenant-1", "Default"));
        when(users.selectOne(any())).thenReturn(null);

        assertStatus(() -> resolve("tenant-1", null, null, "user@example.com", false, true), HttpStatus.FORBIDDEN);

        verify(users, never()).insertIfAbsent(any());
    }

    @Test
    void createsTenantAdminAndHonorsInactiveAccountSetting() {
        when(tenantMapper.selectById("tenant-1")).thenReturn(tenant("tenant-1", "Default"));
        UserEntity createdUser = new UserEntity();
        createdUser.setId("new-user");
        createdUser.setTenantId("tenant-1");
        createdUser.setEmail("user@example.com");
        createdUser.setAuthority("TENANT_ADMIN");
        createdUser.setFirstName("New");
        createdUser.setLastName("User");
        createdUser.setCreatedTime(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli());
        when(users.selectOne(any())).thenReturn(null, createdUser);
        when(users.insertIfAbsent(any())).thenReturn(1);

        SecurityUser result = resolve("tenant-1", null, null, " USER@EXAMPLE.COM ", true, false);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).insertIfAbsent(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(userCaptor.getValue().getAuthority()).isEqualTo("TENANT_ADMIN");
        ArgumentCaptor<UserCredentialsEntity> credentialsCaptor = ArgumentCaptor.forClass(UserCredentialsEntity.class);
        verify(credentials).insert(credentialsCaptor.capture());
        assertThat(credentialsCaptor.getValue().getUserId())
                .isEqualTo(userCaptor.getValue().getId());
        assertThat(credentialsCaptor.getValue().getEnabled()).isFalse();
        assertThat(result.authority()).isEqualTo("TENANT_ADMIN");
        assertThat(result.tenantName()).isEqualTo("Default");
    }

    @Test
    void assignsCustomerUserAuthorityWhenCustomerIsMapped() {
        when(tenantMapper.selectById("tenant-1")).thenReturn(tenant("tenant-1", "Default"));
        CustomerEntity customer = new CustomerEntity();
        customer.setId("customer-1");
        customer.setTenantId("tenant-1");
        when(customers.findOrCreate("tenant-1", "acme-ops")).thenReturn(customer);
        UserEntity createdUser = new UserEntity();
        createdUser.setId("new-user");
        createdUser.setTenantId("tenant-1");
        createdUser.setCustomerId("customer-1");
        createdUser.setEmail("user@example.com");
        createdUser.setAuthority("CUSTOMER_USER");
        createdUser.setCreatedTime(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli());
        when(users.selectOne(any())).thenReturn(null, createdUser);
        when(users.insertIfAbsent(any())).thenReturn(1);

        SecurityUser result = resolve("tenant-1", null, "acme-ops", "user@example.com", true, true);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).insertIfAbsent(userCaptor.capture());
        assertThat(userCaptor.getValue().getCustomerId()).isEqualTo("customer-1");
        assertThat(userCaptor.getValue().getAuthority()).isEqualTo("CUSTOMER_USER");
        assertThat(result.authority()).isEqualTo("CUSTOMER_USER");
    }

    @Test
    void rejectsExistingUserFromAnotherTenant() {
        when(tenantMapper.selectById("tenant-1")).thenReturn(tenant("tenant-1", "Default"));
        UserEntity existing = new UserEntity();
        existing.setId("existing-user");
        existing.setTenantId("tenant-2");
        existing.setEmail("user@example.com");
        when(users.selectOne(any())).thenReturn(existing);

        assertStatus(() -> resolve("tenant-1", null, null, "user@example.com", true, true), HttpStatus.FORBIDDEN);

        verify(users, never()).insertIfAbsent(any());
    }

    @Test
    void reusesExistingUserWithoutReapplyingNewUserMappings() {
        when(tenantMapper.selectById("tenant-1")).thenReturn(tenant("tenant-1", "Default"));
        UserEntity existing = new UserEntity();
        existing.setId("existing-user");
        existing.setTenantId("tenant-1");
        existing.setCustomerId("existing-customer");
        existing.setEmail("user@example.com");
        existing.setAuthority("CUSTOMER_USER");
        existing.setFirstName("Existing");
        when(users.selectOne(any())).thenReturn(existing);

        SecurityUser result = resolve("tenant-1", null, "new-customer", "user@example.com", true, true);

        assertThat(result.id()).isEqualTo("existing-user");
        assertThat(result.customerId()).isEqualTo("existing-customer");
        assertThat(result.authority()).isEqualTo("CUSTOMER_USER");
        verify(users, never()).insertIfAbsent(any());
        verify(customers, never()).findOrCreate(any(), any());
    }

    private SecurityUser resolve(
            String tenantId,
            String mappedTenantId,
            String customerName,
            String email,
            boolean allowUserCreation,
            boolean activateUser) {
        return service.resolve(
                tenantId,
                mappedTenantId,
                null,
                null,
                email,
                "New",
                null,
                customerName,
                "Dex",
                allowUserCreation,
                activateUser);
    }

    private static TenantEntity tenant(String id, String name) {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(id);
        tenant.setName(name);
        return tenant;
    }

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(status);
    }
}
