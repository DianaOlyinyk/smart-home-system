package server.users.internal;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.users.UserAccount;
import server.users.UserService;
import java.net.URI;
import java.util.List;
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

    @GetMapping
    ResponseEntity<List<UserResponse>> findAll(@RequestParam(required = false) String query){
        List<UserResponse> responses = userService.findAll(query).stream()
                .map(UserResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        UserAccount account = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(account));
    }

    @PutMapping("/{id}")
    ResponseEntity<UserResponse> rename(@PathVariable UUID id, @Valid @RequestBody RenameUserRequest request) {
        UserAccount account = userService.rename(id, request.name());
        return ResponseEntity.ok(UserResponse.from(account));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
