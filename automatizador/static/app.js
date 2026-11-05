const PRESETS = {
  gmail: { host: "smtp.gmail.com", puerto: "587", cifrado: "starttls" },
  outlook: { host: "smtp.office365.com", puerto: "587", cifrado: "starttls" },
  yahoo: { host: "smtp.mail.yahoo.com", puerto: "587", cifrado: "starttls" },
};

const EJEMPLO = [
  "Ana Gómez <ana@ejemplo.com>",
  "Luis Pérez <luis@ejemplo.com>",
  "Marta Ríos <marta@ejemplo.com>",
].join("\n");

const BORRADOR = "borrador-correos";
const CAMPOS = ["host", "puerto", "cifrado", "usuario", "remitente", "nombre-remitente", "asunto", "texto", "html", "lista", "pausa"];

const $ = (id) => document.getElementById(id);

let revision = {
  total: 0,
  identico: true,
  invalidos: [],
  invalidos_total: 0,
  duplicados_total: 0,
  muestra: [],
  cierre: [],
  muestras_mensaje: [],
};
let claveDisponible = false;
let reloj = 0;
let vigilante = 0;
let enviando = false;

document.addEventListener("DOMContentLoaded", iniciar);

async function iniciar() {
  document.querySelectorAll("[data-preset]").forEach((boton) => {
    boton.addEventListener("click", () => aplicarPreset(boton.dataset.preset));
  });
  for (const id of ["host", "puerto", "cifrado", "usuario", "remitente", "nombre-remitente", "asunto", "texto", "html", "lista", "pausa"]) {
    $(id).addEventListener("input", programarRevision);
    $(id).addEventListener("change", programarRevision);
  }
  $("archivo").addEventListener("change", leerArchivo);
  $("boton-ejemplo").addEventListener("click", () => {
    $("lista").value = EJEMPLO;
    programarRevision();
  });
  $("boton-simular").addEventListener("click", () => lanzar(true));
  $("boton-enviar").addEventListener("click", abrirConfirmacion);
  $("boton-confirmar").addEventListener("click", confirmarEnvio);
  $("boton-cancelar").addEventListener("click", () => $("confirma").close());
  $("boton-prueba").addEventListener("click", enviarPrueba);
  $("boton-detener").addEventListener("click", detener);
  $("numero-confirma").addEventListener("input", actualizarConfirmacion);
  $("acepto-envio").addEventListener("change", actualizarConfirmacion);
  $("usar-clave-entorno").addEventListener("change", () => {
    $("clave").disabled = $("usar-clave-entorno").checked;
  });
  $("numero-confirma").addEventListener("keydown", (evento) => {
    if (evento.key === "Enter" && !$("boton-confirmar").disabled) {
      evento.preventDefault();
      confirmarEnvio();
    }
  });
  $("confirma").querySelector("form").addEventListener("submit", (evento) => {
    evento.preventDefault();
  });
  restaurarBorrador();
  try {
    const config = await (await fetch("/api/configuracion")).json();
    aplicarConfiguracion(config);
  } catch (_error) {
    mostrarAviso("No se pudo leer la configuración local.");
  }
  if (!$("host").value) aplicarPreset("gmail");
  await revisar();
  await actualizarEstado();
}

function aplicarPreset(nombre) {
  const preset = PRESETS[nombre];
  if (!preset) return;
  $("host").value = preset.host;
  $("puerto").value = preset.puerto;
  $("cifrado").value = preset.cifrado;
  document.querySelectorAll("[data-preset]").forEach((boton) => {
    boton.setAttribute("aria-pressed", boton.dataset.preset === nombre ? "true" : "false");
  });
  programarRevision();
}

function aplicarConfiguracion(config) {
  if (!$("usuario").value && config.usuario) $("usuario").value = config.usuario;
  if (!$("host").value && config.servidor) $("host").value = config.servidor;
  if (config.puerto && $("puerto").value === "587" && config.puerto !== "587") $("puerto").value = config.puerto;
  if (!$("remitente").value && config.remitente) $("remitente").value = config.remitente;
  if (!$("nombre-remitente").value && config.nombre) $("nombre-remitente").value = config.nombre;
  if (config.cifrado) $("cifrado").value = config.cifrado;
  claveDisponible = Boolean(config.clave_disponible);
  $("bloque-clave-entorno").hidden = !claveDisponible;
}

