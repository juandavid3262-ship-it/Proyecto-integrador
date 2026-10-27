package dev.antoniochacon.controller;

import dev.antoniochacon.model.dto.ReparacionForm;
import dev.antoniochacon.model.entity.Usuario;
import dev.antoniochacon.model.enums.EstadoReparacion;
import dev.antoniochacon.model.enums.Rol;
import dev.antoniochacon.repository.UsuarioRepository;
import dev.antoniochacon.service.AccesoDenegadoNegocioException;
import dev.antoniochacon.service.ReglaNegocioException;
import dev.antoniochacon.service.ReparacionService;
import dev.antoniochacon.service.VehiculoService;
import dev.antoniochacon.web.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reparaciones")
public class ReparacionController {

    private final ReparacionService reparaciones;
    private final VehiculoService vehiculos;
    private final UsuarioRepository usuarios;
    private final UsuarioActual actor;

    public ReparacionController(
            ReparacionService reparaciones,
            VehiculoService vehiculos,
            UsuarioRepository usuarios,
            UsuarioActual actor
    ) {
        this.reparaciones = reparaciones;
        this.vehiculos = vehiculos;
        this.usuarios = usuarios;
        this.actor = actor;
    }

    @GetMapping
    public String listar(Model model) {
        preparar(model);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ReparacionForm());
        }
        return "reparaciones/lista";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        preparar(model);
        try {
            model.addAttribute("form", reparaciones.aFormulario(reparaciones.obtenerEditable(id, actor.requerir())));
        } catch (AccesoDenegadoNegocioException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (ReglaNegocioException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        return "reparaciones/lista";
    }

    @PostMapping
    public String crear(
            @Valid @ModelAttribute("form") ReparacionForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect
    ) {
        if (binding.hasErrors()) {
            preparar(model);
            return "reparaciones/lista";
        }
        try {
            reparaciones.crear(form, actor.requerir());
            redirect.addFlashAttribute("exito", "Reparación guardada");
            return "redirect:/reparaciones";
        } catch (AccesoDenegadoNegocioException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (ReglaNegocioException ex) {
            binding.reject("negocio", ex.getMessage());
            preparar(model);
            return "reparaciones/lista";
        }
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ReparacionForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect
    ) {
        if (binding.hasErrors()) {
            preparar(model);
            return "reparaciones/lista";
        }
        try {
            reparaciones.actualizar(id, form, actor.requerir());
            redirect.addFlashAttribute("exito", "Reparación actualizada");
            return "redirect:/reparaciones";
        } catch (AccesoDenegadoNegocioException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (ReglaNegocioException ex) {
            binding.reject("negocio", ex.getMessage());
            preparar(model);
            return "reparaciones/lista";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reparaciones.eliminar(id, actor.requerir());
            redirect.addFlashAttribute("exito", "Reparación eliminada");
        } catch (AccesoDenegadoNegocioException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (ReglaNegocioException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reparaciones";
    }

    private void preparar(Model model) {
        Usuario usuario = actor.requerir();
        model.addAttribute("reparaciones", reparaciones.listar(usuario));
        model.addAttribute("catalogoVehiculos", vehiculos.listar());
        model.addAttribute("mecanicos", usuarios.findByRolOrderByNombreAsc(Rol.MECANICO));
        model.addAttribute("estados", EstadoReparacion.values());
        model.addAttribute("esAdmin", usuario.getRol() == Rol.ADMINISTRADOR);
    }
}
