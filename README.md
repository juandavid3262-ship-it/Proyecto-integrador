# Trabajo Arquitectura — Projects & Todos API

Proyecto Spring Boot con **Gradle** (`build.gradle.kts`), según la estructura indicada.

## Abrir en IntelliJ

1. Descomprime `trabajo-arquitectura.zip`
2. **File → Open** → carpeta con `build.gradle.kts`
3. Trust Project / Load Gradle
4. Run en `dev.nakano.projects.ProjectsApplication`
5. Abre: http://localhost:8081/projects

## Estructura Gradle

```
build.gradle.kts
settings.gradle.kts
src/main/java/dev/nakano/projects/
  ProjectsApplication.java
  controller/
  dto/
  entity/
  exception/
  mapper/
  repository/
  service/
src/main/resources/
  application.properties
  data.sql
```

## Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/projects` | Lista proyectos con todos |
| POST | `/projects` | Crear (valida vacío y duplicados) |
| PUT | `/projects/{id}` | Actualizar |
| DELETE | `/projects/{id}` | Eliminar |

Guía: [COMO-CORRER-EN-INTELLIJ.md](COMO-CORRER-EN-INTELLIJ.md)
