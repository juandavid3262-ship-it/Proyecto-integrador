# Proyecto Integrador — Projects & Todos API

API REST en Spring Boot. Lista para abrir y ejecutar en **IntelliJ IDEA**.

> Guía detallada: [COMO-CORRER-EN-INTELLIJ.md](COMO-CORRER-EN-INTELLIJ.md)

## Requisitos

- **JDK 17+** (recomendado 17)
- IntelliJ IDEA (Community o Ultimate)

## Cómo abrirlo en IntelliJ IDEA (rápido)

1. Descomprime el ZIP.
2. **File → Open** → selecciona la carpeta donde está el **`pom.xml`**.
3. Espera a que Maven cargue dependencias.
4. Abre `ProjectsApplication.java` → clic en ▶ **Run**.
5. Abre en el navegador: http://localhost:8080/projects

Si falla, lee [COMO-CORRER-EN-INTELLIJ.md](COMO-CORRER-EN-INTELLIJ.md) (JDK, Maven, puerto 8080).

## Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/projects` | Lista todos los proyectos con sus atributos |
| `GET` | `/projects/{id}` | Obtiene un proyecto por id |
| `POST` | `/projects` | Crea un proyecto |
| `PUT` | `/projects/{id}` | Actualiza un proyecto |
| `DELETE` | `/projects/{id}` | Elimina un proyecto |

## Validaciones

- Nombre vacío → usa `String.isBlank()`
- Nombre duplicado → no se permite

## Datos iniciales

`data.sql` carga **5 proyectos**, cada uno con **5 todos**.
