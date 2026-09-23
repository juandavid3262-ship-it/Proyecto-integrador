package dev.nakano.projects.controller;

import dev.nakano.projects.dto.ProjectDTO;
import dev.nakano.projects.entity.Project;
import dev.nakano.projects.service.ProjectService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Lista todos los proyectos con sus atributos.
     * GET http://localhost:8081/projects
     */
    @GetMapping
    public ResponseEntity<List<Project>> listarTodosLosProyectos() {
        return ResponseEntity.ok(projectService.findAllProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findProjectById(id));
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody ProjectDTO projectDTO) {
        Project created = projectService.createProject(projectDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id, @RequestBody ProjectDTO projectDTO) {
        return ResponseEntity.ok(projectService.updateProject(id, projectDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
