package io.github.zhijunio.oauth2client.oauth2;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.zhijunio.oauth2client.api.PageData;
import io.github.zhijunio.oauth2client.persistence.entity.DomainEntity;
import io.github.zhijunio.oauth2client.persistence.entity.DomainOauth2ClientEntity;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientEntity;
import io.github.zhijunio.oauth2client.persistence.entity.OAuth2ClientTemplateEntity;
import io.github.zhijunio.oauth2client.persistence.mapper.DomainMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.DomainOauth2ClientMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientDataMapper;
import io.github.zhijunio.oauth2client.persistence.mapper.OAuth2ClientTemplateMapper;
import io.github.zhijunio.oauth2client.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DomainService {

    private final DomainMapper domains;
    private final DomainOauth2ClientMapper links;
    private final OAuth2ClientDataMapper clients;
    private final OAuth2ClientTemplateMapper templates;
    private final TenantService tenants;
    private final Clock clock;

    public DomainService(
            DomainMapper domains,
            DomainOauth2ClientMapper links,
            OAuth2ClientDataMapper clients,
            OAuth2ClientTemplateMapper templates,
            TenantService tenants,
            Clock clock) {
        this.domains = domains;
        this.links = links;
        this.clients = clients;
        this.templates = templates;
        this.tenants = tenants;
        this.clock = clock;
    }

    public List<DomainInfo> list() {
        List<DomainEntity> domainList =
                domains.selectList(Wrappers.<DomainEntity>lambdaQuery().orderByAsc(DomainEntity::getName));
        if (domainList.isEmpty()) return List.of();
        List<String> domainIds = domainList.stream().map(DomainEntity::getId).toList();
        Map<String, List<DomainOauth2ClientEntity>> linksByDomain = links
                .selectList(Wrappers.<DomainOauth2ClientEntity>lambdaQuery()
                        .in(DomainOauth2ClientEntity::getDomainId, domainIds))
                .stream()
                .collect(Collectors.groupingBy(DomainOauth2ClientEntity::getDomainId));
        return domainList.stream()
                .map(domain -> info(domain, linksByDomain.getOrDefault(domain.getId(), List.of())))
                .toList();
    }

    public PageData<DomainInfo> page(int pageSize, int page, String textSearch, String sortProperty, String sortOrder) {
        List<DomainInfo> all = list();
        if (textSearch != null && !textSearch.isBlank()) {
            String query = textSearch.trim().toLowerCase(Locale.ROOT);
            all = all.stream()
                    .filter(domain -> domain.name().toLowerCase(Locale.ROOT).contains(query))
                    .toList();
        }
        if (sortProperty != null && !sortProperty.isBlank()) {
            java.util.Comparator<DomainInfo> comparator = switch (sortProperty) {
                case "name" -> java.util.Comparator.comparing(DomainInfo::name, String.CASE_INSENSITIVE_ORDER);
                case "createdTime" -> java.util.Comparator.comparingLong(DomainInfo::createdTime);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sortProperty");
            };
            all = all.stream()
                    .sorted("DESC".equalsIgnoreCase(sortOrder) ? comparator.reversed() : comparator)
                    .toList();
        }
        return PageData.from(all, pageSize, page);
    }

    public DomainInfo get(String id) {
        DomainEntity domain = require(id);
        return info(
                domain,
                links.selectList(Wrappers.<DomainOauth2ClientEntity>lambdaQuery()
                        .eq(DomainOauth2ClientEntity::getDomainId, domain.getId())));
    }

    @Transactional
    public DomainInfo save(DomainRequest input, List<String> clientIds) {
        String id = input.id() == null || input.id().isBlank()
                ? UUID.randomUUID().toString()
                : input.id().trim();
        DomainEntity domain = domains.selectById(id);
        boolean create = domain == null;
        if (create) {
            domain = new DomainEntity();
            domain.setId(id);
            domain.setCreatedTime(clock.millis());
        }
        String tenantId = input.tenantId().trim();
        tenants.require(tenantId);
        if (TenantService.isSystemTenant(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A domain must belong to a concrete tenant");
        }
        String name = canonicalDomain(input.name());
        long duplicates = domains.selectCount(Wrappers.<DomainEntity>lambdaQuery()
                .eq(DomainEntity::getName, name)
                .ne(DomainEntity::getId, id));
        if (duplicates > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Domain name already exists");
        }
        domain.setTenantId(tenantId);
        domain.setName(name);
        domain.setOauth2Enabled(Boolean.TRUE.equals(input.oauth2Enabled()));
        if (create) domains.insert(domain);
        else domains.updateById(domain);
        if (clientIds != null) replaceClients(domain, clientIds);
        return info(
                domain,
                links.selectList(Wrappers.<DomainOauth2ClientEntity>lambdaQuery()
                        .eq(DomainOauth2ClientEntity::getDomainId, domain.getId())));
    }

    @Transactional
    public void updateClients(String id, List<String> clientIds) {
        replaceClients(require(id), clientIds);
    }

    @Transactional
    public void delete(String id) {
        if (domains.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found");
        }
    }

    public List<String> enabledClientIdsForDomain(String requestDomain) {
        String name;
        try {
            name = canonicalDomain(requestDomain);
        } catch (ResponseStatusException exception) {
            return List.of();
        }
        DomainEntity domain = domains.selectOne(Wrappers.<DomainEntity>lambdaQuery()
                .eq(DomainEntity::getName, name)
                .eq(DomainEntity::getOauth2Enabled, true));
        if (domain == null) return List.of();
        return links
                .selectList(Wrappers.<DomainOauth2ClientEntity>lambdaQuery()
                        .eq(DomainOauth2ClientEntity::getDomainId, domain.getId()))
                .stream()
                .map(DomainOauth2ClientEntity::getOauth2ClientId)
                .distinct()
                .toList();
    }

    public static String requestDomain(HttpServletRequest request) {
        String forwardedProto = request.getHeader("x-forwarded-proto");
        String scheme = forwardedProto;
        if (scheme != null && scheme.contains(",")) scheme = scheme.substring(0, scheme.indexOf(','));
        if (scheme == null || scheme.isBlank()) scheme = request.getScheme();
        scheme = scheme.trim().toLowerCase(Locale.ROOT);
        int port = request.getServerPort();
        String forwardedPort = request.getHeader("x-forwarded-port");
        if (forwardedPort != null && !forwardedPort.isBlank()) {
            try {
                int candidate = Integer.parseInt(forwardedPort.split(",", 2)[0].trim());
                if (candidate > 0 && candidate <= 65535) port = candidate;
            } catch (NumberFormatException ignored) {
                // Use the servlet container's port when the proxy header is malformed.
            }
        } else if (forwardedProto != null && !forwardedProto.isBlank()) {
            if ("http".equals(scheme)) port = 80;
            else if ("https".equals(scheme)) port = 443;
        }
        String host = request.getServerName();
        if (port != 80 && port != 443) host += ":" + port;
        return canonicalDomain(host);
    }

    private void replaceClients(DomainEntity domain, List<String> clientIds) {
        List<String> normalizedIds = clientIds == null
                ? List.of()
                : clientIds.stream()
                        .filter(id -> id != null && !id.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();
        Map<String, OAuth2ClientEntity> clientsById = normalizedIds.isEmpty()
                ? Map.of()
                : clients.selectBatchIds(normalizedIds).stream()
                        .collect(Collectors.toMap(OAuth2ClientEntity::getId, client -> client));
        for (String clientId : normalizedIds) {
            OAuth2ClientEntity client = clientsById.get(clientId);
            if (client == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown OAuth2 client");
            if (!domain.getTenantId().equals(client.getTenantId())
                    && !TenantService.isSystemTenant(client.getTenantId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Domain and OAuth2 client must share a tenant");
            }
        }
        links.delete(Wrappers.<DomainOauth2ClientEntity>lambdaQuery()
                .eq(DomainOauth2ClientEntity::getDomainId, domain.getId()));
        normalizedIds.forEach(clientId -> {
            DomainOauth2ClientEntity link = new DomainOauth2ClientEntity();
            link.setDomainId(domain.getId());
            link.setOauth2ClientId(clientId);
            links.insert(link);
        });
    }

    private DomainEntity require(String id) {
        DomainEntity domain = domains.selectById(id);
        if (domain == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found");
        return domain;
    }

    private DomainInfo info(DomainEntity domain, List<DomainOauth2ClientEntity> domainLinks) {
        List<String> clientIds = domainLinks.stream()
                .map(DomainOauth2ClientEntity::getOauth2ClientId)
                .toList();
        List<OAuth2ClientEntity> domainClients = clientIds.isEmpty() ? List.of() : clients.selectBatchIds(clientIds);
        Map<String, OAuth2ClientEntity> clientsById =
                domainClients.stream().collect(Collectors.toMap(OAuth2ClientEntity::getId, client -> client));
        List<String> templateIds = domainClients.stream()
                .map(OAuth2ClientEntity::getTemplateId)
                .distinct()
                .toList();
        Map<String, OAuth2ClientTemplateEntity> templatesById = templateIds.isEmpty()
                ? Map.of()
                : templates.selectBatchIds(templateIds).stream()
                        .collect(Collectors.toMap(OAuth2ClientTemplateEntity::getId, template -> template));
        List<DomainOAuth2ClientInfo> clientInfos = clientIds.stream()
                .map(clientsById::get)
                .filter(java.util.Objects::nonNull)
                .map(client -> {
                    OAuth2ClientTemplateEntity template = templatesById.get(client.getTemplateId());
                    return new DomainOAuth2ClientInfo(
                            client.getId(),
                            client.getCreatedAt() == null
                                    ? 0
                                    : client.getCreatedAt().toEpochMilli(),
                            client.getName(),
                            template == null ? null : template.getName());
                })
                .sorted(java.util.Comparator.comparing(DomainOAuth2ClientInfo::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new DomainInfo(
                domain.getId(),
                domain.getCreatedTime(),
                domain.getTenantId(),
                domain.getName(),
                Boolean.TRUE.equals(domain.getOauth2Enabled()),
                clientInfos);
    }

    private static String canonicalDomain(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Domain name is required");
        }
        String candidate = value.trim().toLowerCase(Locale.ROOT);
        if (candidate.contains("/") || candidate.contains("@") || candidate.contains(" ")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Domain name must be a host, optionally with a port");
        }
        try {
            URI uri = URI.create("http://" + candidate);
            if (uri.getHost() == null
                    || uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getPort() > 65535) {
                throw new IllegalArgumentException("Invalid host");
            }
            return uri.getHost().toLowerCase(Locale.ROOT) + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Domain name must be a valid host, optionally with a port");
        }
    }

    public record DomainRequest(
            @jakarta.validation.constraints.Size(max = 36) String id,

            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 36)
            String tenantId,

            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 255)
            String name,

            Boolean oauth2Enabled) {}

    public record DomainInfo(
            String id,
            long createdTime,
            String tenantId,
            String name,
            boolean oauth2Enabled,
            List<DomainOAuth2ClientInfo> oauth2ClientInfos) {}

    public record DomainOAuth2ClientInfo(String id, long createdTime, String name, String providerName) {}
}
