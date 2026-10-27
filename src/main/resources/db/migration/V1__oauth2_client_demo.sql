-- Clean baseline for a fresh OAuth2 client demo database.

CREATE TABLE tenant (
    id varchar(36) PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    email varchar(255),
    name varchar(255) NOT NULL,
    version bigint DEFAULT 1
);
CREATE UNIQUE INDEX uk_tenant_name ON tenant (name);

CREATE TABLE customer (
    id varchar(36) PRIMARY KEY,
    tenant_id varchar(36) NOT NULL REFERENCES tenant(id),
    title varchar(255) NOT NULL,
    created_time bigint NOT NULL,
    version bigint DEFAULT 1 NOT NULL,
    CONSTRAINT uk_customer_tenant_title UNIQUE (tenant_id, title)
);

CREATE TABLE domain (
    id varchar(36) PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id varchar(36) NOT NULL REFERENCES tenant(id),
    name varchar(255) NOT NULL UNIQUE,
    oauth2_enabled boolean NOT NULL DEFAULT false
);

CREATE TABLE oauth2_client_template (
    id varchar(80) PRIMARY KEY,
    name varchar(120) NOT NULL,
    protocol varchar(20) NOT NULL CHECK (protocol IN ('OIDC', 'OAUTH2')),
    issuer_uri varchar(500),
    authorization_uri varchar(500) NOT NULL,
    token_uri varchar(500) NOT NULL,
    user_info_uri varchar(500),
    jwk_set_uri varchar(500),
    user_name_attribute varchar(120) NOT NULL,
    default_scopes varchar(500) NOT NULL,
    client_authentication_method varchar(20) NOT NULL DEFAULT 'BASIC'
        CHECK (client_authentication_method IN ('BASIC', 'POST')),
    comment varchar(1000),
    login_button_icon varchar(1000),
    login_button_label varchar(120) NOT NULL,
    help_link varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    type varchar(31),
    basic_email_attribute_key varchar(120),
    basic_first_name_attribute_key varchar(120),
    basic_last_name_attribute_key varchar(120),
    basic_tenant_name_strategy varchar(31),
    basic_tenant_name_pattern varchar(1000),
    basic_customer_name_pattern varchar(1000),
    custom_url varchar(1000),
    custom_username varchar(255),
    custom_password varchar(2000),
    custom_send_token boolean NOT NULL DEFAULT false,
    allow_user_creation boolean NOT NULL DEFAULT true,
    activate_user boolean NOT NULL DEFAULT true
);

CREATE TABLE oauth2_client (
    id varchar(80) PRIMARY KEY,
    name varchar(120) NOT NULL,
    template_id varchar(80) NOT NULL REFERENCES oauth2_client_template(id),
    client_id varchar(500) NOT NULL,
    client_secret varchar(4096) NOT NULL,
    scopes varchar(500) NOT NULL,
    issuer_uri varchar(500),
    authorization_uri varchar(500) NOT NULL,
    token_uri varchar(500) NOT NULL,
    user_info_uri varchar(500),
    jwk_set_uri varchar(500),
    user_name_attribute varchar(120) NOT NULL,
    client_authentication_method varchar(20) NOT NULL
        CHECK (client_authentication_method IN ('BASIC', 'POST')),
    login_button_label varchar(120) NOT NULL,
    login_button_icon varchar(1000),
    enabled boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    tenant_id varchar(36) NOT NULL REFERENCES tenant(id),
    allow_user_creation boolean NOT NULL DEFAULT true,
    activate_user boolean NOT NULL DEFAULT true,
    type varchar(31),
    basic_email_attribute_key varchar(120),
    basic_first_name_attribute_key varchar(120),
    basic_last_name_attribute_key varchar(120),
    basic_tenant_name_strategy varchar(31),
    basic_tenant_name_pattern varchar(1000),
    basic_customer_name_pattern varchar(1000),
    custom_url varchar(1000),
    custom_username varchar(255),
    custom_password varchar(2000),
    custom_send_token boolean NOT NULL DEFAULT false
);
CREATE INDEX idx_oauth2_client_enabled ON oauth2_client (enabled);

CREATE TABLE domain_oauth2_client (
    domain_id varchar(36) NOT NULL REFERENCES domain(id) ON DELETE CASCADE,
    oauth2_client_id varchar(80) NOT NULL REFERENCES oauth2_client(id) ON DELETE CASCADE,
    PRIMARY KEY (domain_id, oauth2_client_id)
);

