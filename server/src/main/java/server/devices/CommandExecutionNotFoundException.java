package server.devices;
import java.util.UUID;
public class CommandExecutionNotFoundException extends RuntimeException {
    public CommandExecutionNotFoundException(UUID id) {
        super("Виконання команди з id " + id + " не знайдено");
    }
}
