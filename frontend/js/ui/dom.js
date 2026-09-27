import { authService } from "../services/auth-service.js";
import { storeService } from "../services/store-service.js";

export function createElement(
  tag,
  { className = "", text = null, attrs = {}, children = [] } = {},
) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== null) node.textContent = String(text);
  Object.entries(attrs).forEach(([key, value]) => {
    if (value === true) node.setAttribute(key, "");
    else if (value !== false && value !== null && value !== undefined)
      node.setAttribute(key, String(value));
  });
  children.filter(Boolean).forEach((child) => node.append(child));
  return node;
}

export function setMessage(node, message, type = "success") {
  if (!node) return;
  node.textContent = message;
  node.classList.remove(
    "alert-success",
    "alert-danger",
    "alert-warning",
    "d-none",
  );
  node.classList.add(`alert-${type}`);
  node.hidden = false;
}

export function clear(node) {
  node?.replaceChildren();
}

export function formatMoney(value) {
  return `S/ ${Number(value || 0).toLocaleString("es-PE", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

export function formatDate(value, options = { dateStyle: "long" }) {
  if (!value) return "—";
  const date =
    typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value)
      ? new Date(`${value}T12:00:00`)
      : new Date(value);
  return Number.isNaN(date.valueOf())
    ? "—"
    : new Intl.DateTimeFormat("es-PE", options).format(date);
}

export function pagePath(file) {
  return new URL(`../../pages/${file}`, import.meta.url).href;
}

export function protectPage(role, redirect = "login.html") {
  const session = authService.current();
  if (session?.role === role) return true;
  const returnTo = `${location.pathname.split("/").pop()}${location.search}`;
  location.replace(
    `${pagePath(redirect)}?returnTo=${encodeURIComponent(returnTo)}`,
  );
  return false;
}

export function updateNavigation(session) {
  document.querySelectorAll("#contador-carrito").forEach((counter) => {
    const count = storeService.cartSummary().count;
    if (counter.firstChild) counter.firstChild.nodeValue = String(count);
    counter.setAttribute(
      "aria-label",
      `${count} artículo${count === 1 ? "" : "s"} en el carrito`,
    );
    counter.hidden = count === 0;
  });

  document.querySelectorAll("[data-auth-link]").forEach((link) => {
    if (!session) {
      link.textContent = "Iniciar sesión";
      link.setAttribute("href", pagePath("login.html"));
      link.removeAttribute("data-logout");
      return;
    }
    link.textContent = session.role === "admin" ? "Panel Admin" : "Mi cuenta";
    link.setAttribute(
      "href",
      pagePath(session.role === "admin" ? "admin.html" : "perfil.html"),
    );
    link.removeAttribute("data-logout");
  });

  document.querySelectorAll("[data-auth-name]").forEach((node) => {
    node.textContent = session?.name ?? "Invitado";
  });
}
