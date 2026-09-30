package server.commands;
import java.util.UUID;

public class CommandAlreadyExistsException extends RuntimeException {
    public CommandAlreadyExistsException(UUID deviceId, String name) {
        super("Команда " + name + " для пристрою " + deviceId + " вже існує");
    }
}
