const CHECKOUT_FORM_SELECTOR = "#formulario-checkout";
const CHECKOUT_PANEL_SELECTOR = "[data-checkout-panel]";
const CHECKOUT_STEP_SELECTOR = "[data-checkout-step]";

function getStepNumber(element, attribute) {
  return Number(element.dataset[attribute]);
}

function initializeDeliveryDate(form) {
  const dateInput = form.querySelector("#fechaEntrega");
  if (!dateInput) return;

  const earliestDate = new Date();
  earliestDate.setDate(earliestDate.getDate() + 1);
  dateInput.min = [
    earliestDate.getFullYear(),
    String(earliestDate.getMonth() + 1).padStart(2, "0"),
    String(earliestDate.getDate()).padStart(2, "0"),
  ].join("-");
}

function createPanelValidator(form, panels) {
  return function validatePanel(stepNumber) {
    const panel = panels.find(
      (candidate) => getStepNumber(candidate, "checkoutPanel") === stepNumber,
    );
    if (!panel) return false;

    const invalidControl = [
      ...panel.querySelectorAll("input, select, textarea"),
    ]
      .filter((control) => !control.disabled)
      .find((control) => !control.checkValidity());

    const summary = panel.querySelector("[data-checkout-validation-summary]");
    if (!invalidControl) {
      if (summary) summary.hidden = true;
      return true;
    }

    form.classList.add("was-validated");
    if (summary) summary.hidden = false;
    invalidControl.reportValidity();
    invalidControl.focus();
    invalidControl.scrollIntoView({ behavior: "smooth", block: "center" });
    return false;
  };
}

function updateDeliveryFields(form) {
  const deliveryMode = form.querySelector("#modalidad-entrega");
  const needsAddress = deliveryMode?.value !== "RECOJO";

  form.querySelectorAll("[data-address-fields]").forEach((container) => {
    container.hidden = !needsAddress;
    container.querySelectorAll("input").forEach((input) => {
      input.disabled = !needsAddress;
      input.required = needsAddress && input.id === "clienteDireccion";
    });
  });
}

function renderCheckoutStep(panels, steps, stepNumber) {
  panels.forEach((panel) => {
    panel.hidden = getStepNumber(panel, "checkoutPanel") !== stepNumber;
  });

  steps.forEach((step) => {
    const stepValue = getStepNumber(step, "checkoutStep");
    const isActive = stepValue === stepNumber;

    if (isActive) {
      step.setAttribute("aria-current", "step");
    } else {
      step.removeAttribute("aria-current");
    }

    step.classList.toggle("is-current", isActive);
    step.classList.toggle("is-complete", stepValue < stepNumber);
  });

  panels
    .find((panel) => getStepNumber(panel, "checkoutPanel") === stepNumber)
    ?.querySelector("h2")
    ?.focus({ preventScroll: true });
}

function formatCheckoutReview(form) {
  const review = form.querySelector("#checkout-review");
  if (!review) return;

  const getValue = (selector) =>
    form.querySelector(selector)?.value.trim() ?? "";
  const getSelectedLabel = (selector) =>
    form.querySelector(selector)?.selectedOptions[0]?.textContent.trim() ?? "";
  const isPickup = getValue("#modalidad-entrega") === "RECOJO";
  const address = isPickup ? "Recojo en tienda" : getValue("#clienteDireccion");
  const date = getValue("#fechaEntrega");
  const delivery = [
    getSelectedLabel("#modalidad-entrega"),
    address,
    date,
    getSelectedLabel("#horaEntrega"),
  ]
    .filter(Boolean)
    .join(" · ");

  review.querySelector("#checkout-review-contact").textContent = [
    getValue("#clienteNombre"),
    getValue("#clienteCorreo"),
    getValue("#clienteTelefono"),
  ]
    .filter(Boolean)
    .join(" · ");
  review.querySelector("#checkout-review-delivery").textContent = delivery;
  review.querySelector("#checkout-review-payment").textContent =
    getSelectedLabel("#metodo-pago");
}

export function initializeCheckoutWizard(root = document) {
  const form = root.querySelector(CHECKOUT_FORM_SELECTOR);
  if (!form) return;

  const panels = [...form.querySelectorAll(CHECKOUT_PANEL_SELECTOR)];
  const steps = [...root.querySelectorAll(CHECKOUT_STEP_SELECTOR)];
  const isConfirmed = Boolean(form.querySelector("#checkout-success"));
  const hasServerErrors = Boolean(form.querySelector(".is-invalid"));
  if (panels.length === 0 || steps.length === 0) return;

  const validatePanel = createPanelValidator(form, panels);
  const initialStep = isConfirmed ? 3 : hasServerErrors ? 2 : 1;
  let currentStep = initialStep;

  initializeDeliveryDate(form);
  updateDeliveryFields(form);
  if (hasServerErrors) {
    form.classList.add("was-validated");
    const summary = form.querySelector(
      '[data-checkout-panel="2"] [data-checkout-validation-summary]',
    );
    if (summary) summary.hidden = false;
  }
  form.querySelector("#modalidad-entrega")?.addEventListener("change", () => {
    updateDeliveryFields(form);
  });

  form.addEventListener("click", (event) => {
    const nextButton = event.target.closest("[data-checkout-next]");
    const backButton = event.target.closest("[data-checkout-back]");

    if (nextButton && form.contains(nextButton)) {
      const panelStep = Number(
        nextButton.closest(CHECKOUT_PANEL_SELECTOR)?.dataset.checkoutPanel,
      );
      const nextStep = Number(nextButton.dataset.checkoutNext);
      if (!validatePanel(panelStep)) return;
      if (nextStep === 3) formatCheckoutReview(form);
      currentStep = nextStep;
      renderCheckoutStep(panels, steps, nextStep);
    }

    if (backButton && form.contains(backButton)) {
      currentStep = Number(backButton.dataset.checkoutBack);
      renderCheckoutStep(panels, steps, currentStep);
    }
  });

  form.addEventListener("submit", (event) => {
    if (currentStep < 3) {
      event.preventDefault();

      if (currentStep === 1) {
        currentStep = 2;
        renderCheckoutStep(panels, steps, currentStep);
        return;
      }

      if (validatePanel(currentStep)) {
        formatCheckoutReview(form);
        currentStep = 3;
        renderCheckoutStep(panels, steps, currentStep);
      }
      return;
    }

    if (!validatePanel(3)) event.preventDefault();
  });

  renderCheckoutStep(panels, steps, currentStep);
}
