package dev.nakano.service;

import dev.nakano.model.dto.ProjectDTO;
import dev.nakano.model.dto.TodoDTO;
import dev.nakano.model.entity.Project;
import dev.nakano.model.entity.Todo;
import dev.nakano.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO dto) {
        validarNombreVacio(dto.name());
        validarDuplicidad(dto.name(), null);

        Project project = new Project();
        project.setName(dto.name().trim());
        agregarTodos(project, dto.todos());
        return toDTO(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> findAllProjects() {
        return projectRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        validarNombreVacio(dto.name());
        validarDuplicidad(dto.name(), id);

        project.setName(dto.name().trim());
        project.getTodos().clear();
        agregarTodos(project, dto.todos());
        return toDTO(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        projectRepository.deleteById(id);
    }

    // ---------- Validaciones ----------

    private void validarDuplicidad(String name, Long id) {
        boolean existe = (id == null)
                ? projectRepository.existsByNameIgnoreCase(name.trim())
                : projectRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), id);
        if (existe) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proyecto con ese nombre");
        }
    }

    private void validarNombreVacio(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no puede estar vacío");
        }
    }

    // ---------- Conversión entre entidad y DTO ----------

    private void agregarTodos(Project project, List<TodoDTO> todos) {
        if (todos == null) return;
        for (TodoDTO t : todos) {
            Todo todo = new Todo();
            todo.setTitle(t.title());
            todo.setCompleted(t.isCompleted());
            project.getTodos().add(todo);
        }
    }

    private ProjectDTO toDTO(Project project) {
        List<TodoDTO> todos = project.getTodos().stream()
                .map(t -> new TodoDTO(t.getTitle(), t.isCompleted()))
                .toList();
        return new ProjectDTO(project.getName(), todos);
    }
}