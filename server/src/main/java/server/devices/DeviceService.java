package server.devices;

public interface DeviceService {

    Device create(String name, DeviceType type);
}