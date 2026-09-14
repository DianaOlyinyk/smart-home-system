package server.auth.internal;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.auth.AuthService;
import server.auth.IssuedToken;

@RestController
@RequestMapping("/tokens")
class TokenController {

    private final AuthService authService;

    TokenController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        IssuedToken issuedToken = authService.login(request.email(), request.password());
        return ResponseEntity.ok(TokenResponse.from(issuedToken));
    }
}
