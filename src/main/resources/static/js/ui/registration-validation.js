export function initializeRegistrationValidation(root = document) {
  const form = root.querySelector("#formulario-registro");
  if (!form) return;

  const password = form.querySelector("#clave-registro");
  const confirmation = form.querySelector("#confirmar-clave");
  if (!password || !confirmation) return;

  function validatePasswordConfirmation() {
    const matches = password.value === confirmation.value;
    confirmation.setCustomValidity(
      matches ? "" : "Las contraseñas no coinciden.",
    );
  }

  password.addEventListener("input", validatePasswordConfirmation);
  confirmation.addEventListener("input", validatePasswordConfirmation);
  form.addEventListener("submit", (event) => {
    validatePasswordConfirmation();
    if (!form.reportValidity()) event.preventDefault();
  });
}
