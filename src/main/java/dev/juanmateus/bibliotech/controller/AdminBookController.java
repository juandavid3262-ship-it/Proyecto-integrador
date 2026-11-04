package dev.juanmateus.bibliotech.controller;


import dev.juanmateus.bibliotech.service.BookInventoryService;
import dev.juanmateus.bibliotech.model.constant.BookFormat;
import dev.juanmateus.bibliotech.model.dto.BookDTO;
import dev.juanmateus.bibliotech.service.GenreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/books")
public class AdminBookController {
    private final BookInventoryService inventoryService;
    private final GenreService genreService;

    public AdminBookController(BookInventoryService inventoryService, GenreService genreService) {
        this.inventoryService = inventoryService;
        this.genreService = genreService;
    }

    @GetMapping
    public String getBookListTemplate(Model model) {
        model.addAttribute("books", inventoryService.findAllBooks());
        return "books/list";
    }
    @GetMapping ("/create")
    public String getCreateBookTemplate (Model model){
        model.addAttribute("bookRequest", BookDTO.class);
        model.addAttribute("genres", genreService.findAllGenres());
        model.addAttribute("formats", BookFormat.values());
        return "books/create";
    }

    @PostMapping
    public String createBook(@ModelAttribute("book") BookDTO bookRequest) {
        inventoryService.createBook(bookRequest);
        return "redirect:/books";
    }
}