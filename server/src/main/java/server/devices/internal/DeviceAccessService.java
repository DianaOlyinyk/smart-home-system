package server.devices.internal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import server.devices.AccessRole;
import server.devices.DeviceAccess;
import server.devices.DeviceAccessResponse;
import java.util.List;
import java.util.UUID;

@RestController
class DeviceAccessController {

    private final DeviceAccessService accessService;

    DeviceAccessController(DeviceAccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/devices/{id}/accesses")
    ResponseEntity<DeviceAccessResponse> grant(
            @PathVariable UUID id,
            @Valid @RequestBody GrantAccessRequest request) {
        DeviceAccess access = accessService.grant(id, request.email(), request.role(), request.grantedById());
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceAccessResponse.from(access));
    }

    @DeleteMapping("/devices/{id}/accesses/{userId}")
    ResponseEntity<Void> revoke(@PathVariable UUID id, @PathVariable UUID userId) {
        accessService.revoke(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/devices/{id}/accesses")
    ResponseEntity<List<DeviceAccessResponse>> findByDevice(@PathVariable UUID id) {
        List<DeviceAccessResponse> responses = accessService.findByDevice(id).stream()
                .map(DeviceAccessResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/users/{userId}/devices")
    ResponseEntity<List<DeviceAccessResponse>> findByUser(@PathVariable UUID userId) {
        List<DeviceAccessResponse> responses = accessService.findByUser(userId).stream()
                .map(DeviceAccessResponse::from).toList();
        return ResponseEntity.ok(responses);
    }
}

record GrantAccessRequest(
        @NotBlank(message = "Email є обов'язковим") String email,
        @NotNull(message = "Роль є обов'язковою") AccessRole role,
        @NotNull(message = "grantedById є обов'язковим") UUID grantedById) {}