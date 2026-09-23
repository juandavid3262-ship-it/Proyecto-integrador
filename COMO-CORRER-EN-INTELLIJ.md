# Cómo correr en IntelliJ (Gradle)

## Requisitos
- JDK 21
- IntelliJ IDEA

## Pasos
1. Descomprime el ZIP
2. **File → Open** → carpeta con **`build.gradle.kts`**
3. Espera a que Gradle descargue dependencias
4. Abre `src/main/java/dev/nakano/projects/ProjectsApplication.java`
5. Run ▶
6. En el navegador: http://localhost:8081/projects

## Si el puerto está ocupado
En `application.properties`:
```properties
server.port=8081
```

O libera el 8080 en Windows:
```bat
netstat -ano | findstr :8080
taskkill /PID NUMERO /F
```

## Alternativa por terminal
```bash
./gradlew bootRun
```
