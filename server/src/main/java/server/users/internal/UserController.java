package server.users.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import server.openapi.BadRequestResponse;
import server.openapi.ConflictResponse;
import server.openapi.NotFoundResponse;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.users.UserAccount;
import server.users.UserService;
import server.ApiPaths;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Користувачі", description = "Реєстрація, пошук, перегляд, перейменування та видалення користувачів")
@RequestMapping("/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Зареєструвати користувача", description = "Створює нового користувача. Email має бути унікальним.")
    @ApiResponse(responseCode = "201", description = "Користувача створено")
    @BadRequestResponse
    @ConflictResponse
    @PostMapping
    ResponseEntity<UserResponse> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Дані для реєстрації нового користувача") @Valid @RequestBody CreateUserRequest request) {
        UserAccount account = userService.register(request.email(), request.password(), request.name());
        return ResponseEntity.created(URI.create(ApiPaths.PREFIX + "/users/" + account.id()))
                .body(UserResponse.from(account));
    }

    @Operation(summary = "Знайти користувачів", description = "Повертає всіх користувачів; необов'язковий параметр query звужує вибірку за email або ім'ям.")
    @ApiResponse(responseCode = "200", description = "Список користувачів")
    @GetMapping
    ResponseEntity<List<UserResponse>> findAll(
            @Parameter(description = "Фрагмент email або імені для пошуку; без нього повертаються всі користувачі", example = "owner") @RequestParam(required = false) String query) {
        List<UserResponse> responses = userService.findAll(query).stream()
                .map(UserResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Отримати користувача за id", description = "Повертає профіль користувача.")
    @ApiResponse(responseCode = "200", description = "Користувача знайдено")
    @BadRequestResponse
    @NotFoundResponse
    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getById(
            @Parameter(description = "Ідентифікатор користувача", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        UserAccount account = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(account));
    }

    @Operation(summary = "Перейменувати користувача", description = "Змінює ім'я користувача.")
    @ApiResponse(responseCode = "200", description = "Користувача оновлено")
    @BadRequestResponse
    @NotFoundResponse
    @PutMapping("/{id}")
    ResponseEntity<UserResponse> rename(
            @Parameter(description = "Ідентифікатор користувача", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Нове ім'я користувача") @Valid @RequestBody RenameUserRequest request) {
        UserAccount account = userService.rename(id, request.name());
        return ResponseEntity.ok(UserResponse.from(account));
    }

    @Operation(summary = "Видалити користувача", description = "Видаляє користувача.")
    @ApiResponse(responseCode = "204", description = "Користувача видалено")
    @NotFoundResponse
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Ідентифікатор користувача", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
