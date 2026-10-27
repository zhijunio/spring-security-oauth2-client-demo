package io.github.zhijunio.oauth2client.web;

import io.github.zhijunio.oauth2client.oauth2.DomainService;
import io.github.zhijunio.oauth2client.oauth2.OAuth2ClientService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicOAuth2ClientController {

    private final OAuth2ClientService service;

    public PublicOAuth2ClientController(OAuth2ClientService service) {
        this.service = service;
    }

    @PostMapping("/api/noauth/oauth2Clients")
    List<LoginProvider> clients(HttpServletRequest request) {
        return service.enabledClientsForDomain(DomainService.requestDomain(request)).stream()
                .map(client -> new LoginProvider(
                        client.loginButtonLabel(), client.loginButtonIcon(), "/oauth2/authorization/" + client.id()))
                .toList();
    }

    public record LoginProvider(String name, String icon, String url) {}
}
