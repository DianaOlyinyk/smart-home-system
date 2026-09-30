package server.executions.internal;

import jakarta.validation.constraints.NotNull;
import server.executions.ExecutionStatus;

record ChangeStatusRequest(
        @NotNull(message = "Поле status є обов'язковим")
        ExecutionStatus status) {
}
