import { authService } from "../services/auth-service.js";
import { storeService } from "../services/store-service.js";
import { clear, createElement, formatDate, formatMoney, pagePath, protectPage, setMessage, updateNavigation } from "../ui/dom.js";

const STATUS_LABELS = {
  pendiente: "Pendiente",
  proceso: "En proceso",
  enviado: "Enviado",
  entregado: "Entregado",
  cancelado: "Cancelado",
};
const STATUS_CLASSES = {
  pendiente: "",
  proceso: "estado-proceso",
  enviado: "estado-enviado",
  entregado: "estado-entregado",
  cancelado: "estado-cancelado",
};
const byId = (id) => document.getElementById(id);
const $ = (selector, root = document) => root.querySelector(selector);

function productCard(product) {
  const column = createElement("div", { className: "col" });
  const article = createElement("article", {
    className: "card h-100 overflow-hidden",
  });
  const imageFrame = createElement("div", { className: "ratio ratio-4x3" });
  imageFrame.append(
    createElement("img", {
      attrs: { src: product.image, alt: product.name, loading: "lazy" },
      className: "card-img-top w-100 h-100 object-fit-cover",
    }),
  );
  const body = createElement("div", {
    className: "card-body d-flex flex-column",
  });
  body.append(
    createElement("span", {
      className: "insignia align-self-start",
      text: product.category,
    }),
  );
  body.append(
    createElement("h3", { className: "h4 mt-3", text: product.name }),
  );
  body.append(
    createElement("p", {
      className: "card-text text-body-secondary",
      text: product.description,
    }),
  );
  const footer = createElement("div", {
    className:
      "d-flex align-items-center justify-content-between gap-2 mt-auto pt-3",
  });
  footer.append(createElement("strong", { text: formatMoney(product.price) }));
  footer.append(
    createElement("a", {
      className: "btn btn-outline-primary btn-sm",
      text: "Ver más",
      attrs: {
        href: pagePath(
          `producto.html?producto=${encodeURIComponent(product.id)}`,
        ),
      },
    }),
  );
  body.append(footer);
  article.append(imageFrame, body);
  column.append(article);
  return column;
}

function renderProductList(
  host,
  products,
  emptyText = "No hay productos que coincidan con los filtros.",
) {
  clear(host);
  if (!products.length) {
    host.append(
      createElement("div", {
        className: "col-12",
        children: [
          createElement("div", {
            className: "alert alert-light text-center",
            text: emptyText,
            attrs: { role: "status" },
          }),
        ],
      }),
    );
    return;
  }
  products.forEach((product) => host.append(productCard(product)));
}

export function initHome() {
  const host = byId("productos-destacados");
  if (!host) return;
  const render = () =>
    renderProductList(host, storeService.listFeaturedProducts());
  render();
  window.addEventListener("storage", (event) => {
    if (event.key === null || event.key?.startsWith("opalina-")) render();
  });
}

export function initCatalog() {
  const host = byId("grid-productos");
  if (!host) return;
  const search = byId("busqueda-catalogo");
  const categoryButtons = document.querySelectorAll("[data-filtro-categoria]");
  const priceButtons = document.querySelectorAll("[data-filtro-precio]");
  const count = byId("resultado-catalogo");
  let category = "todos";
  let price = "todos";
  const render = () => {
    const products = storeService.searchProducts({
      query: search?.value ?? "",
      category,
      price,
    });
    renderProductList(host, products);
    if (count)
      count.textContent = `${products.length} ${products.length === 1 ? "producto" : "productos"}`;
  };
  window.addEventListener("storage", (event) => {
    if (event.key === null || event.key?.startsWith("opalina-")) render();
  });
  search?.addEventListener("input", render);
  categoryButtons.forEach((button) =>
    button.addEventListener("click", () => {
      category = button.dataset.filtroCategoria;
      categoryButtons.forEach((item) => {
        const active = item === button;
        item.classList.toggle("btn-primary", active);
        item.classList.toggle("btn-outline-primary", !active);
        item.setAttribute("aria-pressed", String(active));
      });
      render();
    }),
  );
  priceButtons.forEach((button) =>
    button.addEventListener("click", () => {
      price = button.dataset.filtroPrecio;
      priceButtons.forEach((item) => {
        const active = item === button;
        item.classList.toggle("btn-primary", active);
        item.classList.toggle("btn-outline-primary", !active);
        item.setAttribute("aria-pressed", String(active));
      });
      render();
    }),
  );
  render();
}

