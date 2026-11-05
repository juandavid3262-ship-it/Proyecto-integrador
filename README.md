# Proyecto integrador

Automatizador para enviar **el mismo correo a cada persona por separado**. El destinatario va solo en el campo Para: no hay copias ocultas ni un solo mensaje con toda la lista.

Está pensado para un lote como **408 correos**. Con la pausa de 2 segundos, el envío tarda cerca de 15 a 25 minutos, según lo que tarde el servidor.

## Abrir la pantalla

```bash
python3 iniciar.py
```

Entra a [http://127.0.0.1:8765](http://127.0.0.1:8765). Solo responde en este computador.

1. Elige el proveedor (Gmail, Outlook o Yahoo) y escribe la cuenta que envía.
2. Escribe el asunto y el cuerpo **una vez**.
3. Pega la lista o sube un CSV.
4. Simula el lote. Si el número y el texto se ven bien, confirma el envío escribiendo esa cantidad.

La clave no se guarda en el navegador. Si prefieres no escribirla en la pantalla, ponla en un archivo `.env` (hay un modelo en `.env.example`) y marca la casilla para usarla desde ahí.

## Lista de destinatarios

Sirve cualquiera de estos formatos:

```text
ana@correo.com
Luis Pérez <luis@correo.com>
marta@correo.com, Marta Ríos
```

O un CSV. Excel en español suele separarlo con punto y coma:

```text
nombre;correo
Ana Gómez;ana@correo.com
```

Los correos repetidos se envían una sola vez. Las líneas que no son un correo se muestran y se omiten.

Si el texto no incluye `{{nombre}}`, `{{email}}`, `{{n}}` ni `{{total}}`, las 408 personas reciben el asunto y el cuerpo exactos. Esos marcadores son lo único que cambia de un envío a otro.

## Gmail

Gmail no acepta la contraseña normal por SMTP si tienes verificación en dos pasos.

1. Activa la verificación en dos pasos de la cuenta.
2. Crea una contraseña de aplicación en [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).
3. Úsala en el campo clave. El usuario es el correo completo.
4. Servidor `smtp.gmail.com`, puerto `587`, cifrado STARTTLS.

Una cuenta personal de Gmail suele permitir cerca de 500 correos al día. 408 entran en ese margen si ese día no salieron otros envíos desde la misma cuenta. Si el proveedor rechaza la sesión por cupo, el automatizador se detiene y puedes reanudar más tarde con la casilla de no repetir los que ya salieron.

Outlook usa `smtp.office365.com` en el puerto 587. Algunas cuentas de empresa o de universidad bloquean SMTP: este programa no salta ese bloqueo.

## Terminal

Revisar sin enviar:

```bash
python3 -m automatizador.cli \
  --destinatarios ejemplo/destinatarios.csv \
  --mensaje ejemplo/mensaje.txt \
  --asunto "Aviso" \
  --simular
```

Enviar de verdad, con la clave fuera del comando:

```bash
export SMTP_CLAVE="la contraseña de aplicación"
python3 -m automatizador.cli \
  --destinatarios destinatarios.csv \
  --mensaje mensaje.txt \
  --asunto "Aviso" \
  --usuario "tu_correo@gmail.com" \
  --enviar
```

Te pide escribir el número de correos antes de conectar. En un proceso sin teclado, define `CONFIRMAR_CANTIDAD` con ese mismo número.

La bitácora queda en `salidas/ultimo.jsonl`. Si el proceso se corta, vuelve a lanzarlo con `--reanudar` o marca la casilla en la pantalla: no repite los que ya figuran como enviados.

## Pruebas

```bash
python3 -m unittest discover -s tests -v
```