function programarRevision() {
  guardarBorrador();
  clearTimeout(reloj);
  reloj = setTimeout(revisar, 250);
}

async function revisar() {
  guardarBorrador();
  try {
    revision = await postJson("/api/revisar", cuerpoMensaje());
  } catch (error) {
    mostrarAviso(error.message);
    return;
  }
  ocultarAviso();
  pintarLista();
  pintarVista();
  pintarEstimacion();
}

function cuerpoMensaje() {
  return {
    destinatarios: $("lista").value,
    asunto: $("asunto").value,
    texto: $("texto").value,
    html: $("html").value,
  };
}

function cuerpoEnvio() {
  return {
    ...cuerpoMensaje(),
    host: $("host").value,
    puerto: $("puerto").value,
    cifrado: $("cifrado").value,
    usuario: $("usuario").value,
    clave: $("usar-clave-entorno").checked ? "" : $("clave").value,
    usar_clave_entorno: Boolean($("usar-clave-entorno").checked),
    remitente: $("remitente").value,
    nombre_remitente: $("nombre-remitente").value,
    pausa: Number($("pausa").value),
    reanudar: $("reanudar").checked,
  };
}

function pintarLista() {
  const total = revision.total || 0;
  $("cuenta-numero").textContent = String(total);
  const boton = $("boton-enviar");
  boton.textContent = total ? `Enviar ${total} correos, uno por uno` : "Enviar correos, uno por uno";
  $("boton-simular").textContent = total ? `Simular los ${total}` : "Simular";
  const partes = [];
  if (total) {
    const nombres = revision.muestra.map(formatoPersona).join(", ");
    partes.push(`${total} ${total === 1 ? "correo válido" : "correos válidos"}.`);
    if (nombres) partes.push(`Primeros: ${nombres}.`);
    if (revision.cierre && revision.cierre.length) {
      partes.push(`Últimos: ${revision.cierre.map(formatoPersona).join(", ")}.`);
    }
  } else {
    partes.push("Todavía no hay correos válidos.");
  }
  if (revision.duplicados_total === 1) {
    partes.push("1 repetido se enviará una sola vez.");
  } else if (revision.duplicados_total) {
    partes.push(`${revision.duplicados_total} repetidos se enviarán una sola vez.`);
  }
  $("resumen-lista").textContent = partes.join(" ");
  const listaInvalidos = $("invalidos");
  listaInvalidos.replaceChildren();
  if (revision.invalidos_total) {
    listaInvalidos.hidden = false;
    const titulo = document.createElement("li");
    titulo.textContent = revision.invalidos_total === 1
      ? "1 línea no se usará."
      : `${revision.invalidos_total} líneas no se usarán.`;
    listaInvalidos.append(titulo);
    for (const linea of revision.invalidos) {
      const item = document.createElement("li");
      item.textContent = linea;
      listaInvalidos.append(item);
    }
  } else {
    listaInvalidos.hidden = true;
  }
}

function formatoPersona(persona) {
  return persona.nombre ? `${persona.nombre} <${persona.email}>` : persona.email;
}

function pintarVista() {
  const vista = $("vista");
  const muestras = revision.muestras_mensaje || [];
  if (!revision.total || !muestras.length) {
    vista.hidden = true;
    return;
  }
  vista.hidden = false;
  const primera = muestras[0];
  if (revision.identico) {
    $("vista-titulo").textContent = `Este es el contenido exacto que recibirán las ${revision.total} personas.`;
  } else {
    $("vista-titulo").textContent = "El texto base es el mismo. Solo cambian {{nombre}}, {{email}}, {{n}} o {{total}}.";
  }
  $("vista-asunto").textContent = `Para ${primera.email}: ${primera.asunto}`;
  $("vista-texto").textContent = primera.texto;
  const segunda = $("vista-segunda");
  if (muestras[1]) {
    segunda.hidden = false;
    $("vista-asunto-2").textContent = `Para ${muestras[1].email}: ${muestras[1].asunto}`;
    $("vista-texto-2").textContent = muestras[1].texto;
  } else {
    segunda.hidden = true;
  }
}