export function initProductDetail() {
  const host = byId("detalle-producto");
  if (!host) return;
  const id =
    new URLSearchParams(location.search).get("producto") ||
    storeService.listFeaturedProducts()[0]?.id;
  const product = storeService.getProduct(id);
  const image = host.querySelector("img");
  const title = byId("nombre-producto");
  const category = host.querySelector(".insignia");
  const price = host.querySelector(".h3.text-success");
  const description = title?.parentElement.querySelector(
    "p.text-body-secondary.mb-4",
  );
  const stock = host.querySelector(".list-group-item .text-body-secondary");
  const quantity = byId("cantidad");
  const addButton = byId("btn-agregar");
  if (!product) {
    clear(host);
    const message = createElement("div", {
      className: "col-12",
      children: [
        createElement("div", {
          className: "alert alert-warning",
          text: "No se encontró este producto. Puede haber sido retirado del catálogo.",
          attrs: { role: "status" },
        }),
        createElement("a", {
          className: "btn btn-primary",
          text: "Volver al catálogo",
          attrs: { href: pagePath("catalogo.html") },
        }),
      ],
    });
    host.append(message);
    return;
  }
  document.title = `${product.name} | Opalina Florería Piura`;
  if (title) title.textContent = product.name;
  if (image) {
    image.src = product.image;
    image.alt = product.name;
  }
  if (category) category.textContent = product.category;
  if (price) price.textContent = formatMoney(product.price);
  if (description) description.textContent = product.description;
  if (stock)
    stock.textContent = !product.available
      ? "No disponible"
      : product.stock
        ? `${product.stock} en stock`
        : "Sin stock";
  if (quantity) {
    quantity.max = String(product.stock);
    quantity.value = "1";
    quantity.disabled = !product.available || product.stock < 1;
  }
  if (addButton) {
    addButton.dataset.productId = product.id;
    addButton.setAttribute(
      "aria-disabled",
      String(!product.available || product.stock < 1),
    );
    addButton.textContent =
      product.available && product.stock
        ? "Agregar al carrito"
        : product.available
          ? "Agotado"
          : "No disponible";
    addButton.classList.toggle(
      "disabled",
      !product.available || product.stock < 1,
    );
    addButton.addEventListener("click", (event) => {
      event.preventDefault();
      try {
        storeService.addToCart(product.id, Number(quantity?.value ?? 1));
        try {
          sessionStorage.setItem(
            "opalina-feedback",
            "Producto agregado al carrito.",
          );
        } catch {
          // El carrito se conserva en localStorage aunque no haya sesión temporal.
        }
        location.href = pagePath("carrito.html");
      } catch (error) {
        quantity?.setCustomValidity(error.message);
        quantity?.reportValidity();
      }
    });
  }
}

