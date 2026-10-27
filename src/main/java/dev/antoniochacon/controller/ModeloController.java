package dev.antoniochacon.controller;

import dev.antoniochacon.model.dto.ModeloForm;
import dev.antoniochacon.service.ModeloService;
import dev.antoniochacon.service.ReglaNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/modelos")
public class ModeloController {

    private final ModeloService modelos;

    public ModeloController(ModeloService modelos) {
        this.modelos = modelos;
    }

    @GetMapping
    public String listar(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ModeloForm());
        }
        model.addAttribute("modelos", modelos.listar());
        return "modelos/lista";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", modelos.aFormulario(modelos.obtener(id)));
        model.addAttribute("modelos", modelos.listar());
        return "modelos/lista";
    }

    @PostMapping
    public String crear(
            @Valid @ModelAttribute("form") ModeloForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect
    ) {
        if (binding.hasErrors()) {
            model.addAttribute("modelos", modelos.listar());
            return "modelos/lista";
        }
        try {
            modelos.crear(form);
            redirect.addFlashAttribute("exito", "Modelo guardado");
            return "redirect:/modelos";
        } catch (ReglaNegocioException ex) {
            binding.reject("negocio", ex.getMessage());
            model.addAttribute("modelos", modelos.listar());
            return "modelos/lista";
        }
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ModeloForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect
    ) {
        if (binding.hasErrors()) {
            model.addAttribute("modelos", modelos.listar());
            return "modelos/lista";
        }
        try {
            modelos.actualizar(id, form);
            redirect.addFlashAttribute("exito", "Modelo actualizado");
            return "redirect:/modelos";
        } catch (ReglaNegocioException ex) {
            binding.reject("negocio", ex.getMessage());
            model.addAttribute("modelos", modelos.listar());
            return "modelos/lista";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            modelos.eliminar(id);
            redirect.addFlashAttribute("exito", "Modelo eliminado");
        } catch (ReglaNegocioException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/modelos";
    }
}
