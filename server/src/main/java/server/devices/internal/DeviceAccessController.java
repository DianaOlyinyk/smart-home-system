package server.devices.internal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
        UUID grantedById = UUID.randomUUID();
        DeviceAccess access = accessService.grant(id, request.email(), request.role(), grantedById);
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
        @NotBlank(message = "Роль є обов'язковою") String role) {}

record DeviceAccessResponse(UUID id, UUID deviceId, UUID userId, String role) {
    static DeviceAccessResponse from(DeviceAccess access) {
        return new DeviceAccessResponse(
                access.getId(),
                access.getDevice().getId(),
                access.getUser().getId(),
                access.getRole()
        );
    }
}