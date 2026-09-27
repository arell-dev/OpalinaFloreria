export function initializePasswordVisibility(root = document) {
  root.addEventListener("click", (event) => {
    const button = event.target.closest("[data-toggle-password]");
    if (!button || !root.contains(button)) return;

    const input = root.getElementById(button.dataset.togglePassword);
    if (!input) return;

    const isVisible = input.type === "password";
    input.type = isVisible ? "text" : "password";
    button.setAttribute("aria-pressed", String(isVisible));
    button.setAttribute(
      "aria-label",
      isVisible ? "Ocultar contraseña" : "Mostrar contraseña",
    );
  });
}
