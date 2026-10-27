package io.github.zhijunio.oauth2client.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin/oauth2-clients")
    String clients() {
        return "admin/oauth2-clients";
    }
}
