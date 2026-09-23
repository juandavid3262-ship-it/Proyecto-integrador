package dev.antoniochacon.projects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class Application {

    private final Environment environment;

    public Application(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String port = environment.getProperty("local.server.port", "8081");
        System.out.println();
        System.out.println("====================================================");
        System.out.println("  Aplicacion lista. Abre en tu navegador:");
        System.out.println("  http://localhost:" + port + "/projects");
        System.out.println("====================================================");
        System.out.println();
    }
}
