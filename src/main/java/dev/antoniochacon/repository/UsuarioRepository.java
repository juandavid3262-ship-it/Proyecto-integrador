package dev.antoniochacon.repository;

import dev.antoniochacon.model.entity.Usuario;
import dev.antoniochacon.model.enums.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByIdAndRol(Long id, Rol rol);

    List<Usuario> findByRolOrderByNombreAsc(Rol rol);
}
