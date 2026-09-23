package dev.antoniochacon.projects.model.dto;

import java.util.List;

public record ProjectDTO(
        String name,
        List<TodoDTO> todos
) {
}
