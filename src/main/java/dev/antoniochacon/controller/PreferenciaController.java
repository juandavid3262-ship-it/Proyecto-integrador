package dev.antoniochacon.controller;

import dev.antoniochacon.model.dto.PreferenciasDTO;
import dev.antoniochacon.service.ReglaNegocioException;
import dev.antoniochacon.service.UsuarioService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferencias")
public class PreferenciaController {

    private final UsuarioService usuarios;

    public PreferenciaController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PatchMapping("/sidebar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sidebar(@RequestBody SidebarRequest body, Principal principal) {
        usuarios.actualizarSidebar(principal.getName(), body.plegada());
    }

    @PatchMapping("/tema")
    public ResponseEntity<PreferenciasDTO> tema(@RequestBody TemaRequest body, Principal principal) {
        try {
            return ResponseEntity.ok(usuarios.cambiarTema(principal.getName(), body.tema()));
        } catch (ReglaNegocioException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record SidebarRequest(boolean plegada) {
    }

    public record TemaRequest(String tema) {
    }
}
