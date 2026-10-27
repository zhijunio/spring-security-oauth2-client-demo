package io.github.zhijunio.oauth2client.web;

import io.github.zhijunio.oauth2client.api.PageData;
import io.github.zhijunio.oauth2client.oauth2.DomainService;
import io.github.zhijunio.oauth2client.oauth2.DomainService.DomainInfo;
import io.github.zhijunio.oauth2client.oauth2.DomainService.DomainRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/domain")
public class DomainController {

    private final DomainService domains;

    public DomainController(DomainService domains) {
        this.domains = domains;
    }

    @PostMapping
    DomainResource save(
            @Valid @RequestBody DomainRequest input, @RequestParam(required = false) String oauth2ClientIds) {
        DomainInfo domain = domains.save(input, parseIds(oauth2ClientIds));
        return new DomainResource(
                domain.id(), domain.createdTime(), domain.tenantId(), domain.name(), domain.oauth2Enabled());
    }

    @PutMapping("/{id}/oauth2Clients")
    void updateOauth2Clients(@PathVariable String id, @RequestBody List<String> clientIds) {
        domains.updateClients(id, clientIds);
    }

    @GetMapping("/infos")
    PageData<DomainInfo> infos(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) {
        if (pageSize < 1 || pageSize > 100 || page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pageSize must be 1..100 and page must be >= 0");
        }
        return domains.page(pageSize, page, textSearch, sortProperty, sortOrder);
    }

    @GetMapping("/info/{id}")
    DomainInfo info(@PathVariable String id) {
        return domains.get(id);
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable String id) {
        domains.delete(id);
    }

    private List<String> parseIds(String clientIds) {
        if (clientIds == null) return null;
        return clientIds.isBlank()
                ? List.of()
                : Arrays.stream(clientIds.split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .distinct()
                        .toList();
    }

    public record DomainResource(String id, long createdTime, String tenantId, String name, boolean oauth2Enabled) {}
}
