package dev.nakano.projects.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "mensaje", "API Projects lista",
                "proyectos", "http://localhost:8081/projects",
                "h2Console", "http://localhost:8081/h2-console"
        );
    }
}
