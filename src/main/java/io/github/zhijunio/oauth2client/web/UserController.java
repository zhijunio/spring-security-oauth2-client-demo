package io.github.zhijunio.oauth2client.web;

import io.github.zhijunio.oauth2client.service.UserService;
import io.github.zhijunio.oauth2client.service.UserService.UserInfo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    List<UserInfo> list() {
        return users.list();
    }

    @GetMapping("/{id}")
    UserInfo get(@PathVariable String id) {
        return users.get(id);
    }
}