function renderCart() {
  const host = byId("items-carrito");
  if (!host) return;
  const summary = storeService.cartSummary();
  clear(host);
  summary.items.forEach(({ product, quantity }) => {
    const article = createElement("article", {
      className: "card shadow-sm carrito-item",
    });
    const body = createElement("div", { className: "card-body p-3 p-md-4" });
    const row = createElement("div", {
      className: "row g-3 align-items-center",
    });
    const pictureColumn = createElement("div", {
      className: "col-4 col-sm-auto",
    });
    const picture = createElement("div", {
      className: "ratio ratio-1x1 rounded overflow-hidden miniatura-carrito",
    });
    picture.append(
      createElement("img", {
        className: "w-100 h-100 object-fit-cover",
        attrs: { src: product.image, alt: product.name },
      }),
    );
    pictureColumn.append(picture);
    const info = createElement("div", { className: "col-8 col-sm" });
    info.append(
      createElement("h2", { className: "h5 mb-1", text: product.name }),
    );
    info.append(
      createElement("p", {
        className: "mb-0 fw-bold",
        text: `${formatMoney(product.price)} por unidad`,
      }),
    );
    const qty = createElement("div", { className: "col-7 col-sm-auto" });
    const group = createElement("div", {
      className: "input-group input-group-sm",
    });
    const minus = createElement("button", {
      className: "btn btn-outline-secondary",
      text: "−",
      attrs: {
        type: "button",
        "data-cart-action": "decrease",
        "data-product-id": product.id,
        "aria-label": `Disminuir ${product.name}`,
      },
    });
    const qtyValue = createElement("span", {
      className: "input-group-text bg-white cantidad-carrito",
      text: quantity,
      attrs: { "aria-live": "polite" },
    });
    const plus = createElement("button", {
      className: "btn btn-outline-secondary",
      text: "+",
      attrs: {
        type: "button",
        "data-cart-action": "increase",
        "data-product-id": product.id,
        "aria-label": `Aumentar ${product.name}`,
        disabled: quantity >= product.stock,
      },
    });
    group.append(minus, qtyValue, plus);
    qty.append(
      group,
      createElement("small", {
        className: "text-body-secondary",
        text: `${product.stock} disponibles`,
      }),
    );
    const actions = createElement("div", {
      className: "col-5 col-sm-auto text-end",
    });
    actions.append(
      createElement("button", {
        className: "btn btn-outline-primary btn-sm",
        text: "Quitar",
        attrs: {
          type: "button",
          "data-cart-action": "remove",
          "data-product-id": product.id,
          "aria-label": `Quitar ${product.name}`,
        },
      }),
    );
    actions.append(
      createElement("strong", {
        className: "d-block mt-2",
        text: formatMoney(product.price * quantity),
      }),
    );
    row.append(pictureColumn, info, qty, actions);
    body.append(row);
    article.append(body);
    host.append(article);
  });
  const empty = byId("carrito-vacio");
  if (empty) empty.hidden = summary.items.length > 0;
  host.hidden = summary.items.length === 0;
  const checkout = document.querySelector("[data-ir-a-pedido]");
  if (checkout) {
    const isEmpty = summary.items.length === 0;
    checkout.classList.toggle("checkout-disabled", isEmpty);
    checkout.setAttribute("aria-disabled", String(isEmpty));
    const checkoutHint = byId("checkout-vacio-ayuda");
    if (checkoutHint) checkoutHint.hidden = !isEmpty;
  }
  if (byId("subtotal-carrito"))
    byId("subtotal-carrito").textContent = formatMoney(summary.subtotal);
  if (byId("delivery-carrito"))
    byId("delivery-carrito").textContent = summary.deliveryFee
      ? formatMoney(summary.deliveryFee)
      : "Gratis";
  if (byId("total-carrito"))
    byId("total-carrito").textContent = formatMoney(summary.total);
}

export function initCart() {
  const host = byId("items-carrito");
  if (!host) return;
  const notice = byId("aviso-carrito");
  document
    .querySelector("[data-ir-a-pedido]")
    ?.addEventListener("click", (event) => {
      if (storeService.cartSummary().items.length) return;
      event.preventDefault();
      setMessage(
        notice,
        "Tu carrito está vacío. Agrega un producto para continuar.",
        "warning",
      );
    });
  try {
    const feedback = sessionStorage.getItem("opalina-feedback");
    if (feedback && notice) {
      setMessage(notice, feedback);
      sessionStorage.removeItem("opalina-feedback");
    }
  } catch {
    /* La tienda sigue siendo navegable si la sesión está bloqueada. */
  }
  host.addEventListener("click", (event) => {
    const button = event.target.closest?.("[data-cart-action]");
    if (!button) return;
    const entry = storeService
      .getCart()
      .find((item) => item.productId === button.dataset.productId);
    if (!entry) return;
    const action = button.dataset.cartAction;
    const next =
      action === "increase"
        ? entry.quantity + 1
        : action === "decrease"
          ? Math.max(1, entry.quantity - 1)
          : 0;
    try {
      storeService.updateCartItem(entry.productId, next);
      renderCart();
    } catch (error) {
      setMessage(notice, error.message, "danger");
    }
  });
  window.addEventListener("storage", (event) => {
    if (event.key === null || event.key?.startsWith("opalina-")) renderCart();
  });
  renderCart();
}

function renderSteps(current) {
  const host = document.querySelector("[data-checkout-steps]");
  if (!host) return;
  host.querySelectorAll("[data-checkout-step]").forEach((item) => {
    const step = Number(item.dataset.checkoutStep);
    const complete = step < current;
    const active = step === current || (current === 4 && step === 3);
    item.classList.toggle("is-current", step === current);
    item.classList.toggle("is-complete", complete);
    if (active) item.setAttribute("aria-current", "step");
    else item.removeAttribute("aria-current");
    const number = item.querySelector(".checkout-progress-number");
    if (number) number.textContent = complete ? "✓" : String(step);
  });
}

