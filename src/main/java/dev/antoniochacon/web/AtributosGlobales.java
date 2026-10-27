package dev.antoniochacon.web;

import dev.antoniochacon.model.dto.PreferenciasDTO;
import dev.antoniochacon.model.entity.Usuario;
import dev.antoniochacon.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Component
public class AtributosGlobales {

    private final UsuarioRepository usuarios;

    public AtributosGlobales(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        if ("anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return usuarios.findByEmail(auth.getName()).orElse(null);
    }

    @ModelAttribute("prefs")
    public PreferenciasDTO prefs() {
        Usuario usuario = usuarioActual();
        if (usuario == null) {
            return PreferenciasDTO.defecto();
        }
        return new PreferenciasDTO(usuario.getTemaPreferido().name(), usuario.isSidebarPlegada());
    }
}
