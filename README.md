# Taller automotriz

Sistema web para un taller de reparación automotriz. Cumple el avance del proyecto integrador: modelos, vehículos con placa colombiana, repuestos, historial de reparaciones, roles, dashboard, PDF, CSV, barra lateral y tema claro/oscuro.

Se abre en IntelliJ IDEA y corre en **http://localhost:8080**.

## Abrir en IntelliJ

1. Descarga el repositorio, o descomprime `TallerAutomotriz-IntelliJ.zip`.
2. En IntelliJ: **File → Open** y elige la carpeta que contiene `build.gradle.kts`.
3. Confía en el proyecto Gradle y espera a que importe las dependencias. Hace falta **JDK 21**.
4. Abre `src/main/java/dev/antoniochacon/Application.java` y pulsa el botón **Run** junto a `main`.
5. En el navegador abre http://localhost:8080

La consola de la base H2 queda en http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:taller` · usuario `sa` · contraseña vacía.

## Cuentas

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | admin@taller.com | Admin123* |
| Recepcionista | recepcion@taller.com | Recepcion123* |
| Mecánico | mecanico@taller.com | Mecanico123* |
| Mecánico | mecanico2@taller.com | Mecanico456* |

El administrador ve todo el taller, el dashboard y los reportes. La recepción registra vehículos y busca por placa. El mecánico consulta la placa, los repuestos compatibles y solo modifica sus reparaciones.

## Qué incluye

- Inicio de sesión con correo y contraseña (BCrypt) y autorización por `ROLE_ADMINISTRADOR`, `ROLE_RECEPCIONISTA` y `ROLE_MECANICO`.
- CRUD de modelos, vehículos, repuestos y reparaciones. Placa `ABC-123`, colores amarillo, blanco, rojo, azul y verde, EAN de 13 dígitos.
- Búsqueda de modelo por placa y de repuestos compatibles.
- Dashboard con Chart.js, filtro por fechas y repuestos con stock menor a 5.
- Descarga del historial en PDF y CSV.
- Barra lateral plegable y tema claro/oscuro, guardados en el perfil.