function cartLine(item, compact = false) {
  const line = createElement("article", {
    className: `checkout-item${compact ? " checkout-item--compact" : ""}`,
  });
  line.append(
    createElement("img", {
      className: "checkout-item-image",
      attrs: {
        src: item.product.image,
        alt: item.product.name,
        loading: "lazy",
      },
    }),
  );
  const details = createElement("div", { className: "checkout-item-details" });
  details.append(
    createElement("h3", {
      className: compact ? "h6 mb-1" : "h5 mb-1",
      text: item.product.name,
    }),
    createElement("p", {
      className: "text-body-secondary mb-0",
      text: `Cantidad: ${item.quantity}`,
    }),
  );
  line.append(details);
  line.append(
    createElement("strong", {
      className: "checkout-item-price",
      text: formatMoney(item.product.price * item.quantity),
    }),
  );
  return line;
}

function renderCheckoutSummary(summary) {
  const itemsHost = byId("checkout-items");
  const compactHost = byId("resumen-pedido");
  [itemsHost, compactHost].forEach((host) => {
    if (!host) return;
    clear(host);
    summary.items.forEach((item) =>
      host.append(cartLine(item, host === compactHost)),
    );
  });
  updateCheckoutTotals(summary, summary.deliveryFee);
}

function updateCheckoutTotals(summary, deliveryFee) {
  const delivery = deliveryFee ? formatMoney(deliveryFee) : "Gratis";
  if (byId("checkout-subtotal"))
    byId("checkout-subtotal").textContent = formatMoney(summary.subtotal);
  if (byId("checkout-delivery"))
    byId("checkout-delivery").textContent = delivery;
  if (byId("checkout-total"))
    byId("checkout-total").textContent = formatMoney(
      summary.subtotal + deliveryFee,
    );
}

function setCheckoutCustomValidity(control) {
  if (control.id === "clienteNombre")
    control.setCustomValidity(
      control.value.trim().length >= 2
        ? ""
        : "Escribe el nombre completo de quien recibirá el pedido.",
    );
  if (control.id === "clienteDireccion")
    control.setCustomValidity(
      control.value.trim()
        ? ""
        : "Escribe la dirección completa para la entrega.",
    );
}

function validateCheckoutPanel(panel, notice) {
  if (!panel) return true;
  const controls = [
    ...panel.querySelectorAll("input, select, textarea"),
  ].filter((control) => !control.disabled);
  controls.forEach(setCheckoutCustomValidity);
  controls.forEach((control) => control.classList.remove("is-invalid"));
  const invalid = controls.filter((control) => !control.checkValidity());
  if (!invalid.length) return true;
  invalid.forEach((control) => control.classList.add("is-invalid"));
  const firstInvalid = invalid[0];
  const message =
    firstInvalid.id === "acepta-pedido"
      ? "Confirma que los datos de entrega y los productos son correctos para continuar."
      : "Revisa los campos marcados. Completa los datos obligatorios con el formato indicado.";
  setMessage(notice, message, "danger");
  firstInvalid.focus();
  firstInvalid.reportValidity();
  return false;
}

function localDateOffset(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
}

function initCheckoutReview(form) {
  const contact = byId("checkout-review-contact");
  if (contact)
    contact.textContent = [
      byId("clienteNombre").value.trim(),
      byId("clienteTelefono").value.trim(),
      byId("clienteCorreo").value.trim(),
    ].join("\n");
  const delivery = byId("checkout-review-delivery");
  if (delivery) {
    const mode = form.elements.modalidad.value;
    const date = formatDate(byId("fechaEntrega").value);
    const time =
      form.elements.horaEntrega.selectedOptions[0]?.textContent.trim();
    const details = [mode];
    if (mode !== "Recojo en tienda") {
      details.push(byId("clienteDireccion").value.trim());
      const reference = byId("clienteReferencia").value.trim();
      if (reference) details.push(`Referencia: ${reference}`);
    }
    details.push(`Fecha: ${date}`, `Horario: ${time}`);
    const notes = byId("mensaje").value.trim();
    if (notes) details.push(`Mensaje: ${notes}`);
    delivery.textContent = details.join("\n");
  }
  const payment = byId("checkout-review-payment");
  if (payment) payment.textContent = form.elements.metodoPago.value;
}

