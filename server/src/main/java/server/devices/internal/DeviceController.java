package server.devices.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import server.openapi.BadRequestResponse;
import server.openapi.NotFoundResponse;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.devices.Device;
import server.devices.DeviceService;
import server.devices.DeviceType;

import server.ApiPaths;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Пристрої", description = "Керування розумними пристроями: створення, пошук, перейменування, видалення")
@RequestMapping("/devices")
class DeviceController {

    private final DeviceService deviceService;

    DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(summary = "Створити пристрій", description = "Створює пристрій. Якщо передано X-User-Id, цей користувач стає його власником (OWNER).")
    @ApiResponse(responseCode = "201", description = "Пристрій створено; заголовок Location містить адресу ресурсу")
    @BadRequestResponse
    @NotFoundResponse
    @PostMapping
    ResponseEntity<DeviceResponse> create(
            @Parameter(description = "Тимчасова ідентифікація користувача (UUID) до впровадження Security; необов'язковий", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @RequestHeader(name = "X-User-Id", required = false) UUID userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Назва й тип нового пристрою") @Valid @RequestBody CreateDeviceRequest request) {
        Device device = deviceService.create(request.name(), request.type(), userId);
        return ResponseEntity.created(URI.create(ApiPaths.PREFIX + "/devices/" + device.getId()))
                .body(DeviceResponse.from(device));
    }

    @Operation(summary = "Отримати список пристроїв", description = "Повертає всі пристрої разом із доступами; параметр type фільтрує за типом.")
    @ApiResponse(responseCode = "200", description = "Список пристроїв")
    @BadRequestResponse
    @GetMapping
    List<DeviceResponse> findAll(
            @Parameter(description = "Тип пристрою для фільтрації: LAMP, KETTLE, AC, COFFEE_MACHINE або BLINDS; без нього повертаються всі", example = "LAMP") @RequestParam(required = false) DeviceType type) {
        return deviceService.findAll(type).stream().map(DeviceResponse::from).toList();
    }

    @Operation(summary = "Отримати пристрій за id", description = "Повертає пристрій разом зі списком доступів.")
    @ApiResponse(responseCode = "200", description = "Пристрій знайдено")
    @BadRequestResponse
    @NotFoundResponse
    @GetMapping("/{id}")
    DeviceResponse getById(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        return DeviceResponse.from(deviceService.getWithAccesses(id));
    }

    @Operation(summary = "Перейменувати пристрій", description = "Змінює назву пристрою.")
    @ApiResponse(responseCode = "200", description = "Пристрій оновлено")
    @BadRequestResponse
    @NotFoundResponse
    @PutMapping("/{id}")
    DeviceResponse rename(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Нова назва пристрою") @Valid @RequestBody RenameDeviceRequest request) {
        return DeviceResponse.from(deviceService.rename(id, request.name()));
    }

    @Operation(summary = "Видалити пристрій", description = "Видаляє пристрій разом з його доступами, командами та журналом виконань.")
    @ApiResponse(responseCode = "204", description = "Пристрій видалено")
    @NotFoundResponse
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Ідентифікатор пристрою", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        deviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
