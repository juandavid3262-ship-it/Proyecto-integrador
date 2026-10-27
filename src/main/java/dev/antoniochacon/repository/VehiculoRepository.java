package dev.antoniochacon.repository;

import dev.antoniochacon.model.entity.Vehiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    @EntityGraph(attributePaths = "modelo")
    List<Vehiculo> findAllByOrderByPlacaAsc();

    @EntityGraph(attributePaths = "modelo")
    Optional<Vehiculo> findByPlaca(String placa);

    boolean existsByPlaca(String placa);

    boolean existsByPlacaAndIdNot(String placa, Long id);

    boolean existsByModeloId(Long modeloId);
}
