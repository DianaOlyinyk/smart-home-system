package server.devices;

public class DeviceAccessNotFoundException extends RuntimeException {
    public DeviceAccessNotFoundException() {
        super("Доступ до пристрою не знайдено");
    }
}
