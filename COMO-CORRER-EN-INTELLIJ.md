# Si NO te deja correr — léeme

## Error: Port XXXX was already in use

Eso significa que ya hay otra app usando el puerto.

### Solución A (recomendada) — matar el proceso en Windows

1. Cierra todas las pestañas Run de IntelliJ (cuadrado rojo ⏹).
2. Abre **CMD** y ejecuta:

```bat
netstat -ano | findstr :8090
netstat -ano | findstr :8081
netstat -ano | findstr :8080
```

3. Anota el número de la última columna (PID) y ejecuta:

```bat
taskkill /PID AQUI_EL_NUMERO /F
```

4. Vuelve a dar Run en `Application`.

### Solución B — cambiar el puerto

En `src/main/resources/application.properties`:

```properties
server.port=8090
```

Si 8090 también está ocupado, prueba `8091` o `9090`.

---

## Cómo correr paso a paso

1. Descomprime el ZIP en una carpeta nueva (borra la anterior si tenías una vieja).
2. IntelliJ → **File → Open** → selecciona la carpeta donde está `build.gradle.kts`.
3. Cuando diga **Load Gradle Project** / Trust → acepta.
4. Espera a que termine de sincronizar (barra abajo).
5. Abre:
   `src/main/java/dev/antoniochacon/projects/Application.java`
6. Clic en el ▶ verde al lado de `main`.
7. Cuando veas `Started Application`, abre el navegador en:

**http://localhost:8090/projects**

---

## Si no aparece el ▶ Run

- Clic derecho en `build.gradle.kts` → **Reload Gradle Project**
- O panel Gradle (elefante) → Reload
- Verifica JDK: **File → Project Structure → SDK = 21**
