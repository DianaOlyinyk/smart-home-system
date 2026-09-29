package server.executions.internal;

import server.executions.CommandExecution;
import server.users.User;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

record CommandExecutionResponse(
        UUID id,
        UUID deviceId,
        UUID commandId,
        String commandName,
        UUID executedById,
        String executedByEmail,
        Map<String, Object> args,
        String status,
        Instant requestedAt,
        Instant completedAt) {

    static CommandExecutionResponse from(CommandExecution execution) {
        User executedBy = execution.getExecutedBy();
        return new CommandExecutionResponse(
                execution.getId(),
                execution.getDevice().getId(),
                execution.getCommand().getId(),
                execution.getCommand().getName(),
                executedBy == null ? null : executedBy.getId(),
                executedBy == null ? null : executedBy.getEmail(),
                execution.getArgs(),
                execution.getStatus().name(),
                execution.getRequestedAt(),
                execution.getCompletedAt());
    }
}
