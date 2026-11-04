# bibliotech

Proyecto Gradle Spring Boot, listo para abrir en IntelliJ IDEA.

Grupo: `dev.juanmateus`

## Abrir en IntelliJ

1. **File → Open** y selecciona la carpeta que contiene `build.gradle.kts`.
2. IntelliJ importa Gradle y descarga las dependencias. Usa JDK 21.
3. Abre `src/main/java/dev/juanmateus/bibliotech/Application.java`.
4. Pulsa **Run** en `Application`.
5. Abre http://localhost:8080/genres

La consola H2 queda en http://localhost:8080/h2

- JDBC URL: `jdbc:h2:mem:bibliotechdb`
- Usuario: `juanmateus`
- Contraseña: `1234`