CREATE TABLE users (
    id varchar(36) PRIMARY KEY,
    tenant_id varchar(36) NOT NULL REFERENCES tenant(id),
    customer_id varchar(36) REFERENCES customer(id),
    username varchar(255),
    email varchar(255) NOT NULL UNIQUE,
    authority varchar(31) NOT NULL,
    first_name varchar(255),
    last_name varchar(255),
    additional_info text,
    created_time bigint NOT NULL,
    version bigint NOT NULL DEFAULT 1
);
CREATE INDEX idx_users_created_time ON users (created_time DESC);
CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE UNIQUE INDEX uk_users_username ON users (username) WHERE username IS NOT NULL;

CREATE TABLE user_credentials (
    id varchar(36) PRIMARY KEY,
    user_id varchar(36) NOT NULL UNIQUE REFERENCES users(id),
    enabled boolean NOT NULL,
    created_time bigint NOT NULL,
    password_hash varchar(255)
);

-- Demo tenants and login domains.
INSERT INTO tenant (id, created_time, name, version) VALUES
    ('22222222-2222-4222-8222-222222222222', 0, 'Default tenant', 1),
    ('00000000-0000-0000-0000-000000000000', 0, 'System tenant (tenant mapping)', 1);

INSERT INTO domain (id, created_time, tenant_id, name, oauth2_enabled) VALUES
    ('11111111-1111-4111-8111-111111111111', 0, '22222222-2222-4222-8222-222222222222', 'localhost:8080', true),
    ('55555555-5555-4555-8555-555555555555', 0, '22222222-2222-4222-8222-222222222222', 'localhost:8081', true);

-- Four built-in template kinds. Dex uses BASIC as a configured OIDC client.
INSERT INTO oauth2_client_template (
    id, name, protocol, issuer_uri, authorization_uri, token_uri, user_info_uri, jwk_set_uri,
    user_name_attribute, default_scopes, client_authentication_method, comment,
    login_button_icon, login_button_label, help_link, type,
    basic_email_attribute_key, basic_first_name_attribute_key, basic_last_name_attribute_key,
    basic_tenant_name_strategy, custom_url, custom_send_token, allow_user_creation, activate_user
) VALUES
    ('basic', 'Google OpenID Connect', 'OIDC', 'https://accounts.google.com',
     'https://accounts.google.com/o/oauth2/v2/auth', 'https://oauth2.googleapis.com/token',
     'https://openidconnect.googleapis.com/v1/userinfo', 'https://www.googleapis.com/oauth2/v3/certs',
     'sub', 'openid,profile,email', 'BASIC', '预置 OIDC 属性映射模板，可作为通用 OpenID Connect 配置参考。',
     'google-logo', 'Google', 'https://developers.google.com/identity/openid-connect/openid-connect', 'BASIC',
     'email', 'given_name', 'family_name', 'DOMAIN', NULL, false, true, true),
    ('custom', 'Custom OAuth2 / OIDC', 'OIDC', 'https://identity.example.com',
     'https://identity.example.com/oauth2/authorize', 'https://identity.example.com/oauth2/token',
     'https://identity.example.com/userinfo', 'https://identity.example.com/.well-known/jwks.json',
     'sub', 'openid,profile,email', 'BASIC', '自定义用户属性映射示例；创建 Client 前请替换身份提供方和映射服务地址。',
     'custom-logo', 'Continue with SSO', 'https://openid.net/specs/openid-connect-core-1_0.html', 'CUSTOM',
     NULL, NULL, NULL, NULL, 'https://identity.example.com/oauth2/user', false, true, true),
    ('github', 'GitHub OAuth 2.0', 'OAUTH2', NULL,
     'https://github.com/login/oauth/authorize', 'https://github.com/login/oauth/access_token',
     'https://api.github.com/user', NULL, 'id', 'read:user,user:email', 'POST', 'GitHub 登录会额外读取已验证邮箱。',
     'github-logo', 'GitHub', 'https://docs.github.com/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps', 'GITHUB',
     'email', 'name', NULL, 'DOMAIN', NULL, false, true, true),
    ('apple', 'Apple Sign in with Apple', 'OIDC', 'https://appleid.apple.com',
     'https://appleid.apple.com/auth/authorize?response_mode=form_post', 'https://appleid.apple.com/auth/token',
     NULL, 'https://appleid.apple.com/auth/keys', 'email', 'email,openid,name', 'POST',
     'Apple 模板会自动使用 Apple 专用属性处理逻辑。', 'apple-logo', 'Apple',
     'https://developer.apple.com/sign-in-with-apple/get-started/', 'APPLE',
     'email', 'firstName', 'lastName', 'DOMAIN', NULL, false, true, true);

