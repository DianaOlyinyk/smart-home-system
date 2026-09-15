package server.executions.internal;

import jakarta.validation.constraints.NotBlank;

record ExecuteCommandRequest(
        @NotBlank(message = "Аргументи команди (argsJson) є обов'язковими")
        String argsJson) {
}
