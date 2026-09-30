package server.devices.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceAccess;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository;
import server.devices.DeviceType;
import server.users.User;
import server.users.UserNotFoundException;
import server.users.UserService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	@Mock
	DeviceRepository deviceRepository;
	@Mock
	UserService userService;
	DeviceServiceImpl deviceService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		deviceService = new DeviceServiceImpl(deviceRepository, userService, clock);
	}

	@Test
	void createsDeviceWithoutOwnerAndSavesIt() {
		when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Device result = deviceService.create("Living room lamp", DeviceType.LAMP, null);

		assertNull(result.getId());
		assertEquals("Living room lamp", result.getName());
		assertEquals(DeviceType.LAMP, result.getType());
		assertNotNull(result.getConnectionToken());
		assertEquals(NOW, result.getCreatedAt());
		assertTrue(result.getAccesses().isEmpty());
		verify(deviceRepository).save(result);
		verifyNoInteractions(userService);
	}

	@Test
	void createsDeviceAndGrantsOwnerAccessToCreator() {
		UUID ownerId = UUID.randomUUID();
		User owner = userWithId(ownerId);
		when(userService.getUser(ownerId)).thenReturn(owner);
		when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Device result = deviceService.create("Living room lamp", DeviceType.LAMP, ownerId);

		assertEquals(1, result.getAccesses().size());
		DeviceAccess access = result.getAccesses().iterator().next();
		assertEquals(AccessRole.OWNER, access.getRole());
		assertEquals(ownerId, access.getUserId());
		assertEquals(ownerId, access.getGrantedById());
		assertEquals(NOW, access.getGrantedAt());
		verify(deviceRepository).save(result);
	}

	@Test
	void createFailsWhenOwnerDoesNotExist() {
		UUID ownerId = UUID.randomUUID();
		when(userService.getUser(ownerId)).thenThrow(new UserNotFoundException(ownerId));

		assertThrows(UserNotFoundException.class,
				() -> deviceService.create("Living room lamp", DeviceType.LAMP, ownerId));

		verify(deviceRepository, never()).save(any());
	}

	@Test
	void findsExistingDevice() {
		UUID deviceId = UUID.randomUUID();
		Device device = new Device("Kettle", DeviceType.KETTLE, "token", NOW);
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
	void findAllWithoutTypeLoadsAllDevicesWithAccesses() {
		List<Device> devices = List.of(new Device("Kettle", DeviceType.KETTLE, "token", NOW));
		when(deviceRepository.findAllWithAccesses()).thenReturn(devices);

		assertEquals(devices, deviceService.findAll(null));

		verify(deviceRepository, never()).findByTypeOrderByNameAsc(any());
	}

	@Test
	void findAllWithTypeFiltersByType() {
		List<Device> devices = List.of(new Device("Lamp", DeviceType.LAMP, "token", NOW));
		when(deviceRepository.findByTypeOrderByNameAsc(DeviceType.LAMP)).thenReturn(devices);

		assertEquals(devices, deviceService.findAll(DeviceType.LAMP));

		verify(deviceRepository, never()).findAllWithAccesses();
	}

	@Test
	void getWithAccessesReturnsDevice() {
		UUID deviceId = UUID.randomUUID();
		Device device = new Device("Kettle", DeviceType.KETTLE, "token", NOW);
		when(deviceRepository.findByIdWithAccesses(deviceId)).thenReturn(Optional.of(device));

		assertEquals(device, deviceService.getWithAccesses(deviceId));
	}

	@Test
	void getWithAccessesThrowsWhenDeviceMissing() {
		UUID deviceId = UUID.randomUUID();
		when(deviceRepository.findByIdWithAccesses(deviceId)).thenReturn(Optional.empty());

		assertThrows(DeviceNotFoundException.class, () -> deviceService.getWithAccesses(deviceId));
	}

	@Test
	void renameChangesDeviceName() {
		UUID deviceId = UUID.randomUUID();
		Device device = new Device("Old name", DeviceType.LAMP, "token", NOW);
		when(deviceRepository.findByIdWithAccesses(deviceId)).thenReturn(Optional.of(device));

		Device result = deviceService.rename(deviceId, "New name");

		assertEquals("New name", result.getName());
	}

	@Test
	void renameThrowsWhenDeviceMissing() {
		UUID deviceId = UUID.randomUUID();
		when(deviceRepository.findByIdWithAccesses(deviceId)).thenReturn(Optional.empty());

		assertThrows(DeviceNotFoundException.class, () -> deviceService.rename(deviceId, "New name"));
	}

	@Test
	void deleteRemovesExistingDevice() {
		UUID deviceId = UUID.randomUUID();
		Device device = new Device("Kettle", DeviceType.KETTLE, "token", NOW);
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

		deviceService.delete(deviceId);

		verify(deviceRepository).delete(device);
	}

	@Test
	void deleteThrowsWhenDeviceMissing() {
		UUID deviceId = UUID.randomUUID();
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

		assertThrows(DeviceNotFoundException.class, () -> deviceService.delete(deviceId));

		verify(deviceRepository, never()).delete(any());
	}

	private static User userWithId(UUID id) {
		User user = new User("olena@example.com", "hash", "Олена", NOW);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