export function initCheckout() {
  const form = document.querySelector("[data-checkout-form]");
  if (!form || !protectPage("cliente")) return;
  const customer = authService.customer();
  const summary = storeService.cartSummary();
  const notice = byId("aviso-pedido");
  const requestedOrderId = new URLSearchParams(location.search).get("pedido");
  const previousOrder = requestedOrderId
    ? storeService.getOrder(requestedOrderId)
    : null;
  if (previousOrder?.customerId !== customer?.id && !summary.items.length) {
    location.replace(pagePath("carrito.html"));
    return;
  }

  const orderSummary =
    previousOrder?.customerId === customer?.id
      ? {
          items: previousOrder.items.map((item) => ({
            product: {
              name: item.name,
              image: item.image,
              price: item.unitPrice,
            },
            quantity: item.quantity,
          })),
          subtotal: previousOrder.subtotal,
          deliveryFee: previousOrder.deliveryFee,
          total: previousOrder.total,
        }
      : summary;

  let currentStep = 1;
  let submitting = false;
  renderSteps(currentStep);
  renderCheckoutSummary(orderSummary);
  byId("clienteCorreo").value = customer?.email ?? "";

  const profileFields = {
    clienteNombre: customer?.name,
    clienteTelefono: customer?.phone,
    clienteDireccion: customer?.address,
  };
  Object.entries(profileFields).forEach(([id, value]) => {
    const field = byId(id);
    if (value && field && !field.value) field.value = value;
  });

  const dateInput = byId("fechaEntrega");
  if (dateInput) dateInput.min = localDateOffset(1);
  const deliveryMode = form.elements.modalidad;
  const addressFields = document.querySelectorAll("[data-address-fields]");
  const addressInput = byId("clienteDireccion");
  const referenceInput = byId("clienteReferencia");
  const updateAddressFields = () => {
    const isPickup = deliveryMode.value === "Recojo en tienda";
    addressFields.forEach((field) => {
      field.hidden = isPickup;
    });
    [addressInput, referenceInput].forEach((field) => {
      if (!field) return;
      field.disabled = isPickup;
      if (field === addressInput) field.required = !isPickup;
    });
  };
  deliveryMode.addEventListener("change", updateAddressFields);
  deliveryMode.addEventListener("change", () => {
    const fee =
      deliveryMode.value === "Recojo en tienda" ? 0 : summary.deliveryFee;
    updateCheckoutTotals(summary, fee);
  });
  updateAddressFields();

  const panels = new Map(
    [...form.querySelectorAll("[data-checkout-panel]")].map((panel) => [
      Number(panel.dataset.checkoutPanel),
      panel,
    ]),
  );
  const showStep = (step) => {
    currentStep = step;
    panels.forEach((panel, panelStep) => {
      panel.hidden = panelStep !== step;
      panel.classList.remove("checkout-panel-enter");
    });
    const activePanel = panels.get(step);
    if (activePanel) {
      void activePanel.offsetWidth;
      activePanel.classList.add("checkout-panel-enter");
      activePanel
        .querySelector("[tabindex='-1']")
        ?.focus({ preventScroll: true });
    }
    renderSteps(step);
    notice.classList.add("d-none");
    notice.textContent = "";
    const reduceMotion = window.matchMedia?.(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    window.scrollTo({ top: 0, behavior: reduceMotion ? "auto" : "smooth" });
  };

  if (previousOrder?.customerId === customer?.id) {
    showStep(3);
    byId("checkout-review").hidden = true;
    byId("checkout-success").hidden = false;
    byId("checkout-order-number").textContent = previousOrder.id;
    byId("titulo-pedido").textContent = "Pedido registrado";
    renderSteps(4);
    return;
  }

  form.addEventListener("input", (event) => {
    setCheckoutCustomValidity(event.target);
    if (event.target.matches(".is-invalid") && event.target.checkValidity())
      event.target.classList.remove("is-invalid");
  });
  form.addEventListener("change", (event) => {
    setCheckoutCustomValidity(event.target);
    if (event.target.matches(".is-invalid") && event.target.checkValidity())
      event.target.classList.remove("is-invalid");
  });
  form.addEventListener("click", (event) => {
    const nextButton = event.target.closest("[data-checkout-next]");
    const backButton = event.target.closest("[data-checkout-back]");
    if (backButton) {
      showStep(Number(backButton.dataset.checkoutBack));
      return;
    }
    if (!nextButton) return;
    const nextStep = Number(nextButton.dataset.checkoutNext);
    if (currentStep === 2 && !validateCheckoutPanel(panels.get(2), notice))
      return;
    if (nextStep === 3) initCheckoutReview(form);
    showStep(nextStep);
  });

  const submitButton = form.querySelector("[data-confirm-order]");
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    if (submitting || currentStep !== 3) return;
    if (!validateCheckoutPanel(panels.get(3), notice)) return;
    const deliveryModeValue = form.elements.modalidad.value;
    const delivery = {
      mode: deliveryModeValue,
      address:
        deliveryModeValue === "Recojo en tienda"
          ? ""
          : addressInput.value.trim(),
      reference:
        deliveryModeValue === "Recojo en tienda"
          ? ""
          : referenceInput.value.trim(),
      date: dateInput.value,
      time: form.elements.horaEntrega.value,
      notes: byId("mensaje").value.trim(),
      paymentMethod: form.elements.metodoPago.value,
    };
    submitting = true;
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = "Registrando pedido…";
    }
    let order;
    try {
      order = storeService.createOrder({
        customer: {
          ...customer,
          name: byId("clienteNombre").value.trim(),
          phone: byId("clienteTelefono").value.trim(),
        },
        delivery,
      });
    } catch (error) {
      submitting = false;
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = "Confirmar pedido";
      }
      setMessage(notice, error.message, "danger");
      return;
    }

    try {
      storeService.clearCart();
    } catch {
      setMessage(
        notice,
        "El pedido quedó registrado, pero no se pudo actualizar el carrito. Puedes continuar y consultar el pedido en tu cuenta.",
        "warning",
      );
    }
    const orderNumber = byId("checkout-order-number");
    if (orderNumber) orderNumber.textContent = order.id;
    byId("checkout-review").hidden = true;
    byId("checkout-success").hidden = false;
    byId("titulo-pedido").textContent = "Pedido registrado";
    history.replaceState(
      null,
      "",
      `${location.pathname}?pedido=${encodeURIComponent(order.id)}`,
    );
    renderSteps(4);
    if (submitButton) submitButton.hidden = true;
    updateNavigation(authService.current());
    if (notice.classList.contains("d-none")) {
      setMessage(
        notice,
        `Tu pedido ${order.id} quedó registrado correctamente.`,
        "success",
      );
      notice.setAttribute("role", "status");
    }
    const reduceMotion = window.matchMedia?.(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    window.scrollTo({ top: 0, behavior: reduceMotion ? "auto" : "smooth" });
  });
}

