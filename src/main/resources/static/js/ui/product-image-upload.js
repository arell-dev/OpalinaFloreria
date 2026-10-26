const FORMATOS_PERMITIDOS = new Set(["jpg", "jpeg", "png", "webp"]);
const TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024;

export function initializeProductImageUploads(root = document) {
  root.querySelectorAll("[data-product-image-input]").forEach((input) => {
    const container = input.closest(".admin-image-upload");
    const previewContainer = container?.querySelector("[data-product-image-preview-container]");
    const preview = container?.querySelector("[data-product-image-preview]");
    const message = container?.querySelector("[data-product-image-message]");
    const originalSource = preview?.getAttribute("src") || "";
    let previewUrl = null;

    input.addEventListener("change", () => {
      const file = input.files?.[0];
      if (!file) {
        mostrarEstado(input, previewContainer, preview, message, originalSource, "");
        return;
      }

      const extension = file.name.split(".").pop()?.toLowerCase() || "";
      if (!FORMATOS_PERMITIDOS.has(extension)) {
        input.value = "";
        mostrarEstado(input, previewContainer, preview, message, originalSource, "Elige una imagen JPG, PNG o WebP.", true);
        return;
      }
      if (file.size > TAMANO_MAXIMO_BYTES) {
        input.value = "";
        mostrarEstado(input, previewContainer, preview, message, originalSource, "La imagen no puede superar los 5 MB.", true);
        return;
      }

      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      previewUrl = URL.createObjectURL(file);
      preview.src = previewUrl;
      previewContainer.hidden = false;
      input.classList.remove("is-invalid");
      message.textContent = `Vista previa: ${file.name}`;
      message.classList.remove("text-danger");
    });

    function mostrarEstado(campo, contenedor, imagen, mensaje, fuenteOriginal, texto, esError = false) {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
        previewUrl = null;
      }
      if (fuenteOriginal) {
        imagen.src = fuenteOriginal;
        contenedor.hidden = false;
      } else {
        imagen.removeAttribute("src");
        contenedor.hidden = true;
      }
      campo.classList.toggle("is-invalid", esError);
      mensaje.textContent = texto;
      mensaje.classList.toggle("text-danger", esError);
    }
  });

  root.querySelectorAll('[data-open-modal-on-load="true"]').forEach((modalElement) => {
    const bootstrapModal = window.bootstrap?.Modal;
    if (bootstrapModal) {
      bootstrapModal.getOrCreateInstance(modalElement).show();
    }
  });
}
