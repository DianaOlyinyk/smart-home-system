package server.auth.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import server.openapi.BadRequestResponse;
import server.openapi.UnauthorizedResponse;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.auth.AuthService;
import server.auth.IssuedToken;

@RestController
@Tag(name = "Автентифікація", description = "Вхід у систему та отримання токена")
@RequestMapping("/login")
class TokenController {

    private final AuthService authService;

    TokenController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Увійти в систему", description = "Перевіряє email і пароль та видає токен автентифікації.")
    @ApiResponse(responseCode = "200", description = "Вхід виконано, токен видано")
    @BadRequestResponse
    @UnauthorizedResponse
    @PostMapping
    ResponseEntity<TokenResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Email і пароль користувача") @Valid @RequestBody LoginRequest request) {
        IssuedToken issuedToken = authService.login(request.email(), request.password());
        return ResponseEntity.ok(TokenResponse.from(issuedToken));
    }
}
