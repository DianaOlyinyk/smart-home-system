package server.devices;

public class AccessAlreadyGrantedException extends RuntimeException {
    public AccessAlreadyGrantedException() {
        super("Доступ вже надано");
    }
}
