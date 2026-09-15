package server.executions.internal;

import server.executions.CommandExecution;

import java.time.Instant;
import java.util.UUID;

record CommandExecutionResponse(
        UUID id,
        UUID deviceId,
        UUID commandId,
        String argsJson,
        String status,
        Instant requestedAt) {

    static CommandExecutionResponse from(CommandExecution execution) {
        return new CommandExecutionResponse(
                execution.id(),
                execution.deviceId(),
                execution.commandId(),
                execution.argsJson(),
                execution.status(),
                execution.requestedAt());
    }
}
