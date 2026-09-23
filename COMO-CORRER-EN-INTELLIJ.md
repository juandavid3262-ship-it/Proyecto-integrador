# Cómo correr en IntelliJ (Gradle)

1. Descomprime el ZIP
2. **File → Open** → carpeta con **`build.gradle.kts`**
3. Espera a que Gradle cargue
4. Abre `src/main/java/dev/antoniochacon/projects/Application.java`
5. Run ▶
6. Navegador: http://localhost:8081/projects

Si el puerto está ocupado, en `application.properties`:
```properties
server.port=8081
```
