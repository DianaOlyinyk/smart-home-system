package server.executions;

public enum ExecutionStatus {
    PENDING, RUNNING, SUCCESS, FAILED, TIMEOUT;

    public boolean canTransitionTo(ExecutionStatus next) {
        return switch (this) {
            case PENDING -> next == RUNNING;
            case RUNNING -> next == SUCCESS || next == FAILED || next == TIMEOUT;
            case SUCCESS, FAILED, TIMEOUT -> false;
        };
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == TIMEOUT;
    }
}
