# bibliotech

Proyecto Gradle Spring Boot, listo para abrir en IntelliJ IDEA.

Grupo: `dev.juanmateus`

## Abrir en IntelliJ

1. Descomprime el ZIP y entra a la carpeta `bibliotech`.
2. **File → Open** y selecciona esa carpeta (la que contiene `build.gradle.kts`), no la carpeta padre.
3. Espera a que IntelliJ importe Gradle. Usa JDK 21 o superior.
4. Arriba a la derecha elige **Spring Boot Run** y pulsa el botón verde.
5. Abre http://localhost:8080/genres

La consola H2 queda en http://localhost:8080/h2

- JDBC URL: `jdbc:h2:mem:bibliotechdb`
- Usuario: `juanmateus`
- Contraseña: `1234`
