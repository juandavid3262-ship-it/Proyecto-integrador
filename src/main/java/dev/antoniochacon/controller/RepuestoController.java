package dev.antoniochacon.controller;

import dev.antoniochacon.model.dto.RepuestoForm;
import dev.antoniochacon.model.entity.Modelo;
import dev.antoniochacon.service.ModeloService;
import dev.antoniochacon.service.ReglaNegocioException;
import dev.antoniochacon.service.RepuestoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/repuestos")
public class RepuestoController {

    private final RepuestoService repuestos;
    private final ModeloService modelos;

    public RepuestoController(RepuestoService repuestos, ModeloService modelos) {
        this.repuestos = repuestos;
        this.modelos = modelos;
    }

    @GetMapping
    public String listar(Model model) {
        preparar(model);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RepuestoForm());
        }
        return "repuestos/lista";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        preparar(model);
        model.addAttribute("form", repuestos.aFormulario(repuestos.obtener(id)));
        return "repuestos/lista";
    }

    @GetMapping("/compatibles")
    public String compatibles(@RequestParam(required = false) Long modeloId, Model model) {
        model.addAttribute("catalogoModelos", modelos.listar());
        model.addAttribute("modeloId", modeloId);
        if (modeloId != null) {
            try {
                Modelo modelo = modelos.obtener(modeloId);
                model.addAttribute("modelo", modelo);
                model.addAttribute("repuestos", repuestos.buscarCompatibles(modeloId));
            } catch (ReglaNegocioException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        return "repuestos/compatibles";
    }

    @PostMapping
    public String crear(
            @Valid @ModelAttribute("form") RepuestoForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect
    ) {
        if (binding.hasErrors()) {
            preparar(model);
            return "repuestos/lista";
        }
        try {
            repuestos.crear(form);
            redirect.addFlashAttribute("exito", "Repuesto guardado");
            return "redirect:/repuestos";
        } catch (ReglaNegocioException ex) {
            binding.reject("negocio", ex.getMessage());
            preparar(model);
            return "repuestos/lista";
        }
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") RepuestoForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect
    ) {
        if (binding.hasErrors()) {
            preparar(model);
            return "repuestos/lista";
        }
        try {
            repuestos.actualizar(id, form);
            redirect.addFlashAttribute("exito", "Repuesto actualizado");
            return "redirect:/repuestos";
        } catch (ReglaNegocioException ex) {
            binding.reject("negocio", ex.getMessage());
            preparar(model);
            return "repuestos/lista";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            repuestos.eliminar(id);
            redirect.addFlashAttribute("exito", "Repuesto eliminado");
        } catch (ReglaNegocioException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/repuestos";
    }

    private void preparar(Model model) {
        model.addAttribute("repuestos", repuestos.listar());
        model.addAttribute("catalogoModelos", modelos.listar());
    }
}
