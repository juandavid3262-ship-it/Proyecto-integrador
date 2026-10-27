package dev.antoniochacon.web;

import dev.antoniochacon.model.entity.Usuario;
import dev.antoniochacon.service.ReglaNegocioException;
import dev.antoniochacon.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {

    private final UsuarioService usuarios;

    public UsuarioActual(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario requerir() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ReglaNegocioException("Debes iniciar sesión");
        }
        return usuarios.requerir(auth.getName());
    }
}