export function initConfirmation() {
  const main = byId("titulo-confirmacion");
  if (!main) return;
  if (!protectPage("cliente")) return;
  const customer = authService.customer();
  const orderId =
    new URLSearchParams(location.search).get("pedido") ||
    (() => {
      try {
        return sessionStorage.getItem("opalina-last-order");
      } catch {
        return null;
      }
    })();
  const order = storeService.getOrder(orderId);
  if (!order || order.customerId !== customer?.id) {
    location.replace(pagePath("historial-pedidos.html"));
    return;
  }
  location.replace(
    `${pagePath("pedido.html")}?pedido=${encodeURIComponent(order.id)}`,
  );
}

export function initLogin() {
  const form = byId("formulario-ingreso");
  if (!form) return;
  const notice = byId("mensaje-error");
  const returnTo = new URLSearchParams(location.search).get("returnTo");
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    try {
      const session = authService.login(
        byId("correo").value,
        byId("clave").value,
      );
      updateNavigation(session);
      const destination =
        returnTo && /^[a-z0-9-]+\.html(?:\?.*)?$/i.test(returnTo)
          ? returnTo
          : session.role === "admin"
            ? "admin.html"
            : "perfil.html";
      location.href = pagePath(destination);
    } catch (error) {
      setMessage(notice, error.message, "danger");
    }
  });
}

export function initRegistration() {
  const form = byId("formulario-registro");
  if (!form) return;
  const notice = byId("mensaje-error");
  const password = byId("clave-registro");
  const confirmation = byId("confirmar-clave");
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    confirmation.setCustomValidity(
      password.value === confirmation.value
        ? ""
        : "Las contraseñas no coinciden.",
    );
    if (!form.reportValidity()) return;
    password.value = "";
    confirmation.value = "";
    setMessage(notice, "Tus datos fueron validados correctamente.", "success");
    notice.setAttribute("role", "status");
    notice.setAttribute("aria-live", "polite");
  });
  confirmation.addEventListener("input", () =>
    confirmation.setCustomValidity(""),
  );
}

