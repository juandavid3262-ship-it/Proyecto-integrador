package dev.nakano.projects.service;

import dev.nakano.projects.dto.ProjectDTO;
import dev.nakano.projects.entity.Project;
import dev.nakano.projects.exception.BusinessException;
import dev.nakano.projects.exception.ResourceNotFoundException;
import dev.nakano.projects.mapper.ProjectMapper;
import dev.nakano.projects.repository.ProjectRepository;
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
        validarNombreVacio(projectDTO.getName());
        validarDuplicidad(projectDTO.getName(), null);
        Project project = ProjectMapper.toEntity(projectDTO);
        return projectRepository.save(project);
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
        validarNombreVacio(projectDTO.getName());
        Project project = findProjectById(id);
        validarDuplicidad(projectDTO.getName(), id);
        ProjectMapper.updateEntity(project, projectDTO);
        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        Project project = findProjectById(id);
        projectRepository.delete(project);
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
