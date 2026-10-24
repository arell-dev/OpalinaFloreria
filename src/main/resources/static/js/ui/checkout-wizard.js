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

    const invalidControl = [...panel.querySelectorAll("input, select, textarea")]
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

function updateDeliverySummary(form) {
  const summary = form.querySelector(".checkout-summary");
  const mode = form.querySelector("#modalidad-entrega")?.value;
  if (!summary || !mode) return;

  const subtotal = Number(summary.dataset.subtotal) || 0;
  const deliveryCost = Number(summary.dataset.deliveryCost) || 0;
  const pickup = mode === "RECOJO";
  const delivery = pickup ? 0 : deliveryCost;
  const formatter = new Intl.NumberFormat("es-ES", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  summary.querySelector("#checkout-delivery-label").textContent = pickup
    ? "Recojo en tienda"
    : "Delivery";
  const dateLabel = form.querySelector("#fecha-entrega-label");
  if (dateLabel) {
    dateLabel.textContent = pickup ? "Fecha de recojo" : "Fecha de entrega";
  }
  summary.querySelector("#checkout-delivery").textContent = `S/ ${formatter.format(delivery)}`;
  summary.querySelector("#checkout-total").textContent = `S/ ${formatter.format(subtotal + delivery)}`;
}

function updatePaymentSimulation(form) {
  const simulation = form.querySelector("#checkout-payment-simulation");
  const title = form.querySelector("#checkout-payment-title");
  const selectedPayment = form.querySelector('input[name="metodoPago"]:checked');
  if (!simulation || !title) return;

  const paymentNames = {
    YAPE: "Yape",
    PLIN: "Plin",
    TRANSFERENCIA: "transferencia bancaria",
  };

  simulation.hidden = !selectedPayment;
  if (selectedPayment) {
    const methodName = paymentNames[selectedPayment.value] ?? selectedPayment.value;
    title.textContent = `Método seleccionado: ${methodName}.`;
  }
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
  const getValue = (selector) => form.querySelector(selector)?.value.trim() ?? "";
  const getSelectedLabel = (selector) =>
    form.querySelector(selector)?.selectedOptions[0]?.textContent.trim() ?? "";
  const pickup = getValue("#modalidad-entrega") === "RECOJO";
  const address = pickup ? "" : getValue("#clienteDireccion");
  const date = getValue("#fechaEntrega");
  const delivery = [
    pickup ? "Recojo en tienda" : getSelectedLabel("#modalidad-entrega"),
    address,
    date,
    getSelectedLabel("#horaEntrega"),
  ]
    .filter(Boolean)
    .join(" · ");
  form.querySelector("#checkout-review-contact").textContent = [
    getValue("#clienteNombre"),
    getValue("#clienteTelefono"),
  ]
    .filter(Boolean)
    .join(" · ");
  form.querySelector("#checkout-review-delivery").textContent = delivery;
}

export function initializeCheckoutWizard(root = document) {
  const form = root.querySelector(CHECKOUT_FORM_SELECTOR);
  if (!form) return;

  const panels = [...form.querySelectorAll(CHECKOUT_PANEL_SELECTOR)];
  const steps = [...root.querySelectorAll(CHECKOUT_STEP_SELECTOR)];
  const firstInvalidControl = form.querySelector(".is-invalid");
  const errorPanel = firstInvalidControl?.closest(CHECKOUT_PANEL_SELECTOR);
  const errorStep = errorPanel
    ? getStepNumber(errorPanel, "checkoutPanel")
    : null;
  if (panels.length === 0 || steps.length === 0) return;

  const validatePanel = createPanelValidator(form, panels);
  const initialStep = errorStep ?? 1;
  let currentStep = initialStep;

  initializeDeliveryDate(form);
  updateDeliveryFields(form);
  updateDeliverySummary(form);
  formatCheckoutReview(form);
  updatePaymentSimulation(form);

  if (errorStep) {
    form.classList.add("was-validated");
    const summary = form.querySelector(
      `[data-checkout-panel="${errorStep}"] [data-checkout-validation-summary]`,
    );
    if (summary) summary.hidden = false;
  }

  form.addEventListener("change", (event) => {
    if (event.target.matches("#modalidad-entrega")) {
      updateDeliveryFields(form);
      updateDeliverySummary(form);
    }

    if (event.target.matches('input[name="metodoPago"]')) {
      updatePaymentSimulation(form);
    }
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
    if (currentStep < 4) {
      event.preventDefault();
      if (!validatePanel(currentStep)) return;

      currentStep += 1;
      if (currentStep === 3) formatCheckoutReview(form);
      renderCheckoutStep(panels, steps, currentStep);
      return;
    }

    if (!validatePanel(4)) event.preventDefault();
  });

  renderCheckoutStep(panels, steps, currentStep);
}