export function initProfile() {
  const form = document.querySelector("main form");
  if (!form || !protectPage("cliente")) return;
  const customer = authService.customer();
  if (!customer) return;
  const notice = createElement("div", {
    className: "alert d-none",
    attrs: { role: "status", "aria-live": "polite" },
  });
  form.before(notice);
  const splitName = customer.name.split(" ");
  const values = {
    nombre: splitName.shift() ?? "",
    apellidos: splitName.join(" "),
    correo: customer.email,
    telefono: customer.phone,
    direccion: customer.address,
    referencia: customer.reference,
  };
  Object.entries(values).forEach(([id, value]) => {
    if (byId(id)) byId(id).value = value;
  });
  const sidebarName = document.querySelector("[data-customer-name]");
  const sidebarEmail = document.querySelector("[data-customer-email]");
  if (sidebarName) sidebarName.textContent = customer.name;
  if (sidebarEmail) sidebarEmail.textContent = customer.email;
  const avatar = document.querySelector(".account-sidebar .avatar-initials");
  if (avatar)
    avatar.textContent = customer.name
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toLocaleUpperCase("es");
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    try {
      const updated = storeService.saveCustomer({
        ...customer,
        name: `${byId("nombre").value.trim()} ${byId("apellidos").value.trim()}`.trim(),
        email: byId("correo").value.trim(),
        phone: byId("telefono").value.trim(),
        address: byId("direccion").value.trim(),
        reference: byId("referencia").value.trim(),
      });
      try {
        sessionStorage.setItem("opalina-feedback", "Perfil actualizado.");
      } catch {
        // El perfil queda guardado aunque el navegador bloquee la sesión temporal.
      }
      setMessage(notice, `Se guardaron los datos de ${updated.name}.`);
      updateNavigation(authService.current());
    } catch (error) {
      setMessage(notice, error.message, "danger");
    }
  });
}

function accountOrders() {
  const host = byId("lista-pedidos");
  if (!host || !protectPage("cliente")) return;
  const customer = authService.customer();
  const filterHost = byId("filtro-estado-pedido");
  let filter = "todos";
  const render = () => {
    const orders = storeService
      .listOrders()
      .filter((order) => order.customerId === customer?.id);
    const visible = orders.filter(
      (order) => filter === "todos" || order.status === filter,
    );
    clear(host);
    if (!visible.length) {
      host.append(
        createElement("div", {
          className: "alert alert-light",
          text: "No tienes pedidos en este estado.",
          attrs: { role: "status" },
        }),
      );
      return;
    }
    visible.forEach((order) => {
      const card = createElement("article", { className: "card shadow-sm" });
      const body = createElement("div", { className: "card-body p-4" });
      const header = createElement("div", {
        className:
          "d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3",
      });
      const titleGroup = createElement("div");
      titleGroup.append(
        createElement("h2", { className: "h5 mb-1", text: order.id }),
        createElement("p", {
          className: "text-body-secondary small mb-0",
          text: formatDate(order.createdAt),
        }),
      );
      header.append(
        titleGroup,
        createElement("span", {
          className: `estado-estado ${STATUS_CLASSES[order.status]}`,
          text: STATUS_LABELS[order.status],
        }),
      );
      body.append(
        header,
        createElement("p", {
          className: "small mb-1",
          text: `Modalidad: ${order.delivery.mode}`,
        }),
        createElement("p", {
          className: "small text-body-secondary mb-3",
          text: `Dirección: ${order.delivery.address || "Recojo en tienda"}`,
        }),
      );
      const footer = createElement("div", {
        className:
          "d-flex flex-wrap justify-content-between align-items-center gap-3",
      });
      footer.append(
        createElement("strong", {
          className: "fs-5",
          text: formatMoney(order.total),
        }),
        createElement("a", {
          className: "btn btn-primary btn-sm cuenta-accion-pedido",
          text: "Ver detalle",
          attrs: {
            href: pagePath(
              `detalle-pedido.html?pedido=${encodeURIComponent(order.id)}`,
            ),
          },
        }),
      );
      body.append(footer);
      card.append(body);
      host.append(card);
    });
  };
  filterHost?.addEventListener("click", (event) => {
    const button = event.target.closest?.("[data-order-filter]");
    if (!button) return;
    filter = button.dataset.orderFilter;
    filterHost.querySelectorAll("button").forEach((item) => {
      const active = item === button;
      item.classList.toggle("active", active);
      item.setAttribute("aria-pressed", String(active));
    });
    render();
  });
  const name = document.querySelector(".account-sidebar h2");
  const email = document.querySelector(
    ".account-sidebar .text-body-secondary.small",
  );
  if (name) name.textContent = customer?.name ?? "Cliente";
  if (email) email.textContent = customer?.email ?? "";
  const avatar = document.querySelector(".account-sidebar .avatar-initials");
  if (avatar && customer)
    avatar.textContent = customer.name
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toLocaleUpperCase("es");
  render();
  window.addEventListener("storage", (event) => {
    if (event.key === null || event.key?.startsWith("opalina-")) render();
  });
}

export function initOrderHistory() {
  accountOrders();
}

