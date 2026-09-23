# Proyecto Integrador — Projects & Todos API

API REST en Spring Boot según el diseño del pizarrón. Lista para abrir y ejecutar en **IntelliJ IDEA**.

## Requisitos

- JDK 21+
- IntelliJ IDEA (Community o Ultimate)
- Maven (incluido con IntelliJ / wrapper no requerido)

## Cómo abrirlo en IntelliJ IDEA

1. **File → Open** y selecciona la carpeta del proyecto (donde está el `pom.xml`).
2. IntelliJ detectará Maven y descargará las dependencias.
3. Abre `ProjectsApplication.java` → clic derecho → **Run 'ProjectsApplication'**.
4. La API quedará en: `http://localhost:8080/projects`

Consola H2 (opcional): `http://localhost:8080/h2-console`  
- JDBC URL: `jdbc:h2:mem:projectsdb`  
- User: `sa` / Password: (vacío)

## Modelo

| Entidad | Campos |
|---------|--------|
| **Project** | `id`, `name`, `List<Todo> todos` |
| **Todo** | `id`, `title`, `isCompleted` |

| DTO | Campos |
|-----|--------|
| **ProjectDTO** | `name`, `List<TodoDTO> todos` |
| **TodoDTO** | `title`, `isCompleted` |

## Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/projects` | Lista todos los proyectos con sus atributos |
| `GET` | `/projects/{id}` | Obtiene un proyecto por id |
| `POST` | `/projects` | Crea un proyecto (valida nombre vacío y duplicados) |
| `PUT` | `/projects/{id}` | Actualiza un proyecto |
| `DELETE` | `/projects/{id}` | Elimina un proyecto |

## Validaciones (`ProjectService.createProject`)

- **validarNombreVacio** — usa `String.isBlank()`
- **validarDuplicidad** — el nombre del proyecto no se repite

## Datos iniciales (`data.sql`)

Al arrancar se cargan **5 proyectos**, cada uno con **5 todos** (25 en total).

## Ejemplo de creación

```bash
curl -X POST http://localhost:8080/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nuevo Proyecto",
    "todos": [
      { "title": "Tarea 1", "isCompleted": false },
      { "title": "Tarea 2", "isCompleted": true }
    ]
  }'
```

## Estructura del proyecto

```
src/main/java/com/nakano/projects/
├── ProjectsApplication.java
├── controller/ProjectController.java
├── dto/ProjectDTO.java, TodoDTO.java
├── entity/Project.java, Todo.java
├── exception/...
├── mapper/ProjectMapper.java
├── repository/ProjectRepository.java
└── service/ProjectService.java
src/main/resources/
├── application.properties
└── data.sql
```
