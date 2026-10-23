package server.executions.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import server.openapi.BadRequestResponse;
import server.openapi.NotFoundResponse;
import server.openapi.UnprocessableResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.executions.CommandExecution;
import server.executions.CommandExecutionService;

import server.ApiPaths;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Виконання команд", description = "Запуск команд на пристроях, журнал виконань і зміна статусу")
@RequestMapping("/devices/{deviceId}")
class CommandExecutionController {

    private final CommandExecutionService commandExecutionService;

    CommandExecutionController(CommandExecutionService commandExecutionService) {
        this.commandExecutionService = commandExecutionService;
    }

    @Operation(summary = "Виконати команду", description = "Перевіряє аргументи за схемою команди, виконує її на пристрої та записує результат у журнал.")
    @ApiResponse(responseCode = "202", description = "Команду прийнято до виконання; заголовок Location містить адресу виконання")
    @BadRequestResponse
    @NotFoundResponse
    @PostMapping("/commands/{commandId}/executions")
    ResponseEntity<CommandExecutionResponse> run(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @Parameter(description = "Ідентифікатор команди", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID commandId,
            @Parameter(description = "Тимчасова ідентифікація користувача (UUID) до впровадження Security; необов'язковий", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Аргументи виконання команди згідно з її схемою") @Valid @RequestBody ExecuteCommandRequest request) {
        CommandExecution execution = commandExecutionService.execute(deviceId, commandId, userId, request.args());
        URI location = URI.create(ApiPaths.PREFIX + "/devices/" + deviceId + "/executions/" + execution.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(location)
                .body(CommandExecutionResponse.from(execution));
    }

    @Operation(summary = "Переглянути журнал пристрою", description = "Повертає журнал виконань команд пристрою; параметр userId залишає лише записи цього користувача (журнал гостя).")
    @ApiResponse(responseCode = "200", description = "Журнал виконань")
    @BadRequestResponse
    @NotFoundResponse
    @GetMapping("/executions")
    List<CommandExecutionResponse> journal(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @Parameter(description = "Показати лише виконання цього користувача (журнал гостя); без нього повертаються всі", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @RequestParam(required = false) UUID userId) {
        return commandExecutionService.findJournal(deviceId, userId).stream()
                .map(CommandExecutionResponse::from)
                .toList();
    }

    @Operation(summary = "Отримати виконання за id", description = "Повертає один запис журналу виконань пристрою.")
    @ApiResponse(responseCode = "200", description = "Виконання знайдено")
    @BadRequestResponse
    @NotFoundResponse
    @GetMapping("/executions/{executionId}")
    CommandExecutionResponse getById(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @Parameter(description = "Ідентифікатор виконання", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID executionId) {
        return CommandExecutionResponse.from(commandExecutionService.findById(deviceId, executionId));
    }

    @Operation(summary = "Змінити статус виконання", description = "Переводить виконання в новий статус за допустимими переходами (PENDING, RUNNING, SUCCESS, FAILED, TIMEOUT).")
    @ApiResponse(responseCode = "200", description = "Статус змінено")
    @BadRequestResponse
    @NotFoundResponse
    @UnprocessableResponse
    @PatchMapping("/executions/{executionId}")
    CommandExecutionResponse changeStatus(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @Parameter(description = "Ідентифікатор виконання", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID executionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Новий статус виконання") @Valid @RequestBody ChangeStatusRequest request) {
        return CommandExecutionResponse.from(
                commandExecutionService.changeStatus(deviceId, executionId, request.status()));
    }

    @Operation(summary = "Видалити виконання", description = "Видаляє запис із журналу виконань.")
    @ApiResponse(responseCode = "204", description = "Запис видалено")
    @NotFoundResponse
    @DeleteMapping("/executions/{executionId}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID deviceId,
            @Parameter(description = "Ідентифікатор виконання", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID executionId) {
        commandExecutionService.delete(deviceId, executionId);
        return ResponseEntity.noContent().build();
    }
}
