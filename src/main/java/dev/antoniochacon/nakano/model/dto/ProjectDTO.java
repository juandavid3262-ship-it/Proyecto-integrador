package dev.antoniochacon.nakano.model.dto;

import java.util.List;

public record ProjectDTO(String name, List<TodoDTO> todos) {

}