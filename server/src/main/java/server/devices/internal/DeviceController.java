package server.devices.internal;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import server.devices.Device;
import server.devices.DeviceService;
import server.devices.DeviceType;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/devices")
class DeviceController {

    private final DeviceService deviceService;

    DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    ResponseEntity<DeviceResponse> create(@Valid @RequestBody CreateDeviceRequest request) {
        Device device = request.ownerId() == null
                ? deviceService.create(request.name(), request.type())
                : deviceService.create(request.name(), request.type(), request.ownerId());
        return ResponseEntity.created(URI.create("/devices/" + device.id()))
                .body(DeviceResponse.from(device));
    }

    @GetMapping
    List<DeviceResponse> findAll(@RequestParam(required = false) DeviceType type) {
        return deviceService.findAll(type).stream().map(DeviceResponse::from).toList();
    }

    @GetMapping("/{id}")
    DeviceResponse getWithAccesses(@PathVariable UUID id) {
        return DeviceResponse.from(deviceService.getWithAccesses(id));
    }

    @PutMapping("/{id}")
    DeviceResponse rename(@PathVariable UUID id, @Valid @RequestBody RenameDeviceRequest request) {
        return DeviceResponse.from(deviceService.rename(id, request.name()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        deviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
