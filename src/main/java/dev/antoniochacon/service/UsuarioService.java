package dev.antoniochacon.service;

import dev.antoniochacon.model.entity.Usuario;
import dev.antoniochacon.model.enums.Tema;
import dev.antoniochacon.model.dto.PreferenciasDTO;
import dev.antoniochacon.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarios.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    public Usuario requerir(String email) {
        return usuarios.findByEmail(email)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
    }

    public PreferenciasDTO obtenerPreferencias(String email) {
        Usuario usuario = requerir(email);
        return new PreferenciasDTO(usuario.getTemaPreferido().name(), usuario.isSidebarPlegada());
    }

    @Transactional
    public void actualizarSidebar(String email, boolean plegada) {
        Usuario usuario = requerir(email);
        usuario.setSidebarPlegada(plegada);
        usuarios.save(usuario);
    }

    @Transactional
    public PreferenciasDTO cambiarTema(String email, String tema) {
        Tema valor = validarTema(tema);
        Usuario usuario = requerir(email);
        usuario.setTemaPreferido(valor);
        usuarios.save(usuario);
        return new PreferenciasDTO(valor.name(), usuario.isSidebarPlegada());
    }

    private Tema validarTema(String tema) {
        if (tema == null) {
            throw new ReglaNegocioException("El tema debe ser CLARO u OSCURO");
        }
        try {
            return Tema.valueOf(tema.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException("El tema debe ser CLARO u OSCURO");
        }
    }
}
