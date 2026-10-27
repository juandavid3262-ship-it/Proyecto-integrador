package dev.antoniochacon.repository;

import dev.antoniochacon.model.entity.Modelo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModeloRepository extends JpaRepository<Modelo, Long> {

    List<Modelo> findAllByOrderByMarcaAscNombreAsc();

    boolean existsByNombreIgnoreCaseAndMarcaIgnoreCase(String nombre, String marca);

    boolean existsByNombreIgnoreCaseAndMarcaIgnoreCaseAndIdNot(String nombre, String marca, Long id);

    Optional<Modelo> findByNombreIgnoreCaseAndMarcaIgnoreCase(String nombre, String marca);
}
