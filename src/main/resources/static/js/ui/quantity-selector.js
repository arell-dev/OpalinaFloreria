const QUANTITY_SELECTOR = "[data-cantidad-control]";

function updateButtons(input, decreaseButton, increaseButton, minimum, maximum) {
  const quantity = Number(input.value);
  const hasValidQuantity = input.value !== "" && Number.isInteger(quantity);

  decreaseButton.disabled = !hasValidQuantity || quantity <= minimum;
  increaseButton.disabled = !hasValidQuantity || quantity >= maximum;
}

function initializeQuantitySelector(container) {
  const input = container.querySelector("[data-cantidad-input]");
  const decreaseButton = container.querySelector("[data-cantidad-decrease]");
  const increaseButton = container.querySelector("[data-cantidad-increase]");

  if (!input || !decreaseButton || !increaseButton) return;

  const minimum = Number(input.dataset.min) || 1;
  const maximum = Number(input.dataset.max);

  if (!Number.isInteger(maximum) || maximum < minimum) return;

  const setQuantity = (quantity) => {
    const boundedQuantity = Math.min(maximum, Math.max(minimum, quantity));
    input.value = String(boundedQuantity);
    updateButtons(input, decreaseButton, increaseButton, minimum, maximum);
  };

  input.addEventListener("input", () => {
    const digitsOnly = input.value.replace(/\D/g, "");

    if (digitsOnly !== input.value) input.value = digitsOnly;

    if (!digitsOnly) {
      updateButtons(input, decreaseButton, increaseButton, minimum, maximum);
      return;
    }

    setQuantity(Number.parseInt(digitsOnly, 10));
  });

  input.addEventListener("blur", () => {
    if (!input.value) setQuantity(minimum);
  });

  decreaseButton.addEventListener("click", () => {
    setQuantity(Number(input.value || minimum) - 1);
  });

  increaseButton.addEventListener("click", () => {
    setQuantity(Number(input.value || minimum) + 1);
  });

  updateButtons(input, decreaseButton, increaseButton, minimum, maximum);
}

export function initializeQuantitySelectors(root = document) {
  root.querySelectorAll(QUANTITY_SELECTOR).forEach(initializeQuantitySelector);
}
