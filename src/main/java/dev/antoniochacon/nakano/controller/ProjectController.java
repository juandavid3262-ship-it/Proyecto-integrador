package dev.antoniochacon.nakano.controller;

import dev.antoniochacon.nakano.model.dto.ProjectDTO;
import dev.antoniochacon.nakano.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectDTO> findAll() {
        return projectService.findAllProjects();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDTO create(@RequestBody ProjectDTO dto) {
        return projectService.createProject(dto);
    }

    @PutMapping("/{id}")
    public ProjectDTO update(@PathVariable Long id, @RequestBody ProjectDTO dto) {
        return projectService.updateProject(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.deleteProject(id);
    }
}
