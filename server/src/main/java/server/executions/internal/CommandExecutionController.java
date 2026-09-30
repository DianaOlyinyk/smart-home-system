package server.executions.internal;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.executions.CommandExecution;
import server.executions.CommandExecutionService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/devices/{deviceId}/commands/{commandId}/executions")
class CommandExecutionController {

    private final CommandExecutionService commandExecutionService;

    CommandExecutionController(CommandExecutionService commandExecutionService) {
        this.commandExecutionService = commandExecutionService;
    }

    @PostMapping
    ResponseEntity<CommandExecutionResponse> run(
            @PathVariable UUID deviceId,
            @PathVariable UUID commandId,
            @Valid @RequestBody ExecuteCommandRequest request) {
        CommandExecution execution = commandExecutionService.execute(deviceId, commandId, request.args());
        URI location = URI.create("/devices/" + deviceId + "/commands/" + commandId + "/executions/" + execution.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(location)
                .body(CommandExecutionResponse.from(execution));
    }
}
