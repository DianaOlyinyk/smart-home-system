package server.devices;
import java.util.UUID;

public class DeviceNotFoundException extends RuntimeException{
    public DeviceNotFoundException(UUID deviceId){
        super("Пристрій з id " + deviceId + " не знайдено");
    }
}
