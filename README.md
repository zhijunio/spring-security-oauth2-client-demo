# Spring Security OAuth2 Client Lab

[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot 4.1.2](https://img.shields.io/badge/Spring_Boot-4.1.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security 7](https://img.shields.io/badge/Spring_Security-7-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL 18](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven 3.10.0](https://img.shields.io/badge/Maven-3.10.0-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

[English](README.md) | [简体中文](README.zh-CN.md)

A Spring Boot sample for learning OAuth2 / OIDC social login. OAuth2 Clients and provider templates are persisted separately. Mapper types and their type-specific settings are embedded in Client and template records; there is no standalone mapper table. Templates provide defaults, while each created Client keeps its own runtime configuration. Spring Security builds `ClientRegistration` instances dynamically from enabled Clients stored in PostgreSQL.

## Tech stack

- Java 25, Spring Boot 4.1.2, Spring Security 7
- PostgreSQL, Flyway, MyBatis-Plus 3.5.17
- Spring MVC + Thymeleaf; browser sessions; platform JWTs are staged server-side (BFF)
- OAuth2 Authorization Code Flow; supports both OIDC Clients and plain OAuth2 Clients

## Run locally

```bash
cp .env.sample .env
cp .secrets.properties.sample .secrets.properties
```

Before initializing the database, generate a BCrypt password hash with a Spring Security-compatible `PasswordEncoder` and set it as `OAUTH2_ADMIN_PASSWORD_HASH` in `.secrets.properties`. Use the encoded format `{bcrypt}$2a$...`; do not put the plaintext password in this file. Dex is a local test identity provider; its client and test accounts are defined in `dex/config.yaml` and require no environment variables. The administrator entry is shown on the home page by default. On the first Flyway migration, the admin account is created with the pre-encoded password hash. The local Dex test password is `dex-demo`; accounts and client configuration are in `dex/config.yaml` and are for learning only. Flyway creates the administrator only when the hash is non-empty. The Dex test Client Secret is a random plaintext value shared by the Dex configuration and Flyway seed; it is intended only for local testing. OAuth2 Client Secrets and Custom mapper passwords are also stored in plaintext in this sample. Do not commit `.env` or `.secrets.properties`.

Start PostgreSQL and run the application:

```bash
docker compose up -d postgres dex
./mvnw spring-boot:run
```

The single Flyway baseline creates tenants, credentials, Domain-to-Client associations, OAuth2 Clients, and templates. It includes four mapper types: `BASIC`, `CUSTOM`, `GITHUB`, and `APPLE`. The template display field is `name`. Dex examples cover `dex-local`, tenant/customer mapping, custom tenant names, disabling user creation, and inactive new users; all buttons are for local testing only. The Dex Client Secret is stored in plaintext in the Flyway seed and must match `dex/config.yaml`. Other Client Secrets created in the admin UI are also stored in plaintext. The mapper selector offers the three general choices `BASIC`, `CUSTOM`, and `GITHUB`; the Apple template selects `APPLE` automatically. Open <http://localhost:8080/> and sign in as `admin@local.invalid` with the configured administrator password. See [Dex users and mapping scenarios](docs/dex-test-scenarios.md) for test accounts, the two Domains, Client groups, and expected mappings.

The default instance uses port 8080. To test the second Domain, start another instance in a separate terminal:

```bash
SERVER_PORT=8081 SERVER_SERVLET_SESSION_COOKIE_NAME=JSESSIONID_8081 ./mvnw spring-boot:run
```

Both instances share PostgreSQL. Initial data associates tenant/customer mapping Clients with `localhost:8081` and keeps the basic login Clients on `localhost:8080`.

Before creating a login Client, copy its generated callback URL from the UI and register it with the identity provider. Enter the OAuth Client ID and Secret, then select a template and mapper. Secrets are stored in plaintext; the detail API returns an empty `clientSecret`, and leaving the field blank during an edit preserves the stored value. Apple's Client Secret must be an Apple-signed client secret JWT. A login button appears on a Domain's home page only after the Client is enabled and associated with that Domain.

```text
http://localhost:8080/login/oauth2/code/{registrationId}
```

Default scopes are Basic OIDC `openid,profile,email`, Custom `openid,profile,email`, GitHub `read:user,user:email`, and Apple `email,openid,name`. The preconfigured Dex Client uses `openid,profile,email`. After login, the application finds the local account by mapped email. A Client owned by a regular tenant accepts only users in that tenant; a system-tenant Client can match existing users across the platform. Email is the account-linking key in this sample; it does not use provider `iss + sub` as the local user key. BASIC and APPLE check the standard `email_verified` claim; GitHub selects a verified primary email through its email API.

## Data model aligned with ThingsBoard

| Table / entity | Responsibility | Main fields |
|---|---|---|
| `tenant` / `TenantEntity` | Tenant directory associated with Clients | String ID, creation time, name, email, `additional_info`, version |
| `domain` / `DomainEntity` | Tenant login-domain configuration | String ID, tenant ID, domain name, OAuth2 switch, creation time |
| `domain_oauth2_client` / `DomainOauth2ClientEntity` | Many-to-many Domain and OAuth2 Client association | Composite key `domain_id`, `oauth2_client_id` |
| `oauth2_client_template` / `OAuth2ClientTemplateEntity` | OAuth2 Client configuration templates and defaults | name, protocol/issuer, authorization/token/user-info/JWK URLs, default scopes, username attribute, authentication method, button fields, help link, mapper type and type-specific `mapperConfig` |
| `oauth2_client` / `OAuth2ClientEntity` | Independently enabled runtime Client; copies endpoints and other settings from its template at creation | registration ID, tenant ID, Client ID, plaintext secret, `loginButtonIcon`, `loginButtonLabel`, allow/activate user, endpoints, scopes, mapper type and Basic/Custom settings, enabled state |

Mapper types and their settings live directly in template and runtime Client records, alongside a minimal tenant directory and Domain association table. The system tenant owns special Clients: only Clients assigned to the system tenant use mapping results to determine the tenant for newly created users. BASIC/GITHUB/APPLE support `EMAIL` (the full email), `DOMAIN` (the portion after `@`), and `CUSTOM` (expand `%{attribute}` from provider claims); the CUSTOM mapper can return `tenantId` or `tenantName` directly. A Client assigned to a regular tenant always uses that tenant and ignores the mapper's tenant fields. New users can be assigned a customer through the Custom mapper's `customerId` / `customerName` or Basic's `customerNamePattern`; a `customerId` must belong to the resolved tenant. `basic_default_dashboard_name` and `basic_always_full_screen` are outside this OAuth2 login sample's scope. Platform tenant roles and resource policies are not implemented.

`oauth2_client_template.protocol` describes the protocol and accepts only `OIDC` or `OAUTH2`; `mapperConfig.type` describes the identity mapper. The four types `BASIC`, `CUSTOM`, `GITHUB`, and `APPLE` have different configuration shapes: BASIC/GITHUB/APPLE use `mapperConfig.basic`, while CUSTOM uses `mapperConfig.custom` (URL, username, password, and whether to send the token). The UI lets users choose the three general mapper types `BASIC`, `CUSTOM`, and `GITHUB`. Apple is a preset-template-only type and is selected automatically. Dex uses `BASIC` and stores its OIDC endpoints on its runtime Client record; there is no Dex template type. Unrelated mapper configuration is not persisted.

The Flyway baseline seeds a system tenant with the fixed ID `00000000-0000-0000-0000-000000000000` for global Clients using tenant mapping. It cannot be a mapping target or the owner of a Domain. Tenant names are unique and are used to find or create tenants from mapping results. Persistence uses MyBatis-Plus `BaseMapper` and Lambda Wrappers. The rebuilt baseline is for an empty database; do not reuse a database that has the old multi-version Flyway history. Back up existing data and plan a separate migration before upgrading an old database. Client Secrets and Custom mapper passwords are stored in plaintext in this sample and are not returned by read APIs.

## Management API

All management endpoints below require the administrator session and CSRF protection, except for the public login Client list.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/oauth2/config/template` | List templates with default endpoints, scopes, mapper, and button fields |
| `POST` | `/api/oauth2/config/template` | Create or update a provider template; mapper settings go in the type-specific `mapperConfig` |
| `DELETE` | `/api/oauth2/config/template/{clientRegistrationTemplateId}` | Delete a template that is not referenced by a Client |
| `GET` | `/api/tenants` | List tenants |
| `GET` | `/api/tenants/{id}` | Get one tenant |
| `POST` | `/api/tenants` | Create a tenant with `name` and optional `email` and `additionalInfo` |
| `GET` | `/api/users` | List the most recently created users (up to 100) |
| `GET` | `/api/users/{id}` | Get one user |
| `POST` | `/api/domain?oauth2ClientIds={id1},{id2}` | Create or update a Domain and set its available OAuth2 Clients |
| `PUT` | `/api/domain/{id}/oauth2Clients` | Replace the OAuth2 Client IDs associated with a Domain |
| `GET` | `/api/domain/infos?pageSize=20&page=0` | Page through Domains and their Client associations |
| `GET` | `/api/domain/info/{id}` | Get one Domain configuration |
| `DELETE` | `/api/domain/{id}` | Delete a Domain and its Client associations |
| `GET` | `/api/oauth2/client/infos?pageSize=20&page=0` | Page through Clients; response includes `data`, `totalPages`, `totalElements`, and `hasNext` |
| `GET` | `/api/oauth2/client/infos?clientIds={id1},{id2}` | Get Client information by IDs |
| `GET` | `/api/oauth2/client/list?clientIds={id1},{id2}` | Get Client information by IDs (ThingsBoard-compatible path) |
| `GET` | `/api/oauth2/client/{id}` | Get one Client; `clientSecret` is empty and credentials are never returned |
| `POST` | `/api/oauth2/client` | Create a Client or update it by `id`; a blank Secret preserves the stored value |
| `DELETE` | `/api/oauth2/client/{id}` | Delete a Client |
| `GET` | `/api/oauth2/loginProcessingUrl` | Get the OAuth2 callback processing path |
| `POST` | `/api/noauth/oauth2Clients` | Public ThingsBoard-compatible response with `name`, `icon`, and `url` |
| `GET` | `/api/me` | Get the current account using the platform JWT |

The management API uses one administrator session. ThingsBoard also separates access by `SYS_ADMIN` / `TENANT_ADMIN` and tenant resource permissions; this sample protects the management domain with one administrator account. A Domain belongs to a regular tenant. Its associated Clients may belong to the same tenant or to the system tenant. Domain records contain a host or host:port, without a scheme or path. The public login list matches the request host exactly and returns only enabled Clients associated with an enabled Domain. If no Domain matches, no login options are shown. A reverse proxy can forward the external scheme and port through `X-Forwarded-Proto` / `X-Forwarded-Port`. `tenant_profile` is not modeled to keep the tenant table small. There is no separate mapper persistence entity or mapper REST API; Client and template APIs use nested `mapperConfig`.

OAuth2 APIs use the same paths and HTTP methods as ThingsBoard for `/api/oauth2/config/template`, `/api/oauth2/client/*`, `/api/oauth2/loginProcessingUrl`, `/api/noauth/oauth2Clients`, and Domain OAuth2 association endpoints. Pagination uses the same response envelope, and Client and Domain `infos` endpoints use matching query parameters. Client APIs use nested `mapperConfig` with `type`, `allowUserCreation`, `activateUser`, and `basic` / `custom` configuration. This sample adds `tenantId`, template association, and an enabled switch to run independently. `platforms` and `pkgName` are omitted. `allowUserCreation` controls JIT account creation; `activateUser` only controls whether credentials for a new user are enabled and does not change existing users' credentials. OAuth2 login success depends on external authentication and user mapping, not on whether local password credentials are enabled. Users are stored in `users` and matched uniquely by email. A regular-tenant Client is scoped to its tenant; a system-tenant Client can match existing users across the platform and applies tenant mapping to new users. User APIs provide list and detail reads; tenant APIs provide list, detail, and create operations. `basicCustomerNamePattern` finds or creates a local customer when it maps a customer name. Mapper types and settings live on Clients and templates rather than a standalone mapper table; APIs use nested `mapperConfig`. `templateId`, `protocol`, `issuerUri`, and `enabled` are extensions needed by this sample. `DomainOauth2ClientEntity` maps the composite Domain-to-Client association; `propagateToEdge` is not modeled because this sample has no Edge synchronization. Tenant-role authorization and mobile-platform filtering are outside the single-admin authorization model. The login Client list contains only enabled Clients associated with the enabled Domain matching the request host.

`UserEntity` maps the `users` table and keeps tenant, customer, username, email, authority, first name, last name, additional info, creation time, and version. Credential state and BCrypt password hashes are stored separately in `user_credentials`, linked by `user_id`. Username and email can both be used for local login; OAuth2 users are still created from the mapped email. Registration, password changes, and MFA are not implemented. Example Client creation request (the default tenant ID is seeded by the migration):

```json
{
  "id": "d49d6f3e-07a0-4fbe-97ea-616b155b62cc",
  "name": "Company Google",
  "tenantId": "22222222-2222-4222-8222-222222222222",
  "templateId": "basic",
  "clientId": "provider-client-id",
  "clientSecret": "provider-client-secret",
  "scope": ["openid", "profile", "email"],
  "mapperConfig": {
    "type": "BASIC",
    "allowUserCreation": true,
    "activateUser": true,
    "basic": {
      "emailAttributeKey": "email",
      "firstNameAttributeKey": "given_name",
      "lastNameAttributeKey": "family_name",
      "tenantNameStrategy": "DOMAIN"
    }
  },
  "enabled": true
}
```

Example template configuration (CUSTOM type):

```json
{
  "id": "custom",
  "name": "Custom OAuth2 / OIDC",
  "protocol": "OIDC",
  "issuerUri": "https://identity.example.com",
  "authorizationUri": "https://identity.example.com/oauth2/authorize",
  "accessTokenUri": "https://identity.example.com/oauth2/token",
  "userInfoUri": "https://identity.example.com/userinfo",
  "jwkSetUri": "https://identity.example.com/.well-known/jwks.json",
  "scope": ["openid", "profile", "email"],
  "mapperConfig": {
    "type": "CUSTOM",
    "allowUserCreation": true,
    "activateUser": true,
    "custom": {
      "url": "https://identity.example.com/oauth2/user",
      "sendToken": false
    }
  }
}
```

The Custom mapper calls the configured HTTPS user-attribute endpoint. Optional username and password are stored in plaintext and are not returned when reading templates. A Client inherits the template's Basic or Custom configuration when created; Client requests can override non-secret fields and provide a new password.

If supplied, `id` must be a UUID. It is also used as Spring Security's `registrationId`, so it must match the callback URI registered with the identity provider. If omitted, the server generates a UUID. APIs and the admin UI never return Secrets.

## Login and platform API flow

```mermaid
sequenceDiagram
    autonumber
    actor User as User
    participant Browser as Browser
    participant App as Sample app
    participant DB as PostgreSQL
    participant IdP as Identity provider
    participant API as Platform API

    User->>Browser: Select an enabled login option
    Browser->>App: /oauth2/authorization/{registrationId}
    App->>DB: Load Client and template snapshot by registrationId
    DB-->>App: ClientRegistration settings and client Secret
    App->>IdP: Authorization request (state; nonce for OIDC)
    IdP-->>Browser: Redirect with code and state
    Browser->>App: /login/oauth2/code/{registrationId}
    App->>IdP: Exchange code and validate identity server-side
    IdP-->>App: OIDC claims or OAuth2 user attributes
    App->>App: Select mapper by client.mapperConfig.type
    App->>App: Find user by email and validate tenantId
    App->>App: Sign short-lived platform JWT and store it in server session
    App-->>Browser: Establish local session
    Browser->>App: Request a platform API demo token
    App-->>Browser: Return short-lived JWT from session
    Browser->>API: Authorization: Bearer platform JWT
    API-->>Browser: Validate signature, issuer, audience, and expiry
```

## Code entry points

| Responsibility | Code |
|---|---|
| Dynamic registration repository | `oauth2/DatabaseClientRegistrationRepository.java` |
| Client, template, and mapper management | `oauth2/OAuth2ClientService.java`, `web/OAuth2ClientController.java` |
| Provider user-attribute mapping | `oauth2/ExternalUserResolver.java`, `oauth2/strategy/OAuth2ClientStrategyProvider.java`, `oauth2/strategy/*OAuth2ClientStrategy.java` |
| Establish local identity after OAuth success and issue access token | `security/SecurityUser.java`, `security/Oauth2AuthenticationSuccessHandler.java`, `token/JwtTokenFactory.java` |
| Admin, session, and API security | `security/SecurityConfiguration.java` |
| MyBatis-Plus mappers and entities | `persistence/mapper/`, `persistence/entity/` |
| Database schema and template seed | `src/main/resources/db/migration/` |

## Sample boundaries

- OAuth2 Clients, templates, mappers, local admin, and users created through OAuth2 are persisted in PostgreSQL. External accounts are linked by email; there is no provider subject mapping table.
- The HS256 key for platform JWTs is generated randomly at startup, so restarting invalidates old tokens. A real system should use stable key management and rotation.
- The first administrator is initialized by Flyway with the username `admin`; the migration reads a pre-encoded password from `OAUTH2_ADMIN_PASSWORD_HASH`. An empty value skips administrator creation; existing accounts are not overwritten. This sample does not manage administrator lifecycle or multiple admin roles.
- The sample listens on local HTTP only. Before deployment, configure HTTPS, secure cookies, trusted proxy boundaries, key management, login auditing, rate limiting, and account lifecycle handling.
- Google/GitHub templates demonstrate common OIDC / OAuth2 differences. Platform-wide tenant authorization, customer lifecycle/assignment, Apple client-secret JWT generation, and provider session logout are not implemented. The Custom mapper uses this sample's HTTPS JSON endpoint contract and does not implement the full ThingsBoard account/customer lifecycle.

## Verification

```bash
./mvnw spotless:check
./mvnw compiler:compile
```

## License

This project is licensed under the Apache License 2.0. You may use, modify, and distribute it under the terms in [`LICENSE`](LICENSE). When redistributing it, include a copy of the license and retain applicable copyright and attribution notices. The license includes an express patent grant and is provided without warranty.
