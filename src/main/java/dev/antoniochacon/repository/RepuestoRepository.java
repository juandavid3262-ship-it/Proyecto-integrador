package dev.antoniochacon.repository;

import dev.antoniochacon.model.entity.Repuesto;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {

    @EntityGraph(attributePaths = "modelo")
    List<Repuesto> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "modelo")
    List<Repuesto> findByModeloIdOrderByNombreAsc(Long modeloId);

    @EntityGraph(attributePaths = "modelo")
    List<Repuesto> findByStockLessThanOrderByStockAsc(int stock);

    boolean existsByCodigoEan(String codigoEan);

    boolean existsByCodigoEanAndIdNot(String codigoEan, Long id);

    boolean existsByModeloId(Long modeloId);
}
