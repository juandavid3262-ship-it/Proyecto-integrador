package dev.antoniochacon.repository;

import dev.antoniochacon.model.entity.Reparacion;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReparacionRepository extends JpaRepository<Reparacion, Long> {

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.modelo", "mecanico"})
    List<Reparacion> findAllByOrderByFechaDescIdDesc();

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.modelo", "mecanico"})
    List<Reparacion> findByMecanicoIdOrderByFechaDescIdDesc(Long mecanicoId);

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.modelo", "mecanico"})
    List<Reparacion> findByFechaBetweenOrderByFechaDescIdDesc(LocalDate desde, LocalDate hasta);

    boolean existsByVehiculoId(Long vehiculoId);

    @Query("""
            select r.estado, count(r)
            from Reparacion r
            where r.fecha between :desde and :hasta
            group by r.estado
            """)
    List<Object[]> contarPorEstado(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            select year(r.fecha), month(r.fecha), r.tipo, count(r)
            from Reparacion r
            where r.fecha between :desde and :hasta
            group by year(r.fecha), month(r.fecha), r.tipo
            order by year(r.fecha), month(r.fecha), r.tipo
            """)
    List<Object[]> contarPorMesYTipo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
