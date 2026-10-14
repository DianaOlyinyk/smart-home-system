package server.devices.internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceRepository;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.users.UserAccount;
import server.users.UserService;
import java.time.Instant;

@Component
class SeedDataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);
    private static final String OWNER_EMAIL = "owner@example.com";
    private final UserService userService;
    private final DeviceService deviceService;
    private final DeviceRepository deviceRepository;

    SeedDataLoader(UserService userService,
                   DeviceService deviceService,
                   DeviceRepository deviceRepository) {
        this.userService = userService;
        this.deviceService = deviceService;
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (alreadySeeded()) {
            log.info("Seed data already present, skipping");
            return;
        }
        UserAccount owner = userService.register(OWNER_EMAIL, "Passw0rd123", "Owner");
        UserAccount guest = userService.register("guest@example.com", "Passw0rd123", "Guest");
        UserAccount stranger = userService.register("stranger@example.com", "Passw0rd123", "Stranger");
        Device lamp = deviceService.create("Seed Lamp", DeviceType.LAMP, owner.id());
        Device device = deviceRepository.findByIdWithAccesses(lamp.getId())
                .orElseThrow(() -> new DeviceNotFoundException(lamp.getId()));
        device.grantAccess(userService.getUser(guest.id()), AccessRole.GUEST,
                userService.getUser(owner.id()), Instant.now());
        deviceRepository.save(device);
        log.info("Seed user: {}", owner.email());
        log.info("Seed user: {}", guest.email());
        log.info("Seed user: {}", stranger.email());
        log.info("Seed device id: {}", lamp.getId());
    }

    private boolean alreadySeeded() {
        return userService.findAll(OWNER_EMAIL).stream()
                .anyMatch(user -> OWNER_EMAIL.equalsIgnoreCase(user.email()));
    }
}