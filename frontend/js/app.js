import { authService } from "./services/auth-service.js";
import { initAdmin } from "./pages/admin.js";
import { initAuthGuards, initCart, initCatalog, initCheckout, initConfirmation, initHome, initLogin, initOrderDetail, initOrderHistory, initProductDetail, initProfile, initRegistration } from "./pages/storefront.js";
import { updateNavigation } from "./ui/dom.js";

const page = location.pathname.split("/").pop() || "index.html";
const session = authService.current();

updateNavigation(session);
if (initAuthGuards()) {
  const pageInitializers = {
    "index.html": initHome,
    "catalogo.html": initCatalog,
    "producto.html": initProductDetail,
    "carrito.html": initCart,
    "pedido.html": initCheckout,
    "confirmacion.html": initConfirmation,
    "login.html": initLogin,
    "registro.html": initRegistration,
    "perfil.html": initProfile,
    "historial-pedidos.html": initOrderHistory,
    "detalle-pedido.html": initOrderDetail,
  };
  pageInitializers[page]?.();
  if (page.startsWith("admin")) initAdmin();
}

window.addEventListener("storage", (event) => {
  if (event.key === null || event.key?.startsWith("opalina-"))
    updateNavigation(authService.current());
});

document.addEventListener("click", (event) => {
  const logout = event.target.closest?.("[data-logout]");
  if (logout) {
    event.preventDefault();
    authService.logout();
    location.href = new URL("../pages/login.html", import.meta.url).href;
    return;
  }
  const toggle = event.target.closest?.("[data-toggle-password]");
  if (!toggle) return;
  const input = document.getElementById(toggle.dataset.togglePassword);
  if (!input) return;
  const visible = input.type === "password";
  input.type = visible ? "text" : "password";
  toggle.setAttribute("aria-pressed", String(visible));
  toggle.setAttribute(
    "aria-label",
    visible ? "Ocultar contraseña" : "Mostrar contraseña",
  );
});
