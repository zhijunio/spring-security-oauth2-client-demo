package io.github.zhijunio.oauth2client.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.zhijunio.oauth2client.persistence.entity.UserCredentialsEntity;
import io.github.zhijunio.oauth2client.persistence.entity.UserEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.UserCredentialsMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class SecurityUserDetailsServiceTest {

    @Mock
    private UserCredentialsMapper credentials;

    @Mock
    private UserMapper users;

    @InjectMocks
    private SecurityUserDetailsService service;

    @Test
    void mapsSystemAdminToSpringAdminRole() {
        UserEntity account = account("SYS_ADMIN");
        when(users.selectOne(any())).thenReturn(account);
        when(credentials.selectOne(any())).thenReturn(credential(true));

        UserDetails user = service.loadUserByUsername(" ADMIN@LOCAL.INVALID ");

        assertThat(user.getUsername()).isEqualTo("admin@local.invalid");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void doesNotGrantAdminRoleToTenantAuthorities() {
        when(users.selectOne(any())).thenReturn(account("TENANT_ADMIN"));
        when(credentials.selectOne(any())).thenReturn(credential(true));

        UserDetails user = service.loadUserByUsername("user@example.com");

        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void rejectsAccountsWithoutEnabledCredentials() {
        when(users.selectOne(any())).thenReturn(account("SYS_ADMIN"));
        when(credentials.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.loadUserByUsername("admin@local.invalid"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void disablesLocalLoginWhenCredentialsAreInactive() {
        when(users.selectOne(any())).thenReturn(account("SYS_ADMIN"));
        when(credentials.selectOne(any())).thenReturn(credential(false));

        UserDetails user = service.loadUserByUsername("admin@local.invalid");

        assertThat(user.isEnabled()).isFalse();
    }

    private static UserEntity account(String authority) {
        UserEntity account = new UserEntity();
        account.setId("user-id");
        account.setUsername("admin@local.invalid");
        account.setEmail("admin@local.invalid");
        account.setAuthority(authority);
        return account;
    }

    private static UserCredentialsEntity credential(boolean enabled) {
        UserCredentialsEntity credential = new UserCredentialsEntity();
        credential.setUserId("user-id");
        credential.setEnabled(enabled);
        credential.setPasswordHash("{bcrypt}$2a$10$example");
        return credential;
    }
}
