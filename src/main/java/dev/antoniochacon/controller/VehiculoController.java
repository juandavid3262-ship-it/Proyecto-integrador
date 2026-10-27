package dev.antoniochacon.controller;

import dev.antoniochacon.model.dto.VehiculoForm;
import dev.antoniochacon.model.entity.Vehiculo;
import dev.antoniochacon.model.enums.ColorPlaca;
import dev.antoniochacon.service.ModeloService;
import dev.antoniochacon.service.ReglaNegocioException;
import dev.antoniochacon.service.VehiculoService;
import jakarta.validation.Valid;
import java.util.Optional;
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
@RequestMapping("/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculos;
    private final ModeloService modelos;

    public VehiculoController(VehiculoService vehiculos, ModeloService modelos) {
        this.vehiculos = vehiculos;
        this.modelos = modelos;
    }

    @GetMapping
    public String listar(Model model) {
        preparar(model);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new VehiculoForm());
        }
        return "vehiculos/lista";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        preparar(model);
        model.addAttribute("form", vehiculos.aFormulario(vehiculos.obtener(id)));
        return "vehiculos/lista";
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam(required = false) String placa, Model model) {
        model.addAttribute("placa", placa == null ? "" : placa);
        if (placa == null || placa.isBlank()) {
            return "vehiculos/buscar";
        }
        try {
            Optional<Vehiculo> encontrado = vehiculos.buscarModeloPorPlaca(placa);
            if (encontrado.isEmpty()) {
                model.addAttribute("mensaje", "Vehículo no encontrado");
            } else {
                model.addAttribute("vehiculo", encontrado.get());
            }
        } catch (ReglaNegocioException ex) {
            model.addAttribute("mensaje", ex.getMessage());
        }
        return "vehiculos/buscar";
    }

    @PostMapping
    public String crear(
            @Valid @ModelAttribute("form") VehiculoForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect
    ) {
        if (binding.hasErrors()) {
            preparar(model);
            return "vehiculos/lista";
        }
        try {
            vehiculos.crear(form);
            redirect.addFlashAttribute("exito", "Vehículo guardado");
            return "redirect:/vehiculos";
        } catch (ReglaNegocioException ex) {
            binding.reject("negocio", ex.getMessage());
            preparar(model);
            return "vehiculos/lista";
        }
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") VehiculoForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect
    ) {
        if (binding.hasErrors()) {
            preparar(model);
            return "vehiculos/lista";
        }
        try {
            vehiculos.actualizar(id, form);
            redirect.addFlashAttribute("exito", "Vehículo actualizado");
            return "redirect:/vehiculos";
        } catch (ReglaNegocioException ex) {
            binding.reject("negocio", ex.getMessage());
            preparar(model);
            return "vehiculos/lista";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            vehiculos.eliminar(id);
            redirect.addFlashAttribute("exito", "Vehículo eliminado");
        } catch (ReglaNegocioException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/vehiculos";
    }

    private void preparar(Model model) {
        model.addAttribute("vehiculos", vehiculos.listar());
        model.addAttribute("catalogoModelos", modelos.listar());
        model.addAttribute("colores", ColorPlaca.values());
    }
}
