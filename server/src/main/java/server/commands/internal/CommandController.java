package server.commands.internal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.commands.Command;
import server.commands.CommandService;
import server.commands.RequiredRole;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/devices/{deviceId}/commands")
public class CommandController {
    private final CommandService commandService;
    CommandController(CommandService commandService) {
        this.commandService = commandService;
    }
    @PostMapping
    ResponseEntity<CommandResponse> create(
        @PathVariable UUID deviceId,
        @Valid @RequestBody CreateCommandRequest request) {
            Command command = commandService.create(
            deviceId, request.name(), request.argsSchema(), request.requiredRole());
            return ResponseEntity
                .created(URI.create("/devices/" + deviceId + "/commands/" + command.getId()))
                .body(CommandResponse.from(command));
    }

    @GetMapping
    List<CommandResponse> get(@PathVariable UUID deviceId, @RequestParam(required = false) RequiredRole requiredRole) {
        return commandService.findAllByDevice(deviceId, requiredRole).stream().map(CommandResponse::from).toList();
    }

    @PutMapping("/{commandId}")
    ResponseEntity<Void> update( @PathVariable UUID deviceId, @PathVariable UUID commandId, @Valid  @RequestBody UpdateCommandRequest request) {
        commandService.update(deviceId, commandId, request.argsSchema(), request.requiredRole());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commandId}")
    ResponseEntity<Void> delete(@PathVariable UUID deviceId, @PathVariable UUID commandId) {
        commandService.delete(deviceId, commandId);
        return ResponseEntity.noContent().build();
    }
}
