package dev.nakano.projects.dto;

import java.util.ArrayList;
import java.util.List;

public class ProjectDTO {

    private String name;
    private List<TodoDTO> todos = new ArrayList<>();

    public ProjectDTO() {
    }

    public ProjectDTO(String name, List<TodoDTO> todos) {
        this.name = name;
        this.todos = todos != null ? todos : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TodoDTO> getTodos() {
        return todos;
    }

    public void setTodos(List<TodoDTO> todos) {
        this.todos = todos;
    }
}
