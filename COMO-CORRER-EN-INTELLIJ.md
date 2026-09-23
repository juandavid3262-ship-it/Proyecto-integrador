# Cómo abrir y correr en IntelliJ IDEA

## Requisitos
- **JDK 17** (o superior). En IntelliJ: `File → Project Structure → Project → SDK`
- IntelliJ IDEA Community o Ultimate

## Pasos (importante)

1. **Descomprime** el ZIP (no abras el `.zip` directamente).
2. En IntelliJ: **File → Open**
3. Selecciona la carpeta donde está el archivo **`pom.xml`** (no la carpeta `src`).
4. Cuando pregunte, elige **Trust Project** / **Open as Project**.
5. Espera a que Maven descargue dependencias (barra abajo a la derecha).
6. Si aparece un banner **Maven** o **Load Maven Project**, haz clic en **Load** / **Reload**.
7. Abre:
   `src/main/java/com/nakano/projects/ProjectsApplication.java`
8. Clic en el ícono verde ▶ junto a `main` → **Run 'ProjectsApplication'**

La API queda en: http://localhost:8081/projects

## Si no te deja correr

### 1) No aparece el botón Run / "Nothing here"
- Confirma que abriste la carpeta con el `pom.xml`.
- Clic derecho en `pom.xml` → **Add as Maven Project**.
- `File → Invalidate Caches → Invalidate and Restart`.

### 2) Error de Java / "invalid target release: 21" / "release version 21 not supported"
El proyecto usa **Java 17**.
- `File → Project Structure → Project`
  - **SDK**: Java 17
  - **Language level**: 17
- `Settings → Build → Build Tools → Maven → Runner → JRE`: Java 17
- `Settings → Build → Compiler → Java Compiler → Project bytecode version`: 17

Si no tienes JDK 17:
- `File → Project Structure → SDKs → + → Download JDK…` → versión **17**

### 3) Puerto 8080 ocupado
Cambia en `src/main/resources/application.properties`:
```properties
server.port=8081
```

### 4) Dependencias rojas / clases de Spring no se resuelven
- Abre el panel **Maven** (lado derecho) → ícono **Reload All Maven Projects**
- O en terminal dentro de la carpeta del proyecto:
```bash
./mvnw clean install
```
(En Windows: `mvnw.cmd clean install`)

## Alternativa sin IntelliJ (terminal)

```bash
./mvnw spring-boot:run
```

Luego abre: http://localhost:8081/projects
