package server.devices;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    @Query("SELECT DISTINCT d FROM Device d LEFT JOIN FETCH d.accesses")
    List<Device> findAllWithAccesses();

    @Query("SELECT DISTINCT d FROM Device d LEFT JOIN FETCH d.accesses WHERE d.id = :id")
    Optional<Device> findByIdWithAccesses(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"accesses"})
    List<Device> findByTypeOrderByNameAsc(DeviceType type);

    default List<Device> findAll(DeviceType type) {
        return type == null ? findAllWithAccesses() : findByTypeOrderByNameAsc(type);
    }

    default Optional<Device> getWithAccesses(UUID id) {
        return findByIdWithAccesses(id);
    }
}