function pintarEstimacion() {
  const total = revision.total || 0;
  const pausa = Number($("pausa").value);
  const nota = $("estimacion");
  const limite = $("aviso-limite");
  if (!total) {
    nota.textContent = "";
    limite.hidden = true;
    return;
  }
  const segundos = Math.round(total * ((Number.isFinite(pausa) ? pausa : 0) + 0.5));
  const duracion = segundos < 90
    ? `cerca de ${Math.max(segundos, 1)} s`
    : `cerca de ${Math.round(segundos / 60)} min`;
  nota.textContent = `${total} envíos individuales. Con esta pausa, el lote toma ${duracion}.`;
  const host = $("host").value.toLowerCase();
  if (total >= 400 && host.includes("gmail")) {
    limite.hidden = false;
    limite.textContent = `${total} cabe en el límite diario habitual de Gmail (cerca de 500), si la cuenta no envió otros correos hoy.`;
  } else if (total > 50 && Number.isFinite(pausa) && pausa < 1) {
    limite.hidden = false;
    limite.textContent = "Una pausa menor a 1 segundo puede hacer que el proveedor bloquee la cuenta.";
  } else {
    limite.hidden = true;
  }
}

function abrirConfirmacion() {
  if (!revision.total) {
    mostrarAviso("No hay correos válidos en la lista.");
    return;
  }
  if (!$("asunto").value.trim() || !$("texto").value.trim()) {
    mostrarAviso("Escribe el asunto y el cuerpo.");
    return;
  }
  $("confirma-cantidad").textContent = String(revision.total);
  $("confirma-pausa").textContent = $("pausa").value || "0";
  $("confirma-identico").textContent = revision.identico
    ? "Todas las personas reciben el mismo asunto y el mismo cuerpo."
    : "El cuerpo es el mismo, con el nombre o el correo de cada persona donde dejaste un marcador.";
  $("numero-confirma").value = "";
  $("acepto-envio").checked = false;
  $("error-confirma").textContent = "";
  $("boton-confirmar").disabled = true;
  $("confirma").showModal();
  $("numero-confirma").focus();
}

function actualizarConfirmacion() {
  const escrito = $("numero-confirma").value.trim();
  const coincide = escrito === String(revision.total);
  const acepto = $("acepto-envio").checked;
  $("boton-confirmar").disabled = !(coincide && acepto);
  if (escrito && !coincide) {
    $("error-confirma").textContent = `El número tiene que ser ${revision.total}.`;
  } else {
    $("error-confirma").textContent = "";
  }
}

async function confirmarEnvio() {
  const pedido = cuerpoEnvio();
  pedido.confirmar = Number($("numero-confirma").value);
  $("confirma").close();
  await lanzar(false, pedido);
}

async function lanzar(simular, pedido) {
  if (enviando) return;
  const cuerpo = pedido || cuerpoEnvio();
  enviando = true;
  bloquear(true);
  ocultarAviso();
  try {
    const respuesta = await postJson(simular ? "/api/simular" : "/api/enviar", cuerpo);
    mostrarProgreso();
    $("estado-texto").textContent = respuesta.mensaje;
    vigilar();
  } catch (error) {
    mostrarAviso(error.message);
    enviando = false;
    bloquear(false);
  }
}

async function enviarPrueba() {
  const destino = $("prueba-destino").value.trim();
  if (!destino) {
    mostrarAviso("Escribe el correo que recibirá la prueba.");
    return;
  }
  bloquear(true);
  ocultarAviso();
  try {
    const cuerpo = cuerpoEnvio();
    cuerpo.destino_prueba = destino;
    const respuesta = await postJson("/api/prueba", cuerpo);
    mostrarAviso("");
    $("aviso").hidden = true;
    $("estado-texto").textContent = respuesta.mensaje;
    $("progreso").hidden = false;
  } catch (error) {
    mostrarAviso(error.message);
  } finally {
    if (!vigilante) bloquear(false);
  }
}

