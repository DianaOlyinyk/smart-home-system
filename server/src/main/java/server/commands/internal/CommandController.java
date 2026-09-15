package server.commands.internal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.commands.Command;
import server.commands.CommandService;
import java.net.URI;
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
                .created(URI.create("/devices/" + deviceId + "/commands/" + command.id()))
                .body(CommandResponse.from(command));
    }
}
