package server.executions;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import server.commands.Command;
import server.commands.RequiredRole;
import server.devices.Device;
import server.devices.DeviceType;
import server.users.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CommandExecutionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommandExecutionRepository repository;

    private Statistics statistics;
    private User owner;
    private User guest;
    private Device lamp;
    private Command setBrightness;
    private Command turnOn;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();

        owner = em.persist(new User("owner@example.com", "hash", "Власник", NOW));
        guest = em.persist(new User("guest@example.com", "hash", "Гість", NOW));
        lamp = em.persist(new Device("Лампа", DeviceType.LAMP, "token-lamp", NOW));
        setBrightness = em.persist(new Command(lamp, "set_brightness", "{}", RequiredRole.GUEST, NOW));
        turnOn = em.persist(new Command(lamp, "turn_on", "{}", RequiredRole.GUEST, NOW));
    }

    private CommandExecution persistExecution(Command command, User executedBy, Map<String, Object> args, int minutesAfter) {
        return em.persist(new CommandExecution(
                command, command.getDevice(), executedBy, args, NOW.plus(minutesAfter, ChronoUnit.MINUTES)));
    }

    @Test
    void argsAreStoredAsJsonAndReadBack() {
        CommandExecution saved = persistExecution(
                setBrightness, owner, Map.of("brightness", 80, "mode", "warm"), 0);
        em.flush();
        em.clear();

        CommandExecution loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getArgs())
                .containsEntry("brightness", 80)
                .containsEntry("mode", "warm");
        assertThat(loaded.getStatus()).isEqualTo(ExecutionStatus.PENDING);
    }

    @Test
    void statusTransitionIsPersisted() {
        CommandExecution saved = persistExecution(turnOn, owner, Map.of(), 0);
        saved.transitionTo(ExecutionStatus.RUNNING).transitionTo(ExecutionStatus.SUCCESS);
        em.flush();
        em.clear();

        CommandExecution loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(loaded.getCompletedAt()).isNotNull();
    }

    @Test
    void journalLoadsCommandsAndUsersInSingleQuery() {
        persistExecution(setBrightness, owner, Map.of("brightness", 10), 0);
        persistExecution(turnOn, guest, Map.of(), 1);
        persistExecution(setBrightness, guest, Map.of("brightness", 90), 2);
        persistExecution(turnOn, null, Map.of(), 3);
        em.flush();
        em.clear();
        statistics.clear();

        List<CommandExecution> journal = repository.findJournalByDeviceId(lamp.getId());
        journal.forEach(e -> {
            e.getCommand().getName();
            if (e.getExecutedBy() != null) {
                e.getExecutedBy().getEmail();
            }
        });

        assertThat(journal).hasSize(4);
        assertThat(journal).extracting(CommandExecution::getRequestedAt).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void guestJournalContainsOnlyOwnExecutions() {
        persistExecution(setBrightness, owner, Map.of("brightness", 10), 0);
        CommandExecution first = persistExecution(turnOn, guest, Map.of(), 1);
        CommandExecution second = persistExecution(setBrightness, guest, Map.of("brightness", 90), 2);
        em.flush();
        em.clear();

        List<CommandExecution> journal =
                repository.findByDeviceIdAndExecutedByIdOrderByRequestedAtDesc(lamp.getId(), guest.getId());

        assertThat(journal).extracting(CommandExecution::getId).containsExactly(second.getId(), first.getId());
    }

    @Test
    void findByIdAndDeviceIdIgnoresExecutionOfAnotherDevice() {
        Device kettle = em.persist(new Device("Чайник", DeviceType.KETTLE, "token-kettle", NOW));
        CommandExecution execution = persistExecution(turnOn, owner, Map.of(), 0);
        em.flush();
        em.clear();

        assertThat(repository.findByIdAndDeviceId(execution.getId(), lamp.getId())).isPresent();
        assertThat(repository.findByIdAndDeviceId(execution.getId(), kettle.getId())).isEmpty();
    }

    @Test
    void deletingCommandDeletesItsExecutions() {
        persistExecution(setBrightness, owner, Map.of("brightness", 10), 0);
        CommandExecution kept = persistExecution(turnOn, owner, Map.of(), 1);
        em.flush();
        em.clear();

        em.remove(em.find(Command.class, setBrightness.getId()));
        em.flush();
        em.clear();

        assertThat(repository.findAll()).extracting(CommandExecution::getId).containsExactly(kept.getId());
    }

    @Test
    void deletingUserKeepsExecutionWithoutExecutor() {
        CommandExecution execution = persistExecution(turnOn, guest, Map.of(), 0);
        em.flush();
        em.clear();

        em.remove(em.find(User.class, guest.getId()));
        em.flush();
        em.clear();

        CommandExecution loaded = repository.findById(execution.getId()).orElseThrow();
        assertThat(loaded.getExecutedBy()).isNull();
    }
}