export function initOrderDetail() {
  const title = byId("titulo-detalle-pedido");
  if (!title || !protectPage("cliente")) return;
  const customer = authService.customer();
  const id = new URLSearchParams(location.search).get("pedido");
  const order = storeService.getOrder(id);
  if (!order || order.customerId !== customer?.id) {
    title.textContent = "Pedido no encontrado";
    const detail = document.querySelector("main .row.g-4.align-items-start");
    if (detail) detail.hidden = true;
    return;
  }
  if (byId("ceja-orden")) byId("ceja-orden").textContent = `Pedido ${order.id}`;
  if (byId("fecha-orden"))
    byId("fecha-orden").textContent =
      `Realizado el ${formatDate(order.createdAt)}`;
  const status = byId("estado-orden");
  status.textContent = STATUS_LABELS[order.status];
  status.className = `estado-estado ${STATUS_CLASSES[order.status]}`;
  const accountName =
    document.querySelector("[data-customer-name]") ??
    document.querySelector(".account-sidebar h2");
  const accountEmail =
    document.querySelector("[data-customer-email]") ??
    document.querySelector(".account-sidebar .text-body-secondary.small");
  if (accountName) accountName.textContent = customer.name;
  if (accountEmail) accountEmail.textContent = customer.email;
  const avatar = document.querySelector(".account-sidebar .avatar-initials");
  if (avatar)
    avatar.textContent = customer.name
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toLocaleUpperCase("es");
  const timeline = document.querySelector(".pedido-timeline");
  const progress = ["pendiente", "proceso", "enviado", "entregado"];
  const cancelled = order.status === "cancelado";
  if (timeline)
    [...timeline.children].forEach((step, index) => {
      const stateIndex = progress.indexOf(order.status);
      step.className = `paso-tiempo ${cancelled ? "paso-tiempo--pendiente" : index < stateIndex ? "paso-tiempo--hecho" : index === stateIndex ? "paso-tiempo--actual" : "paso-tiempo--pendiente"}`;
      const history = order.history.find(
        (entry) => entry.status === progress[index],
      );
      const dateNode = step.querySelector("p");
      if (dateNode)
        dateNode.textContent = history ? formatDate(history.date) : "Pendiente";
    });
  const productSection = byId("titulo-productos-pedido")?.parentElement;
  if (productSection) {
    const productHost = byId("items-detalle-pedido");
    if (productHost) clear(productHost);
    order.items.forEach((item) => {
      const row = createElement("div", {
        className: "d-flex align-items-center gap-3 mb-3",
      });
      row.append(
        createElement("img", {
          className: "miniatura-carrito rounded",
          attrs: { src: item.image, alt: item.name, width: 72, height: 72 },
        }),
      );
      const details = createElement("div", { className: "flex-grow-1" });
      details.append(
        createElement("h3", { className: "h5 mb-1", text: item.name }),
        createElement("p", {
          className: "text-body-secondary small mb-0",
          text: `${item.category} · Cantidad: ${item.quantity}`,
        }),
      );
      row.append(
        details,
        createElement("strong", {
          className: "flex-shrink-0",
          text: formatMoney(item.lineTotal),
        }),
      );
      (productHost ?? productSection).append(row);
    });
  }
  const delivery = order.delivery;
  const setLabel = (idName, label, value) => {
    const node = byId(idName);
    if (!node) return;
    clear(node);
    node.append(
      createElement("strong", { text: `${label}: ` }),
      document.createTextNode(value || "—"),
    );
  };
  setLabel("modalidad-entrega", "Modalidad", delivery.mode);
  setLabel(
    "direccion-entrega",
    "Dirección",
    delivery.address || "Recojo en tienda",
  );
  setLabel(
    "notas-entrega",
    "Notas",
    delivery.notes || delivery.reference || "—",
  );
  setLabel("metodo-pago", "Método", order.payment.method);
  setLabel("estado-pago", "Estado", order.payment.status);
  const summary = document.querySelector(
    "#titulo-resumen-pedido",
  )?.parentElement;
  const spans = summary?.querySelectorAll(
    ".d-flex.justify-content-between span:last-child",
  );
  if (spans?.[0]) spans[0].textContent = formatMoney(order.subtotal);
  if (spans?.[1])
    spans[1].textContent = order.deliveryFee
      ? formatMoney(order.deliveryFee)
      : "Gratis";
  if (spans?.[2]) spans[2].textContent = formatMoney(order.total);
}

export function initAuthGuards() {
  const file = location.pathname.split("/").pop();
  if (file?.startsWith("admin")) return protectPage("admin");
  if (
    ["perfil.html", "historial-pedidos.html", "detalle-pedido.html"].includes(
      file,
    )
  )
    return protectPage("cliente");
  return true;
}
