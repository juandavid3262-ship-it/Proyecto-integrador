package dev.juanmateus.bibliotech.controller;

import dev.juanmateus.bibliotech.model.entity.Genre;
import dev.juanmateus.bibliotech.service.GenreService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/genres")
public class AdminGenreController {
    private final GenreService service;

    public AdminGenreController(GenreService service) {
        this.service = service;
    }

    @GetMapping
    public String getGenreTemplate(Model model) {
        List<Genre> genres = service.findAllGenres();

        model.addAttribute("genres", genres);

        //esta es la ruta de la plantilla a devolver
        return "genres/list";
    }

    @GetMapping("/create")
    public String getCreateGenreTemplate() {
        return "genres/create";
    }

    @PostMapping
    public String createGnere(@RequestParam("name") String genreName, Model model) {
        try {
            service.createGenre(genreName);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("error", "El género '" + genreName + "' ya existe");
            return "genres/create";
        }
        return "redirect:/genres";
    }

    @GetMapping("/edit/{id}")
    public String getUpdateGenreTemplate(@PathVariable Long id, Model model) {
        Optional<Genre> foundGenre = service.findGenreById(id);

        // si no se encontro el genero buscado
        if (foundGenre.isEmpty()) {
            // todo: mostrar una pagina de error amigable
            throw new EntityNotFoundException("No se encontro el genero buscado");
        }

        // se pasa el Genre (no el Optional) a la vista
        model.addAttribute("genre", foundGenre.get());
        return "genres/update";
    }

    @PutMapping("/{id}")
    public String updateGenre(@PathVariable Long id, @ModelAttribute("genre") Genre genre) {
        service.updateGenre(id, genre.getName());

        return "redirect:/genres";
    }

    @GetMapping("/delete/{id}")
    public String getDeleteGenreTemplate(@PathVariable Long id, Model model) {
        Optional<Genre> foundGenre = service.findGenreById(id);

        if (foundGenre.isEmpty()) {
            // lanzar el error feo de spring
            throw new EntityNotFoundException("No se encontro el genero buscado");
        }

        model.addAttribute("genre", foundGenre.get());

        return "genres/delete";
    }

    @DeleteMapping("/{id}")
    public String deleteGenre(@PathVariable Long id) {
        service.deleteGenre(id);
        return "redirect:/genres";
    }
}