package server.devices;

public class CommandAlreadyExistsException extends RuntimeException {
    public CommandAlreadyExistsException(String name) {
        super("Команда з ім'ям " + name + "вже існує");
    }
}
