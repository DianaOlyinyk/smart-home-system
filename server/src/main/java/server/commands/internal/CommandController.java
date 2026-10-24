package server.commands.internal;

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
import server.commands.Command;
import server.commands.CommandService;
import server.commands.RequiredRole;

import server.ApiPaths;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Команди пристрою", description = "Опис команд, які можна виконати на пристрої")
@RequestMapping("/devices/{deviceId}/commands")
public class CommandController {
    private final CommandService commandService;
    CommandController(CommandService commandService) {
        this.commandService = commandService;
    }
    @Operation(summary = "Створити команду", description = "Додає команду до пристрою: назва, JSON Schema аргументів і роль, якій команда доступна.")
    @ApiResponse(responseCode = "201", description = "Команду створено")
    @BadRequestResponse
    @NotFoundResponse
    @ConflictResponse
    @PostMapping
    ResponseEntity<CommandResponse> create(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Назва команди, схема аргументів і роль доступу") @Valid @RequestBody CreateCommandRequest request) {
            Command command = commandService.create(
            deviceId, request.name(), request.argsSchema(), request.requiredRole());
            return ResponseEntity
                .created(URI.create(ApiPaths.PREFIX + "/devices/" + deviceId + "/commands/" + command.getId()))
                .body(CommandResponse.from(command));
    }

    @Operation(summary = "Отримати команди пристрою", description = "Повертає команди пристрою, відсортовані за назвою; параметр requiredRole фільтрує за роллю.")
    @ApiResponse(responseCode = "200", description = "Список команд")
    @BadRequestResponse
    @NotFoundResponse
    @GetMapping
    List<CommandResponse> get(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @Parameter(description = "Роль, якій доступна команда: OWNER або GUEST; без неї повертаються всі команди", example = "GUEST") @RequestParam(required = false) RequiredRole requiredRole) {
        return commandService.findAllByDevice(deviceId, requiredRole).stream().map(CommandResponse::from).toList();
    }

    @Operation(summary = "Оновити команду", description = "Змінює схему аргументів і роль доступу команди.")
    @ApiResponse(responseCode = "204", description = "Команду оновлено")
    @BadRequestResponse
    @NotFoundResponse
    @PutMapping("/{commandId}")
    ResponseEntity<Void> update(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @Parameter(description = "Ідентифікатор команди", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID commandId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Нова схема аргументів і роль доступу") @Valid @RequestBody UpdateCommandRequest request) {
        commandService.update(deviceId, commandId, request.argsSchema(), request.requiredRole());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Видалити команду", description = "Видаляє команду пристрою.")
    @ApiResponse(responseCode = "204", description = "Команду видалено")
    @NotFoundResponse
    @DeleteMapping("/{commandId}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @Parameter(description = "Ідентифікатор команди", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID commandId) {
        commandService.delete(deviceId, commandId);
        return ResponseEntity.noContent().build();
    }
}
