const CATEGORY_FILTER = "[data-filtro-categoria]";
const PRICE_FILTER = "[data-filtro-precio]";
const SEARCH_DELAY_MS = 350;

function updateFilterButtons(form, selector, activeButton) {
  form.querySelectorAll(selector).forEach((button) => {
    const isActive = button === activeButton;
    button.classList.toggle("btn-primary", isActive);
    button.classList.toggle("btn-outline-primary", !isActive);
    button.setAttribute("aria-pressed", String(isActive));
  });
}

function restoreFilterState(form, input, selector, dataKey) {
  const selectedValue = input.value || "todos";
  const activeButton = [...form.querySelectorAll(selector)].find(
    (button) => button.dataset[dataKey] === selectedValue,
  );

  if (activeButton) updateFilterButtons(form, selector, activeButton);
}

function buildCatalogUrl(form) {
  const url = new URL(form.action, window.location.href);
  const parameters = new URLSearchParams();

  new FormData(form).forEach((value, name) => {
    const text = String(value).trim();
    if (text && text !== "todos") parameters.set(name, text);
  });

  url.search = parameters.toString();
  return url;
}

function updateBrowserHistory(url, mode) {
  if (mode === "none" || url.href === window.location.href) return;

  if (mode === "push") {
    window.history.pushState({}, "", url);
    return;
  }

  window.history.replaceState({}, "", url);
}

export function initializeCatalogFilters(root = document) {
  const form = root.querySelector("#catalogo-filtros");
  if (!form) return;

  const categoryInput = form.querySelector("#filtro-categoria");
  const priceInput = form.querySelector("#filtro-precio");
  const searchInput = form.querySelector("#busqueda-catalogo");
  const resultMessage = form.querySelector("#resultado-catalogo");
  const productGrid = form.querySelector("#grid-productos");

  if (!categoryInput || !priceInput || !searchInput || !resultMessage || !productGrid) return;

  let searchTimeout;
  let activeRequest;

  const refreshCatalog = async (url, historyMode) => {
    activeRequest?.abort();
    const request = new AbortController();
    activeRequest = request;

    form.setAttribute("aria-busy", "true");
    productGrid.setAttribute("aria-busy", "true");
    resultMessage.textContent = "Actualizando productos...";

    try {
      const response = await fetch(url, { signal: request.signal });

      if (!response.ok) throw new Error("No se pudo consultar el catálogo.");

      const html = await response.text();
      const nextDocument = new DOMParser().parseFromString(html, "text/html");
      const nextGrid = nextDocument.querySelector("#grid-productos");
      const nextResultMessage = nextDocument.querySelector("#resultado-catalogo");

      if (!nextGrid || !nextResultMessage) {
        throw new Error("La respuesta no contiene los resultados del catálogo.");
      }

      if (request.signal.aborted) return;

      productGrid.innerHTML = nextGrid.innerHTML;
      resultMessage.textContent = nextResultMessage.textContent;
      updateBrowserHistory(url, historyMode);
    } catch (error) {
      if (error.name === "AbortError") return;
      window.location.assign(url.href);
    } finally {
      if (activeRequest === request) {
        form.removeAttribute("aria-busy");
        productGrid.removeAttribute("aria-busy");
        activeRequest = undefined;
      }
    }
  };

  restoreFilterState(form, categoryInput, CATEGORY_FILTER, "filtroCategoria");
  restoreFilterState(form, priceInput, PRICE_FILTER, "filtroPrecio");

  form.addEventListener("click", (event) => {
    const button = event.target.closest(`${CATEGORY_FILTER}, ${PRICE_FILTER}`);
    if (!button || !form.contains(button)) return;

    window.clearTimeout(searchTimeout);

    const isCategoryFilter = button.matches(CATEGORY_FILTER);
    const input = isCategoryFilter ? categoryInput : priceInput;
    const selector = isCategoryFilter ? CATEGORY_FILTER : PRICE_FILTER;
    const dataKey = isCategoryFilter ? "filtroCategoria" : "filtroPrecio";
    const selectedValue = button.dataset[dataKey] === "todos" ? "" : button.dataset[dataKey];

    if (input.value === selectedValue) return;

    input.value = selectedValue;
    updateFilterButtons(form, selector, button);
    refreshCatalog(buildCatalogUrl(form), "push");
  });

  searchInput.addEventListener("input", () => {
    window.clearTimeout(searchTimeout);
    searchTimeout = window.setTimeout(() => {
      refreshCatalog(buildCatalogUrl(form), "replace");
    }, SEARCH_DELAY_MS);
  });

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    window.clearTimeout(searchTimeout);
    refreshCatalog(buildCatalogUrl(form), "push");
  });

  window.addEventListener("popstate", () => {
    const url = new URL(window.location.href);
    const catalogPath = new URL(form.action, window.location.href).pathname;
    if (url.pathname !== catalogPath) return;

    searchInput.value = url.searchParams.get("q") || "";
    categoryInput.value = url.searchParams.get("categoria") || "";
    priceInput.value = url.searchParams.get("precio") || "";
    restoreFilterState(form, categoryInput, CATEGORY_FILTER, "filtroCategoria");
    restoreFilterState(form, priceInput, PRICE_FILTER, "filtroPrecio");
    refreshCatalog(url, "none");
  });
}
