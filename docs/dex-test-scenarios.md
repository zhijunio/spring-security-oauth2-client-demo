# Dex 用户与 OAuth2 映射测试场景

本文说明本地 Dex 测试账号如何映射到 demo 的用户、租户和客户。所有数据仅用于本地开发。

## 1. 两个访问 Domain 的分工

| 应用地址 | Domain | 登录按钮 | 用途 |
|---|---|---|---|
| `http://localhost:8080/` | `localhost:8080` | Dex、Dex · 仅允许已有用户 | 默认租户登录、验证账号复用和禁止自动创建 |
| `http://localhost:8081/` | `localhost:8081` | Dex · 域名租户 + 客户、Dex · 自定义租户名、Dex · 新用户保持未激活 | 验证 tenant/customer mapper 与新用户激活选项 |

这里的 **Domain 是用户访问 demo 的 host 与端口**，负责筛选登录页展示的 Client。`acme.example.com` 等邮箱后缀不是 demo Domain，而是 `DOMAIN` tenant mapping 从 IdP 邮箱 claim 中取出的租户名称。

两个应用实例共用 PostgreSQL，因此用户、租户和客户数据相同。浏览器 Cookie 不按端口隔离；为避免两个实例的内存 Session 相互覆盖，8081 使用单独的 Session Cookie 名。

## 2. 启动方式

先启动 PostgreSQL 和 Dex：

```bash
docker compose up -d postgres dex
```

正常启动应用到 8080：

```bash
./mvnw spring-boot:run
```

再在另一个终端启动 8081：

```bash
SERVER_PORT=8081 SERVER_SERVLET_SESSION_COOKIE_NAME=JSESSIONID_8081 ./mvnw spring-boot:run
```

Dex 使用静态配置文件 `dex/config.yaml`，不依赖环境变量。Dex 客户端登记了两个端口各自的回调 URI；Flyway 将基础 Client 关联到 8080，将映射 Client 关联到 8081。新增端口或更改回调时，需同时调整 Domain 关联与 Dex `redirectURIs`。

## 3. 测试账号

所有账号的密码都是 `dex-demo`。Dex 静态用户为不同场景提供 `email`、`email_verified`、`name` 和 `preferred_username` claims。Dex 的本地密码 Connector 支持这些静态用户字段，`profile` scope 会提供 profile claims。[Dex 本地 Connector 文档](https://dexidp.io/docs/connectors/local/)；[Dex claims 文档](https://dexidp.io/docs/configuration/custom-scopes-claims-clients/)

| Dex 登录邮箱 | 用户 profile name | `preferred_username` | 测试用途 |
|---|---|---|---|
| `dex.user@example.com` | `dex-user` | `default-user` | 默认 tenant-bound Client |
| `alice@acme.example.com` | `alice-acme` | `acme-ops` | `acme.example.com` 租户下的第一个客户 |
| `bob@acme.example.com` | `bob-acme` | `acme-sales` | 与 Alice 同租户、不同客户 |
| `carol@beta.example.net` | `carol-beta` | `beta-support` | 另一个邮箱域名和租户 |
| `dana@custom.example.org` | `dana-custom` | `custom-org` | 自定义 tenant name 模板 |
| `existing@local.example` | `existing-user` | `existing-user` | `allowUserCreation` 账号复用测试 |
| `inactive@local.example` | `inactive-user` | `inactive-user` | 新用户不激活测试 |

## 4. 场景 A：普通 Client 固定归属租户

访问 `http://localhost:8080/`，点击 **Dex**，使用 `dex.user@example.com` 登录。

预期：

- 如果本地不存在该邮箱，创建用户并归属 `Default tenant`。
- 此 Client 虽有默认 `DOMAIN` 策略，但 Client 本身属于普通租户，因此不会按邮箱域名改派租户。
- 没有客户映射时，新用户 authority 为 `TENANT_ADMIN`。

## 5. 场景 B：按邮箱域名映射租户、按用户名映射客户

访问 `http://localhost:8081/`，点击 **Dex · 域名租户 + 客户**。

先使用 Alice 登录。该 Client 属于系统租户，`tenantNameStrategy=DOMAIN`，取邮箱 `@` 后部分作为租户名称；`customerNamePattern=%{preferred_username}` 用 IdP 用户属性生成客户名称。

| 登录账号 | 预期租户 | 预期客户 | 新用户 authority |
|---|---|---|---|
| `alice@acme.example.com` | `acme.example.com` | `acme-ops` | `CUSTOMER_USER` |
| `bob@acme.example.com` | `acme.example.com` | `acme-sales` | `CUSTOMER_USER` |
| `carol@beta.example.net` | `beta.example.net` | `beta-support` | `CUSTOMER_USER` |

Alice 和 Bob 的邮箱域名相同，因此复用同一个租户；其 `preferred_username` 不同，因此创建不同客户。Carol 的邮箱后缀不同，会进入另一个租户。

## 6. 场景 C：自定义租户名称模板

访问 `http://localhost:8081/`，点击 **Dex · 自定义租户名**，使用 `dana@custom.example.org` 登录。

该 Client 使用 `tenantNameStrategy=CUSTOM`、`tenantNamePattern=tenant-%{preferred_username}`。mapper 将 `%{preferred_username}` 替换为 `custom-org`，首次登录创建名为 `tenant-custom-org` 的租户。没有 customer ID 或 customer name 时，新用户 authority 为 `TENANT_ADMIN`。

这个场景也说明：自定义租户模板取 IdP claims，不取访问 demo 的 `localhost:8081` Domain 名称。

## 7. 场景 D：`allowUserCreation` 关闭

1. 在 8080 点击 **Dex · 仅允许已有用户**，尝试用 `existing@local.example` 登录。首次应失败，因为本地还没有该用户，且 `allowUserCreation=false`。
2. 登出后点击 **Dex**，使用同一账号登录。本地用户会在 `Default tenant` 创建。
3. 再登出并通过 **Dex · 仅允许已有用户** 登录。现在用户已存在且属于该 Client 的租户，应登录成功。

若首次失败后仍有登录状态，先从应用登出，再开始下一步。

## 8. 场景 E：`activateUser` 只控制新用户凭据

访问 8081，点击 **Dex · 新用户保持未激活**，使用 `inactive@local.example` 登录。

预期：用户创建在 `Default tenant`，OAuth2 登录成功，但 `user_credentials.enabled=false`。该 Client 的 `activateUser=false` 只影响新建用户；再次通过 OAuth2 登录已有用户时不会修改凭据启用状态，也不会因本地密码凭据未启用而拒绝第三方登录。

## 9. 查看持久化结果

管理员登录 8080 后可查看用户与租户列表。也可以在本地 PostgreSQL 查询用户—租户—客户关系及凭据状态：

```sql
SELECT u.email, t.name AS tenant, c.title AS customer, u.authority, uc.enabled AS credentials_enabled
FROM users u
JOIN tenant t ON t.id = u.tenant_id
LEFT JOIN customer c ON c.id = u.customer_id
LEFT JOIN user_credentials uc ON uc.user_id = u.id
WHERE u.email IN (
  'dex.user@example.com',
  'alice@acme.example.com',
  'bob@acme.example.com',
  'carol@beta.example.net',
  'dana@custom.example.org',
  'existing@local.example',
  'inactive@local.example'
)
ORDER BY u.email;
```

租户和客户在第一次成功映射时才创建。已有邮箱再次登录时复用已有用户，不会因为另一个 Client 的映射结果而迁移用户或改写其客户归属。若要重复验证首次创建结果，请使用未登录过的 Dex 测试账号。
