package server.executions.internal;

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

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/devices/{deviceId}")
class CommandExecutionController {

    private final CommandExecutionService commandExecutionService;

    CommandExecutionController(CommandExecutionService commandExecutionService) {
        this.commandExecutionService = commandExecutionService;
    }

    @PostMapping("/commands/{commandId}/executions")
    ResponseEntity<CommandExecutionResponse> run(
            @PathVariable UUID deviceId,
            @PathVariable UUID commandId,
            @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody ExecuteCommandRequest request) {
        CommandExecution execution = commandExecutionService.execute(deviceId, commandId, userId, request.args());
        URI location = URI.create("/devices/" + deviceId + "/executions/" + execution.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(location)
                .body(CommandExecutionResponse.from(execution));
    }

    @GetMapping("/executions")
    List<CommandExecutionResponse> journal(
            @PathVariable UUID deviceId,
            @RequestParam(required = false) UUID userId) {
        return commandExecutionService.findJournal(deviceId, userId).stream()
                .map(CommandExecutionResponse::from)
                .toList();
    }

    @GetMapping("/executions/{executionId}")
    CommandExecutionResponse getById(@PathVariable UUID deviceId, @PathVariable UUID executionId) {
        return CommandExecutionResponse.from(commandExecutionService.findById(deviceId, executionId));
    }

    @PatchMapping("/executions/{executionId}")
    CommandExecutionResponse changeStatus(
            @PathVariable UUID deviceId,
            @PathVariable UUID executionId,
            @Valid @RequestBody ChangeStatusRequest request) {
        return CommandExecutionResponse.from(
                commandExecutionService.changeStatus(deviceId, executionId, request.status()));
    }

    @DeleteMapping("/executions/{executionId}")
    ResponseEntity<Void> delete(@PathVariable UUID deviceId, @PathVariable UUID executionId) {
        commandExecutionService.delete(deviceId, executionId);
        return ResponseEntity.noContent().build();
    }
}
