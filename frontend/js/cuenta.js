/* Controles accesibles de las páginas de acceso. */
(() => {
  "use strict";

  const PASSWORD_TOGGLE_SELECTOR = "[data-toggle-password]";

  document.addEventListener("click", (event) => {
    if (!(event.target instanceof Element)) return;

    const button = event.target.closest(PASSWORD_TOGGLE_SELECTOR);
    if (!button) return;

    const input = document.getElementById(button.dataset.togglePassword);
    if (!(input instanceof HTMLInputElement)) return;

    const isVisible = input.type === "password";
    input.type = isVisible ? "text" : "password";
    button.setAttribute("aria-pressed", String(isVisible));
    button.setAttribute(
      "aria-label",
      isVisible ? "Ocultar contraseña" : "Mostrar contraseña",
    );
  });
})();
