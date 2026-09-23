package dev.antoniochacon.projects.service;

import dev.antoniochacon.projects.exception.BusinessException;
import dev.antoniochacon.projects.exception.ResourceNotFoundException;
import dev.antoniochacon.projects.model.dto.ProjectDTO;
import dev.antoniochacon.projects.model.dto.TodoDTO;
import dev.antoniochacon.projects.model.entity.Project;
import dev.antoniochacon.projects.model.entity.Todo;
import dev.antoniochacon.projects.repository.ProjectRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project createProject(ProjectDTO projectDTO) {
        validarNombreVacio(projectDTO.name());
        validarDuplicidad(projectDTO.name(), null);
        return projectRepository.save(toEntity(projectDTO));
    }

    @Transactional(readOnly = true)
    public List<Project> findAllProjects() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project no encontrado con id: " + id));
    }

    public Project updateProject(Long id, ProjectDTO projectDTO) {
        validarNombreVacio(projectDTO.name());
        Project project = findProjectById(id);
        validarDuplicidad(projectDTO.name(), id);
        updateEntity(project, projectDTO);
        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        Project project = findProjectById(id);
        projectRepository.delete(project);
    }

    private Project toEntity(ProjectDTO dto) {
        Project project = new Project(dto.name());
        if (dto.todos() != null) {
            for (TodoDTO todoDTO : dto.todos()) {
                project.addTodo(new Todo(todoDTO.title(), todoDTO.isCompleted()));
            }
        }
        return project;
    }

    private void updateEntity(Project project, ProjectDTO dto) {
        project.setName(dto.name());
        project.clearTodos();
        if (dto.todos() != null) {
            for (TodoDTO todoDTO : dto.todos()) {
                project.addTodo(new Todo(todoDTO.title(), todoDTO.isCompleted()));
            }
        }
    }

    private void validarNombreVacio(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("El nombre del proyecto no puede estar vacío");
        }
    }

    private void validarDuplicidad(String name, Long excludeId) {
        boolean duplicated = excludeId == null
                ? projectRepository.existsByNameIgnoreCase(name.trim())
                : projectRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), excludeId);

        if (duplicated) {
            throw new BusinessException("Ya existe un proyecto con el nombre: " + name.trim());
        }
    }
}