-- All Dex examples reuse the local Dex static client, with their own callback registration IDs.
INSERT INTO oauth2_client (
    id, name, template_id, client_id, client_secret, scopes, issuer_uri, authorization_uri, token_uri,
    user_info_uri, jwk_set_uri, user_name_attribute, client_authentication_method, login_button_label,
    login_button_icon, enabled, tenant_id, allow_user_creation, activate_user, type,
    basic_email_attribute_key, basic_first_name_attribute_key, basic_last_name_attribute_key,
    basic_tenant_name_strategy, basic_tenant_name_pattern, basic_customer_name_pattern
) VALUES
    ('dex-local', 'Local Dex', 'basic', 'oauth2-client-lab',
     'e9210f799f88f364acc21d88c0533e538a4117f9e2ed0a26ecce42bb976ad614', 'openid,profile,email',
     'http://localhost:5556', 'http://localhost:5556/auth', 'http://localhost:5556/token',
     'http://localhost:5556/userinfo', 'http://localhost:5556/keys', 'sub', 'BASIC', 'Dex', 'dex-logo',
     true, '22222222-2222-4222-8222-222222222222', true, true, 'BASIC', 'email', 'preferred_username', 'name', 'DOMAIN', NULL, NULL),
    ('dex-domain-mapping', 'Dex Domain + Customer Mapping', 'basic', 'oauth2-client-lab',
     'e9210f799f88f364acc21d88c0533e538a4117f9e2ed0a26ecce42bb976ad614', 'openid,profile,email',
     'http://localhost:5556', 'http://localhost:5556/auth', 'http://localhost:5556/token',
     'http://localhost:5556/userinfo', 'http://localhost:5556/keys', 'sub', 'BASIC', 'Dex · 域名租户 + 客户', 'dex-logo',
     true, '00000000-0000-0000-0000-000000000000', true, true, 'BASIC', 'email', 'name', NULL, 'DOMAIN', NULL, '%{preferred_username}'),
    ('dex-custom-tenant', 'Dex Custom Tenant Mapping', 'basic', 'oauth2-client-lab', 'e9210f799f88f364acc21d88c0533e538a4117f9e2ed0a26ecce42bb976ad614',
     'openid,profile,email', 'http://localhost:5556', 'http://localhost:5556/auth', 'http://localhost:5556/token',
     'http://localhost:5556/userinfo', 'http://localhost:5556/keys', 'sub', 'BASIC', 'Dex · 自定义租户名', 'dex-logo',
     true, '00000000-0000-0000-0000-000000000000', true, true, 'BASIC', 'email', 'name', NULL, 'CUSTOM', 'tenant-%{preferred_username}', NULL),
    ('dex-inactive-user', 'Dex Inactive New User', 'basic', 'oauth2-client-lab', 'e9210f799f88f364acc21d88c0533e538a4117f9e2ed0a26ecce42bb976ad614',
     'openid,profile,email', 'http://localhost:5556', 'http://localhost:5556/auth', 'http://localhost:5556/token',
     'http://localhost:5556/userinfo', 'http://localhost:5556/keys', 'sub', 'BASIC', 'Dex · 新用户保持未激活', 'dex-logo',
     true, '22222222-2222-4222-8222-222222222222', true, false, 'BASIC', 'email', 'name', NULL, NULL, NULL, NULL),
    ('dex-existing-only', 'Dex Existing Users Only', 'basic', 'oauth2-client-lab', 'e9210f799f88f364acc21d88c0533e538a4117f9e2ed0a26ecce42bb976ad614',
     'openid,profile,email', 'http://localhost:5556', 'http://localhost:5556/auth', 'http://localhost:5556/token',
     'http://localhost:5556/userinfo', 'http://localhost:5556/keys', 'sub', 'BASIC', 'Dex · 仅允许已有用户', 'dex-logo',
     true, '22222222-2222-4222-8222-222222222222', false, true, 'BASIC', 'email', 'name', NULL, NULL, NULL, NULL);

INSERT INTO domain_oauth2_client (domain_id, oauth2_client_id) VALUES
    ('11111111-1111-4111-8111-111111111111', 'dex-local'),
    ('11111111-1111-4111-8111-111111111111', 'dex-existing-only'),
    ('55555555-5555-4555-8555-555555555555', 'dex-domain-mapping'),
    ('55555555-5555-4555-8555-555555555555', 'dex-custom-tenant'),
    ('55555555-5555-4555-8555-555555555555', 'dex-inactive-user');

-- Configure the local administrator only when a precomputed BCrypt hash is supplied.
INSERT INTO users (id, tenant_id, email, username, authority, first_name, created_time, version)
SELECT '33333333-3333-4333-8333-333333333333', '22222222-2222-4222-8222-222222222222',
       'admin@local.invalid', 'admin@local.invalid', 'SYS_ADMIN', 'System Admin', 0, 1
WHERE '${admin_password_hash}' <> '';

INSERT INTO user_credentials (id, user_id, enabled, created_time, password_hash)
SELECT '33333333-3333-4333-8333-333333333334', '33333333-3333-4333-8333-333333333333', true, 0,
       '${admin_password_hash}'
WHERE '${admin_password_hash}' <> '';
