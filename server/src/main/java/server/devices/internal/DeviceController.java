package server.devices.internal;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.devices.Device;
import server.devices.DeviceService;

import java.net.URI;

@RestController
@RequestMapping("/devices")
class DeviceController {

    private final DeviceService deviceService;

    DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    ResponseEntity<DeviceResponse> create(@Valid @RequestBody CreateDeviceRequest request) {
        Device device = deviceService.create(request.name(), request.type());
        return ResponseEntity.created(URI.create("/devices/" + device.id()))
                .body(DeviceResponse.from(device));
    }
}