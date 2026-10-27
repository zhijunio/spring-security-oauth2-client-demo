package io.github.zhijunio.oauth2client.web;

import io.github.zhijunio.oauth2client.oauth2.DomainService;
import io.github.zhijunio.oauth2client.oauth2.OAuth2ClientService;
import io.github.zhijunio.oauth2client.security.Oauth2AuthenticationSuccessHandler;
import io.github.zhijunio.oauth2client.service.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final OAuth2ClientService clients;

    public HomeController(OAuth2ClientService clients) {
        this.clients = clients;
    }

    @GetMapping("/")
    public String home(Authentication authentication, HttpServletRequest request, Model model) {
        LoginSession session = request.getSession(false) == null
                ? null
                : (LoginSession)
                        request.getSession(false).getAttribute(Oauth2AuthenticationSuccessHandler.SESSION_ATTRIBUTE);
        boolean oauthAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User
                && session != null;
        boolean adminAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals);
        if (!oauthAuthenticated && !adminAuthenticated) return "redirect:/login";

        model.addAttribute("authenticated", true);
        model.addAttribute("oauthAuthenticated", oauthAuthenticated);
        model.addAttribute("adminAuthenticated", adminAuthenticated);
        model.addAttribute("loginSession", oauthAuthenticated ? session : null);
        return "index";
    }

    @GetMapping("/login")
    public String login(
            Authentication authentication,
            HttpServletRequest request,
            Model model,
            @RequestParam(defaultValue = "false") boolean error,
            @RequestParam(defaultValue = "false") boolean signedOut) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals)) return "redirect:/";
        model.addAttribute(
                "providers",
                clients.enabledClientsForDomain(DomainService.requestDomain(request)).stream()
                        .map(client ->
                                new LoginProvider(client.id(), client.loginButtonIcon(), client.loginButtonLabel()))
                        .toList());
        model.addAttribute("loginError", error);
        model.addAttribute("signedOut", signedOut);
        return "login";
    }

    public record LoginProvider(String id, String loginButtonIcon, String loginButtonLabel) {}
}
