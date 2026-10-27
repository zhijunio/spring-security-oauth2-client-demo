package io.github.zhijunio.oauth2client.web;

import io.github.zhijunio.oauth2client.api.PageData;
import io.github.zhijunio.oauth2client.oauth2.OAuth2ClientService;
import io.github.zhijunio.oauth2client.oauth2.OAuth2ClientService.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/oauth2")
public class OAuth2ClientController {

    private final OAuth2ClientService service;

    public OAuth2ClientController(OAuth2ClientService service) {
        this.service = service;
    }

    @GetMapping("/client/infos")
    PageData<OAuth2ClientInfo> clients(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int pageSize,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String textSearch,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String sortProperty,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String sortOrder) {
        if (pageSize < 1 || pageSize > 100 || page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pageSize must be 1..100 and page must be >= 0");
        }
        return service.clientPage(pageSize, page, textSearch, sortProperty, sortOrder);
    }

    @GetMapping(value = "/client/infos", params = "clientIds")
    List<OAuth2ClientInfo> clientsByIds(@org.springframework.web.bind.annotation.RequestParam List<String> clientIds) {
        return service.clientsByIds(clientIds);
    }

    @GetMapping(value = "/client/list", params = "clientIds")
    List<OAuth2ClientInfo> clientList(@org.springframework.web.bind.annotation.RequestParam List<String> clientIds) {
        return service.clientsByIds(clientIds);
    }

    @PostMapping("/client")
    OAuth2ClientDetail create(@Valid @RequestBody OAuth2ClientRequest input) {
        String id = service.create(input).id();
        return service.detail(id);
    }

    @GetMapping("/client/{id}")
    OAuth2ClientDetail get(@PathVariable String id) {
        return service.detail(id);
    }

    @DeleteMapping("/client/{id}")
    void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping("/config/template")
    List<OAuth2ClientTemplateInfo> templates() {
        return service.templates();
    }

    @PostMapping("/config/template")
    OAuth2ClientTemplateInfo saveTemplate(@Valid @RequestBody OAuth2ClientTemplateRequest input) {
        return service.saveTemplate(input);
    }

    @DeleteMapping("/config/template/{clientRegistrationTemplateId}")
    void deleteTemplate(@PathVariable String clientRegistrationTemplateId) {
        service.deleteTemplate(clientRegistrationTemplateId);
    }

    @GetMapping("/loginProcessingUrl")
    String loginProcessingUrl() {
        return "\"/login/oauth2/code/\"";
    }
}
