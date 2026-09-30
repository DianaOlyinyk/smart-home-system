package server.devices.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceDeletedEvent;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository;
import server.devices.DeviceType;
import server.users.User;
import server.users.UserRepository;

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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	DeviceRepository deviceRepository;
	@Mock
	UserRepository userRepository;
	@Mock
	ApplicationEventPublisher eventPublisher;
	DeviceServiceImpl deviceService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		deviceService = new DeviceServiceImpl(deviceRepository, clock, userRepository, eventPublisher);
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

	@Test
	void createsDeviceWithOwnerAccess() {
		UUID ownerId = UUID.randomUUID();
		User owner = new User(ownerId, "owner@example.test", "hash", "Owner", NOW);
		when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
		when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Device result = deviceService.create("Living room lamp", DeviceType.LAMP, ownerId);

		assertEquals(1, result.getAccesses().size());
		assertEquals(ownerId, result.getAccesses().iterator().next().getUserId());
		assertEquals(AccessRole.OWNER, result.getAccesses().iterator().next().getRole());
		verify(userRepository).findById(ownerId);
		verify(deviceRepository, org.mockito.Mockito.times(2)).save(any(Device.class));
	}

	@Test
	void findsAllUsingRequestedType() {
		DeviceType type = DeviceType.LAMP;
		when(deviceRepository.findAll(type)).thenReturn(java.util.List.of());

		assertEquals(java.util.List.of(), deviceService.findAll(type));

		verify(deviceRepository).findAll(type);
	}

	@Test
	void getsDeviceWithAccesses() {
		UUID deviceId = UUID.randomUUID();
		Device device = new Device(deviceId, "Lamp", DeviceType.LAMP, "token", NOW);
		when(deviceRepository.getWithAccesses(deviceId)).thenReturn(Optional.of(device));

		assertEquals(device, deviceService.getWithAccesses(deviceId));

		verify(deviceRepository).getWithAccesses(deviceId);
		verify(deviceRepository, never()).findById(deviceId);
	}

	@Test
	void renamesAndSavesDevice() {
		UUID deviceId = UUID.randomUUID();
		Device device = new Device(deviceId, "Old name", DeviceType.LAMP, "token", NOW);
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
		when(deviceRepository.save(device)).thenReturn(device);

		Device result = deviceService.rename(deviceId, "New name");

		assertEquals("New name", result.name());
		verify(deviceRepository).save(device);
	}

	@Test
	void deletesDeviceAndPublishesDeletionEvent() {
		UUID deviceId = UUID.randomUUID();
		Device device = new Device(deviceId, "Lamp", DeviceType.LAMP, "token", NOW);
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

		deviceService.delete(deviceId);

		verify(deviceRepository).deleteById(deviceId);
		verify(eventPublisher).publishEvent(new DeviceDeletedEvent(deviceId));
	}
}