async function detener() {
  try {
    const respuesta = await postJson("/api/detener", {});
    $("estado-texto").textContent = respuesta.mensaje;
  } catch (error) {
    mostrarAviso(error.message);
  }
}

function vigilar() {
  clearInterval(vigilante);
  vigilante = setInterval(actualizarEstado, 500);
  actualizarEstado();
}

async function actualizarEstado() {
  let datos;
  try {
    datos = await (await fetch("/api/estado")).json();
  } catch (_error) {
    return;
  }
  if (datos.fase === "idle" && !datos.procesados) {
    if (!enviando) $("progreso").hidden = true;
    return;
  }
  mostrarProgreso();
  const total = datos.total || 0;
  const hechos = datos.procesados || 0;
  const porcentaje = total ? Math.round((hechos / total) * 100) : 0;
  $("barra-avance").style.setProperty("--avance", `${porcentaje}%`);
  const actual = datos.actual ? ` Ahora: ${datos.actual}.` : "";
  $("estado-texto").textContent = `${datos.mensaje || "Enviando."}${actual}`;
  $("progreso-numeros").textContent =
    `${hechos} de ${total} · ${datos.enviados || 0} enviados · ${datos.simulados || 0} simulados · ${datos.fallidos || 0} fallidos · ${datos.omitidos || 0} omitidos`;
  const corriendo = datos.fase === "running";
  $("boton-detener").hidden = !corriendo;
  pintarTabla(datos.resultados || []);
  if (!corriendo) {
    clearInterval(vigilante);
    vigilante = 0;
    enviando = false;
    bloquear(false);
  }
}

function pintarTabla(filas) {
  const cuerpo = $("tabla-resultados");
  const fragmento = document.createDocumentFragment();
  for (const fila of filas) {
    const tr = document.createElement("tr");
    tr.append(
      celda(fila.hora || ""),
      celda(fila.email || ""),
      celdaEstado(fila.estado || ""),
      celda(fila.detalle || ""),
    );
    fragmento.append(tr);
  }
  cuerpo.replaceChildren(fragmento);
}

function celda(texto) {
  const td = document.createElement("td");
  td.textContent = texto;
  return td;
}

function celdaEstado(estado) {
  const td = document.createElement("td");
  td.textContent = estado;
  td.className = `estado-${estado}`;
  return td;
}

function mostrarProgreso() {
  $("progreso").hidden = false;
}

function bloquear(activo) {
  for (const id of ["boton-enviar", "boton-simular", "boton-prueba", "boton-ejemplo"]) {
    $(id).disabled = activo;
  }
}

function mostrarAviso(texto) {
  if (!texto) {
    ocultarAviso();
    return;
  }
  $("aviso").hidden = false;
  $("aviso").textContent = texto;
}

function ocultarAviso() {
  $("aviso").hidden = true;
  $("aviso").textContent = "";
}

function guardarBorrador() {
  const datos = {};
  for (const id of CAMPOS) datos[id] = $(id).value;
  datos.reanudar = $("reanudar").checked;
  sessionStorage.setItem(BORRADOR, JSON.stringify(datos));
}

function restaurarBorrador() {
  const crudo = sessionStorage.getItem(BORRADOR);
  if (!crudo) return;
  try {
    const datos = JSON.parse(crudo);
    for (const id of CAMPOS) {
      if (typeof datos[id] === "string") $(id).value = datos[id];
    }
    $("reanudar").checked = Boolean(datos.reanudar);
  } catch (_error) {
    sessionStorage.removeItem(BORRADOR);
  }
}

function leerArchivo(evento) {
  const archivo = evento.target.files && evento.target.files[0];
  if (!archivo) return;
  const lector = new FileReader();
  lector.onload = () => {
    $("lista").value = String(lector.result || "");
    programarRevision();
  };
  lector.readAsText(archivo);
}

async function postJson(url, cuerpo) {
  const respuesta = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(cuerpo),
  });
  const datos = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) {
    throw new Error(datos.mensaje || "No se pudo completar la acción.");
  }
  return datos;
}
