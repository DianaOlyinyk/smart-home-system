package server.commands;

import java.util.UUID;

public class CommandNotFoundException extends RuntimeException {

    public CommandNotFoundException(UUID deviceId, UUID commandId) {
        super("Команду з id " + commandId + " для пристрою " + deviceId + " не знайдено");
    }
}
