# Trabajo Arquitectura — Projects API

Gradle + Spring Boot 4.1, con la misma estructura de paquetes que bibliotech.

## Estructura

```
dev.antoniochacon.projects
├── Application
├── model
│   ├── dto        → ProjectDTO, TodoDTO (records)
│   └── entity     → Project, Todo
├── repository     → ProjectRepository
├── service        → ProjectService
└── controller     → ProjectController
```

## Abrir en IntelliJ

1. Descomprime `trabajo-arquitectura.zip`
2. **File → Open** → carpeta con `build.gradle.kts`
3. Run en `Application`
4. http://localhost:8081/projects
