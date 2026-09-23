# nakano

Proyecto Gradle Spring Boot (mismo patrón del zip de referencia).

## Abrir en IntelliJ

1. Descomprime el ZIP
2. **File → Open** → carpeta con `build.gradle.kts`
3. Run ▶ en `Application`
4. Abre: http://localhost:8080/api/projects

## Estructura

```
dev.nakano
├── Application
├── controller/ProjectController
├── model/dto/ProjectDTO, TodoDTO
├── model/entity/Project, Todo
├── repository/ProjectRepository
└── service/ProjectService
```
