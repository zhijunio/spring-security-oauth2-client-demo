package io.github.zhijunio.oauth2client.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.zhijunio.oauth2client.persistence.entity.UserCredentialsEntity;
import io.github.zhijunio.oauth2client.persistence.entity.UserEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.UserCredentialsMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.UserMapper;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserCredentialsMapper credentials;
    private final UserMapper users;

    public SecurityUserDetailsService(UserCredentialsMapper credentials, UserMapper users) {
        this.credentials = credentials;
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        UserEntity account =
                users.selectOne(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, normalizedUsername));
        if (account == null && normalizedUsername.contains("@")) {
            account = users.selectOne(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, normalizedUsername));
        }
        if (account == null) throw new UsernameNotFoundException("User not found");

        UserCredentialsEntity credential = credentials.selectOne(
                Wrappers.<UserCredentialsEntity>lambdaQuery().eq(UserCredentialsEntity::getUserId, account.getId()));
        if (credential == null
                || credential.getPasswordHash() == null
                || credential.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }

        String role = "SYS_ADMIN".equals(account.getAuthority()) ? "ADMIN" : "USER";
        String principalName = account.getUsername() == null ? account.getEmail() : account.getUsername();
        return User.withUsername(principalName)
                .password(credential.getPasswordHash())
                .disabled(!Boolean.TRUE.equals(credential.getEnabled()))
                .roles(role)
                .build();
    }
}
