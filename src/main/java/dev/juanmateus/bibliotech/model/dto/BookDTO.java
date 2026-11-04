package dev.juanmateus.bibliotech.model.dto;

import dev.juanmateus.bibliotech.model.constant.BookFormat;

public record BookDTO(
        String isbn,
        String title,
        String author,
        String genre,
        BookFormat format,
        Integer quantity
) {}
