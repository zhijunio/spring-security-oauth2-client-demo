package io.github.zhijunio.oauth2client.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Order(1)
    SecurityFilterChain bearerApi(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/me")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain admin(HttpSecurity http) throws Exception {
        return http.securityMatcher(
                        "/admin/**",
                        "/api/oauth2/**",
                        "/api/domain",
                        "/api/domain/**",
                        "/api/users",
                        "/api/users/**",
                        "/api/tenants",
                        "/api/tenants/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login")
                        .permitAll()
                        .anyRequest()
                        .hasRole("ADMIN"))
                .formLogin(form -> form.loginPage("/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error"))
                .logout(logout -> logout.logoutUrl("/admin/logout").logoutSuccessUrl("/login?signedOut"))
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain browser(HttpSecurity http, Oauth2AuthenticationSuccessHandler successHandler) throws Exception {
        return http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/noauth/**", "/login/oauth2/code/*"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/assets/**",
                                "/error",
                                "/api/noauth/**",
                                "/oauth2/**",
                                "/login/oauth2/code/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/session/platform-token")
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(
                        exception -> exception.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .oauth2Login(login -> login.successHandler(successHandler).failureUrl("/login?error"))
                .logout(logout -> logout.logoutSuccessUrl("/login?signedOut")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .build();
    }
}
