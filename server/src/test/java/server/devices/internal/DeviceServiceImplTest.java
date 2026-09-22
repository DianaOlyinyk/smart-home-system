package server.devices.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository;
import server.devices.DeviceType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	DeviceRepository deviceRepository;
	DeviceServiceImpl deviceService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		deviceService = new DeviceServiceImpl(deviceRepository, clock);
	}

	@Test
	void createsDeviceAndSavesIt() {
		when(deviceRepository.save(org.mockito.ArgumentMatchers.any(Device.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Device result = deviceService.create("Living room lamp", DeviceType.LAMP);

		assertNotNull(result.id());
		assertEquals("Living room lamp", result.name());
		assertEquals(DeviceType.LAMP, result.type());
		assertNotNull(result.connectionToken());
		assertEquals(NOW, result.createdAt());
		verify(deviceRepository).save(result);
	}

	@Test
	void findsExistingDevice() {
		UUID deviceId = UUID.randomUUID();
		Device device = new Device(deviceId, "Kettle", DeviceType.KETTLE, "token", NOW);
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

		Device result = deviceService.findById(deviceId);

		assertEquals(device, result);
		verify(deviceRepository).findById(deviceId);
	}

	@Test
	void throwsWhenDeviceMissing() {
		UUID deviceId = UUID.randomUUID();
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

		assertThrows(DeviceNotFoundException.class, () -> deviceService.findById(deviceId));
	}
}
