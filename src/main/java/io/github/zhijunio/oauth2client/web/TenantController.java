package io.github.zhijunio.oauth2client.web;

import io.github.zhijunio.oauth2client.service.TenantService;
import io.github.zhijunio.oauth2client.service.TenantService.TenantInfo;
import io.github.zhijunio.oauth2client.service.TenantService.TenantRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenants;

    public TenantController(TenantService tenants) {
        this.tenants = tenants;
    }

    @GetMapping
    List<TenantInfo> list() {
        return tenants.list();
    }

    @GetMapping("/{id}")
    TenantInfo get(@PathVariable String id) {
        return tenants.get(id);
    }

    @PostMapping
    TenantInfo create(@Valid @RequestBody TenantRequest input) {
        return tenants.create(input);
    }
}
