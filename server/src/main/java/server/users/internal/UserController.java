package server.users.internal;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.users.UserAccount;
import server.users.UserService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        UserAccount account = userService.register(request.email(), request.password(), request.name());
        return ResponseEntity.created(URI.create("/users/" + account.id()))
                .body(UserResponse.from(account));
    }

    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        UserAccount account = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(account));
    }
}
