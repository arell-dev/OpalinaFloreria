const CATEGORY_FILTER = "[data-filtro-categoria]";
const PRICE_FILTER = "[data-filtro-precio]";

function updateFilterButtons(form, selector, activeButton) {
  form.querySelectorAll(selector).forEach((button) => {
    const isActive = button === activeButton;
    button.classList.toggle("btn-primary", isActive);
    button.classList.toggle("btn-outline-primary", !isActive);
    button.setAttribute("aria-pressed", String(isActive));
  });
}

function selectFilter(form, input, selector, button, dataKey) {
  input.value =
    button.dataset[dataKey] === "todos" ? "" : button.dataset[dataKey];
  updateFilterButtons(form, selector, button);
  form.requestSubmit();
}

function restoreFilterState(form, input, selector, dataKey) {
  const selectedValue = input.value || "todos";
  const activeButton = [...form.querySelectorAll(selector)].find(
    (button) => button.dataset[dataKey] === selectedValue,
  );

  if (activeButton) updateFilterButtons(form, selector, activeButton);
}

export function initializeCatalogFilters(root = document) {
  const form = root.querySelector("#catalogo-filtros");
  if (!form) return;

  const categoryInput = form.querySelector("#filtro-categoria");
  const priceInput = form.querySelector("#filtro-precio");
  if (!categoryInput || !priceInput) return;

  restoreFilterState(form, categoryInput, CATEGORY_FILTER, "filtroCategoria");
  restoreFilterState(form, priceInput, PRICE_FILTER, "filtroPrecio");

  form.addEventListener("click", (event) => {
    const categoryButton = event.target.closest(CATEGORY_FILTER);
    const priceButton = event.target.closest(PRICE_FILTER);

    if (categoryButton && form.contains(categoryButton)) {
      selectFilter(
        form,
        categoryInput,
        CATEGORY_FILTER,
        categoryButton,
        "filtroCategoria",
      );
    } else if (priceButton && form.contains(priceButton)) {
      selectFilter(form, priceInput, PRICE_FILTER, priceButton, "filtroPrecio");
    }
  });
}
