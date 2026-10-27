const token = document.querySelector('meta[name="_csrf"]')?.content;
const headerName = document.querySelector('meta[name="_csrf_header"]')?.content;

async function patch(url, body) {
  const headers = { "Content-Type": "application/json" };
  if (token && headerName) {
    headers[headerName] = token;
  }
  return fetch(url, { method: "PATCH", headers, body: JSON.stringify(body) });
}

document.getElementById("btn-sidebar")?.addEventListener("click", () => {
  document.body.classList.toggle("is-collapsed");
  const plegada = document.body.classList.contains("is-collapsed");
  patch("/api/preferencias/sidebar", { plegada });
});

document.getElementById("btn-tema")?.addEventListener("click", async () => {
  const actual = document.documentElement.getAttribute("data-theme") === "oscuro" ? "OSCURO" : "CLARO";
  const siguiente = actual === "OSCURO" ? "CLARO" : "OSCURO";
  const respuesta = await patch("/api/preferencias/tema", { tema: siguiente });
  if (respuesta.ok) {
    document.documentElement.setAttribute("data-theme", siguiente.toLowerCase());
    const boton = document.getElementById("btn-tema");
    if (boton) {
      boton.textContent = siguiente === "OSCURO" ? "Claro" : "Oscuro";
    }
  }
});
