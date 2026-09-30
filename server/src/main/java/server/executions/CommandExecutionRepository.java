package server.executions;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommandExecutionRepository extends ListCrudRepository<CommandExecution, UUID> {

    @Query("""
            SELECT e FROM CommandExecution e
            JOIN FETCH e.command
            LEFT JOIN FETCH e.executedBy
            WHERE e.device.id = :deviceId
            ORDER BY e.requestedAt DESC
            """)
    List<CommandExecution> findJournalByDeviceId(@Param("deviceId") UUID deviceId);

    @EntityGraph(attributePaths = {"command", "executedBy"})
    List<CommandExecution> findByDeviceIdAndExecutedByIdOrderByRequestedAtDesc(UUID deviceId, UUID executedById);

    @EntityGraph(attributePaths = {"command", "executedBy"})
    Optional<CommandExecution> findByIdAndDeviceId(UUID id, UUID deviceId);
}
