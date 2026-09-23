package dev.nakano.projects.mapper;

import dev.nakano.projects.dto.ProjectDTO;
import dev.nakano.projects.dto.TodoDTO;
import dev.nakano.projects.entity.Project;
import dev.nakano.projects.entity.Todo;
import java.util.List;
import java.util.stream.Collectors;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectDTO toDTO(Project project) {
        List<TodoDTO> todos = project.getTodos().stream()
                .map(ProjectMapper::toTodoDTO)
                .collect(Collectors.toList());
        return new ProjectDTO(project.getName(), todos);
    }

    public static TodoDTO toTodoDTO(Todo todo) {
        return new TodoDTO(todo.getTitle(), todo.isCompleted());
    }

    public static Project toEntity(ProjectDTO dto) {
        Project project = new Project(dto.getName());
        if (dto.getTodos() != null) {
            for (TodoDTO todoDTO : dto.getTodos()) {
                project.addTodo(toTodoEntity(todoDTO));
            }
        }
        return project;
    }

    public static Todo toTodoEntity(TodoDTO dto) {
        return new Todo(dto.getTitle(), dto.isCompleted());
    }

    public static void updateEntity(Project project, ProjectDTO dto) {
        project.setName(dto.getName());
        project.clearTodos();
        if (dto.getTodos() != null) {
            for (TodoDTO todoDTO : dto.getTodos()) {
                project.addTodo(toTodoEntity(todoDTO));
            }
        }
    }
}
