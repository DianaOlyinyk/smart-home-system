package server.executions;

import java.util.Map;

public class InvalidCommandArgsException extends RuntimeException {

    private final Map<String, String> errors;

    public InvalidCommandArgsException(String message) {
        super(message);
        this.errors = Map.of();
    }

    public InvalidCommandArgsException(Map<String, String> errors) {
        super("Аргументи команди не відповідають схемі");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public static InvalidCommandArgsException malformedJson() {
        return new InvalidCommandArgsException("Аргументи мають бути коректним JSON-об'єктом");
    }
}
