import { authService } from "../services/auth-service.js";
import { demoRepository, STORAGE_KEY } from "../repositories/demo-repository.js";
import { storeService } from "../services/store-service.js";
import { clear, createElement, formatDate, formatMoney, pagePath, setMessage } from "../ui/dom.js";

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const labels = {
  pendiente: "Pendiente",
  proceso: "En proceso",
  enviado: "Enviado",
  entregado: "Entregado",
  cancelado: "Cancelado",
};
const statusClass = {
  pendiente: "",
  proceso: "estado-proceso",
  enviado: "estado-enviado",
  entregado: "estado-entregado",
  cancelado: "estado-cancelado",
};
const norm = (value) =>
  String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
const money = formatMoney;

let currentOrderFilter = "todos";
let modalTrigger;

function showAdminMessage(message, type = "success") {
  const node = $("#admin-feedback");
  if (node) setMessage(node, message, type === "danger" ? "danger" : "success");
}

function stat(values) {
  $$("[data-stat]").forEach((node, index) => {
    node.textContent = values[index] ?? "—";
  });
}

function statusBadge(status) {
  return createElement("span", {
    className: `admin-status admin-status--${status}`,
    text: labels[status] ?? status,
  });
}

function rowCell(text, className = "") {
  return createElement("td", { className, text });
}

function emptyRow(columns, title, hint) {
  const row = createElement("tr");
  const cell = createElement("td", { attrs: { colspan: columns } });
  const box = createElement("div", { className: "admin-empty" });
  box.append(
    createElement("strong", { text: title }),
    createElement("p", { text: hint }),
  );
  cell.append(box);
  row.append(cell);
  return row;
}

function orderProductsSummary(order) {
  return order.items.map((item) => `${item.name} ×${item.quantity}`).join(", ");
}

function renderOrders() {
  const body = $("#filas-pedidos");
  if (!body) return;
  const orders = storeService.listOrders();
  const query = norm($("#buscar-pedido")?.value);
  const visible = orders.filter(
    (order) =>
      (currentOrderFilter === "todos" || order.status === currentOrderFilter) &&
      norm(
        `${order.id} ${order.customerName} ${orderProductsSummary(order)}`,
      ).includes(query),
  );
  stat([
    orders.length,
    orders.filter((order) => order.status === "pendiente").length,
    orders.filter((order) => order.status === "entregado").length,
    money(
      orders
        .filter((order) => order.status !== "cancelado")
        .reduce((sum, order) => sum + order.total, 0),
    ),
  ]);
  $$("[data-filtro-pedido]").forEach((button) => {
    const active = button.dataset.filtroPedido === currentOrderFilter;
    button.classList.toggle("active", active);
    button.setAttribute("aria-pressed", String(active));
    const count = button.querySelector("[data-count]");
    if (count)
      count.textContent = String(
        currentOrderFilter === "todos"
          ? orders.length
          : orders.filter(
              (order) => order.status === button.dataset.filtroPedido,
            ).length,
      );
  });
  const resultCount = $("#resultado-cantidad");
  if (resultCount)
    resultCount.textContent = `${visible.length} de ${orders.length} pedidos`;
  clear(body);
  if (!visible.length) {
    body.append(
      emptyRow(
        6,
        "No se encontraron pedidos",
        "Prueba otro nombre, número de pedido o estado.",
      ),
    );
    return;
  }
  visible.forEach((order) => {
    const row = createElement("tr");
    const orderCell = createElement("td");
    orderCell.append(
      createElement("a", {
        className: "admin-row-title",
        text: order.id,
        attrs: {
          href: pagePath(
            `admin-pedido-detalle.html?orden=${encodeURIComponent(order.id)}`,
          ),
        },
      }),
      createElement("small", {
        className: "admin-cell-subtitle",
        text: formatDate(order.delivery.date),
      }),
    );
    const customerCell = createElement("td");
    customerCell.append(
      createElement("strong", { text: order.customerName }),
      createElement("small", {
        className: "admin-cell-subtitle",
        text: order.phone,
      }),
    );
    const productsCell = createElement("td", {
      text: orderProductsSummary(order),
    });
    const stateCell = createElement("td");
    stateCell.append(statusBadge(order.status));
    const actionCell = createElement("td");
    const actions = createElement("div", { className: "admin-row-actions" });
    actions.append(
      createElement("a", {
        className: "btn btn-sm btn-outline-primary",
        text: "Ver",
        attrs: {
          href: pagePath(
            `admin-pedido-detalle.html?orden=${encodeURIComponent(order.id)}`,
          ),
        },
      }),
    );
    actions.append(
      createElement("button", {
        className: "btn btn-sm btn-outline-primary",
        text: "Cambiar estado",
        attrs: { type: "button", "data-estado-orden": order.id },
      }),
    );
    actionCell.append(actions);
    row.append(
      orderCell,
      customerCell,
      productsCell,
      rowCell(money(order.total), "text-nowrap fw-semibold"),
      stateCell,
      actionCell,
    );
    body.append(row);
  });
}

function renderProducts() {
  const body = $("#filas-productos");
  if (!body) return;
  const products = storeService.listProducts({ includeUnavailable: true });
  const query = norm($("#filtro-busqueda-producto")?.value);
  const category = $("#filtro-categoria-producto")?.value ?? "";
  const availability = $("#filtro-estado-producto")?.value ?? "";
  const stateOf = (product) =>
    !product.available
      ? "No disponible"
      : product.stock === 0
        ? "Sin stock"
        : "Disponible";
  const visible = products.filter(
    (product) =>
      norm(`${product.name} ${product.sku}`).includes(query) &&
      (!category || product.category === category) &&
      (!availability || stateOf(product) === availability),
  );
  stat([
    products.length,
    products.filter((item) => item.available && item.stock > 0).length,
    products.filter((item) => item.stock === 0).length,
    products.filter((item) => item.featured).length,
  ]);
  const resultCount = $("#resultado-cantidad");
  if (resultCount)
    resultCount.textContent = `${visible.length} de ${products.length} productos`;
  clear(body);
  if (!visible.length) {
    body.append(
      emptyRow(
        6,
        "No se encontraron productos",
        "Ajusta los filtros o registra un producto.",
      ),
    );
    return;
  }
  visible.forEach((product) => {
    const row = createElement("tr");
    const infoCell = createElement("td");
    const productInfo = createElement("div", {
      className: "admin-product-cell",
    });
    productInfo.append(
      createElement("img", {
        attrs: { src: product.image, alt: "", width: 48, height: 48 },
      }),
    );
    const details = createElement("div");
    details.append(
      createElement("a", {
        className: "admin-row-title",
        text: product.name,
        attrs: {
          href: pagePath(
            `admin-producto-detalle.html?producto=${encodeURIComponent(product.id)}`,
          ),
        },
      }),
      createElement("small", {
        className: "admin-cell-subtitle",
        text: product.sku,
      }),
    );
    productInfo.append(details);
    infoCell.append(productInfo);
    const stateCell = createElement("td");
    stateCell.append(
      createElement("span", {
        className: "admin-status",
        text: stateOf(product),
      }),
    );
    const actionCell = createElement("td");
    actionCell.append(
      createElement("button", {
        className: "btn btn-sm btn-outline-primary",
        text: "Editar",
        attrs: { type: "button", "data-editar-producto": product.id },
      }),
    );
    row.append(
      infoCell,
      rowCell(product.category),
      rowCell(money(product.price), "text-nowrap fw-semibold"),
      rowCell(product.stock),
      stateCell,
      actionCell,
    );
    body.append(row);
  });
}

function createModal(title, bodyContent, onSubmit) {
  const backdrop = createElement("div", {
    className: "modal fade",
    attrs: {
      tabindex: "-1",
      "aria-labelledby": "modal-title",
      "aria-hidden": "true",
    },
  });
  const dialog = createElement("div", {
    className:
      "modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable",
  });
  const form = createElement("form", { className: "modal-content" });
  const header = createElement("div", { className: "modal-header" });
  header.append(
    createElement("h2", { className: "modal-title fs-5", text: title }),
    createElement("button", {
      className: "btn-close",
      attrs: {
        type: "button",
        "data-bs-dismiss": "modal",
        "aria-label": "Cerrar",
      },
    }),
  );
  const body = createElement("div", { className: "modal-body" });
  body.append(bodyContent);
  const footer = createElement("div", { className: "modal-footer" });
  footer.append(
    createElement("button", {
      className: "btn btn-outline-primary",
      text: "Cancelar",
      attrs: { type: "button", "data-bs-dismiss": "modal" },
    }),
    createElement("button", {
      className: "btn btn-primary",
      text: "Guardar",
      attrs: { type: "submit" },
    }),
  );
  form.append(header, body, footer);
  dialog.append(form);
  backdrop.append(dialog);
  document.body.append(backdrop);
  const instance = new bootstrap.Modal(backdrop);
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    onSubmit(event, form);
  });
  backdrop.addEventListener("hidden.bs.modal", () => {
    backdrop.remove();
    modalTrigger?.focus();
  });
  backdrop.addEventListener("shown.bs.modal", () =>
    form.querySelector("input, select, textarea, button")?.focus(),
  );
  return { element: backdrop, instance, form };
}

function field(labelText, id, type = "text", value = "", required = true) {
  const wrap = createElement("div", { className: "mb-3" });
  const label = createElement("label", {
    className: "form-label",
    text: labelText,
    attrs: { for: id },
  });
  const input = createElement(type === "textarea" ? "textarea" : "input", {
    className: "form-control",
    attrs: {
      id,
      name: id,
      type: type === "textarea" ? null : type,
      value: type === "textarea" ? null : value,
      required,
      maxlength: type === "textarea" ? 1200 : 150,
      rows: type === "textarea" ? 3 : null,
      min: type === "number" ? 0 : null,
      step: type === "number" ? "0.01" : null,
    },
  });
  if (type === "textarea") input.value = value;
  wrap.append(label, input);
  return wrap;
}

function openProductModal(product, trigger) {
  modalTrigger = trigger;
  const body = createElement("div", { className: "row g-3" });
  const name = field(
    "Nombre del producto",
    "edit-name",
    "text",
    product?.name ?? "",
  );
  name.classList.add("col-md-8");
  const sku = field(
    "Código / SKU",
    "edit-sku",
    "text",
    product?.sku ?? `OPA-${Date.now().toString().slice(-6)}`,
  );
  sku.classList.add("col-md-4");
  const description = field(
    "Descripción",
    "edit-description",
    "textarea",
    product?.description ?? "",
  );
  description.classList.add("col-12");
  const categoryWrap = createElement("div", { className: "col-md-6" });
  categoryWrap.append(
    createElement("label", {
      className: "form-label",
      text: "Categoría",
      attrs: { for: "edit-category" },
    }),
  );
  const category = createElement("select", {
    className: "form-select",
    attrs: { id: "edit-category", required: true },
  });
  [
    "Romántico",
    "Cumpleaños",
    "Temporada",
    "Premium",
    "Condolencias",
    "Personalizado",
  ].forEach((item) =>
    category.append(
      createElement("option", {
        text: item,
        attrs: { value: item, selected: item === product?.category },
      }),
    ),
  );
  categoryWrap.append(category);
  const price = field(
    "Precio (S/)",
    "edit-price",
    "number",
    product?.price ?? 0,
  );
  price.classList.add("col-6", "col-md-3");
  const stock = field("Stock", "edit-stock", "number", product?.stock ?? 0);
  stock.classList.add("col-6", "col-md-3");
  const imageWrap = createElement("div", { className: "col-12" });
  imageWrap.append(
    createElement("label", {
      className: "form-label",
      text: "Imagen (URL del catálogo)",
      attrs: { for: "edit-image" },
    }),
  );
  const image = createElement("input", {
    className: "form-control",
    attrs: {
      id: "edit-image",
      type: "text",
      value: product?.image ?? "",
      placeholder: "https://… o /assets/…",
      maxlength: 500,
    },
  });
  imageWrap.append(image);
  const availableWrap = createElement("div", {
    className: "col-12 form-check ms-2",
  });
  const available = createElement("input", {
    className: "form-check-input",
    attrs: {
      id: "edit-available",
      type: "checkbox",
      checked: product?.available ?? true,
    },
  });
  availableWrap.append(
    available,
    createElement("label", {
      className: "form-check-label",
      text: "Disponible para venta",
      attrs: { for: "edit-available" },
    }),
  );
  const featuredWrap = createElement("div", {
    className: "col-12 form-check ms-2",
  });
  const featured = createElement("input", {
    className: "form-check-input",
    attrs: {
      id: "edit-featured",
      type: "checkbox",
      checked: product?.featured ?? false,
    },
  });
  featuredWrap.append(
    featured,
    createElement("label", {
      className: "form-check-label",
      text: "Producto destacado",
      attrs: { for: "edit-featured" },
    }),
  );
  body.append(
    name,
    sku,
    description,
    categoryWrap,
    price,
    stock,
    imageWrap,
    availableWrap,
    featuredWrap,
  );
  const dialog = createModal(
    product ? "Editar producto" : "Nuevo producto",
    body,
    (event, form) => {
      if (!form.reportValidity()) return;
      try {
        storeService.saveProduct({
          id: product?.id ?? `producto-${crypto.randomUUID()}`,
          name: $("#edit-name", form).value.trim(),
          sku: $("#edit-sku", form).value.trim(),
          description: $("#edit-description", form).value.trim(),
          category: $("#edit-category", form).value,
          price: Number($("#edit-price", form).value),
          stock: Number($("#edit-stock", form).value),
          image:
            $("#edit-image", form).value.trim() ||
            "/assets/imagenes/Opalina-Logo.png",
          available: $("#edit-available", form).checked,
          featured: $("#edit-featured", form).checked,
        });
        renderProducts();
        detailProduct();
        dialog.instance.hide();
        showAdminMessage(
          product ? "Producto actualizado." : "Producto creado.",
        );
      } catch (error) {
        showAdminMessage(error.message, "danger");
      }
    },
  );
  dialog.instance.show();
}

function openOrderStatus(order, trigger) {
  modalTrigger = trigger;
  const fieldset = createElement("fieldset");
  fieldset.append(
    createElement("legend", {
      className: "fs-6",
      text: `Nuevo estado para ${order.id}`,
    }),
  );
  Object.entries(labels).forEach(([value, labelText]) => {
    const choice = createElement("div", { className: "form-check mb-2" });
    const radio = createElement("input", {
      className: "form-check-input",
      attrs: {
        type: "radio",
        name: "next-status",
        id: `status-${value}`,
        value,
        checked: order.status === value,
        required: true,
      },
    });
    choice.append(
      radio,
      createElement("label", {
        className: "form-check-label",
        text: labelText,
        attrs: { for: `status-${value}` },
      }),
    );
    fieldset.append(choice);
  });
  const dialog = createModal(
    "Cambiar estado del pedido",
    fieldset,
    (event, form) => {
      const next = new FormData(form).get("next-status");
      try {
        storeService.changeOrderStatus(order.id, next);
        renderOrders();
        renderReports();
        detailOrder();
        dialog.instance.hide();
        showAdminMessage("Estado del pedido actualizado.");
      } catch (error) {
        showAdminMessage(error.message, "danger");
      }
    },
  );
  dialog.instance.show();
}

function renderClients() {
  const body = $("#filas-clientes");
  if (!body) return;
  const all = storeService.listCustomers();
  const query = norm($("#buscar-cliente")?.value);
  const sort = $("#orden-clientes")?.value ?? "nombre";
  const visible = all
    .filter((client) =>
      norm(`${client.name} ${client.email} ${client.phone}`).includes(query),
    )
    .sort((a, b) =>
      sort === "pedidos"
        ? b.orderCount - a.orderCount
        : sort === "total"
          ? b.spent - a.spent
          : a.name.localeCompare(b.name, "es"),
    );
  stat([
    all.length,
    all.reduce((sum, client) => sum + client.orderCount, 0),
    money(all.reduce((sum, client) => sum + client.spent, 0)),
    money(
      all.length
        ? all.reduce((sum, client) => sum + client.spent, 0) / all.length
        : 0,
    ),
  ]);
  const result = $("#resultado-cantidad");
  if (result)
    result.textContent = `${visible.length} de ${all.length} clientes`;
  clear(body);
  if (!visible.length) {
    body.append(
      emptyRow(
        6,
        "No se encontraron clientes",
        "Prueba otro nombre, correo o teléfono.",
      ),
    );
    return;
  }
  visible.forEach((client) => {
    const row = createElement("tr");
    row.append(
      rowCell(client.name),
      rowCell(client.email || "No registrado"),
      rowCell(client.phone),
      rowCell(client.orderCount),
      rowCell(money(client.spent)),
    );
    const action = createElement("td");
    action.append(
      createElement("button", {
        className: "btn btn-outline-primary btn-sm",
        text: "Ver / editar",
        attrs: { type: "button", "data-client-id": client.id },
      }),
    );
    row.append(action);
    body.append(row);
  });
}

function openClient(client, trigger) {
  modalTrigger = trigger;
  const body = createElement("div");
  body.append(
    field("Nombre", "client-name", "text", client.name),
    field("Correo", "client-email", "email", client.email, false),
    field("Teléfono", "client-phone", "tel", client.phone),
  );
  const orders = storeService
    .listOrders()
    .filter((order) => order.customerId === client.id);
  const orderList = createElement("div", { className: "list-group mt-3" });
  orders.forEach((order) =>
    orderList.append(
      createElement("a", {
        className:
          "list-group-item list-group-item-action d-flex justify-content-between gap-3",
        text: `${order.id} · ${money(order.total)} · ${labels[order.status]}`,
        attrs: {
          href: pagePath(
            `admin-pedido-detalle.html?orden=${encodeURIComponent(order.id)}`,
          ),
        },
      }),
    ),
  );
  body.append(
    createElement("h3", { className: "h6 mt-4", text: "Pedidos registrados" }),
    orderList,
  );
  const dialog = createModal("Información del cliente", body, (event, form) => {
    if (!form.reportValidity()) return;
    try {
      storeService.saveCustomer({
        ...client,
        name: $("#client-name", form).value.trim(),
        email: $("#client-email", form).value.trim(),
        phone: $("#client-phone", form).value.trim(),
      });
      renderClients();
      dialog.instance.hide();
      showAdminMessage(
        "Contacto actualizado. Los pedidos mantienen su información histórica.",
      );
    } catch (error) {
      showAdminMessage(error.message, "danger");
    }
  });
  dialog.instance.show();
}

function renderReports() {
  const body = $("#filas-reporte");
  if (!body) return;
  const status = $("#estado-reporte")?.value ?? "todos";
  const orders = storeService
    .listOrders()
    .filter((order) => status === "todos" || order.status === status);
  const grouped = new Map();
  orders.forEach((order) =>
    order.items.forEach((item) => {
      const aggregate = grouped.get(item.productId) ?? {
        id: item.productId,
        name: item.name,
        units: 0,
        total: 0,
      };
      aggregate.units += item.quantity;
      aggregate.total += item.lineTotal;
      grouped.set(item.productId, aggregate);
    }),
  );
  const rows = [...grouped.values()].sort((a, b) => b.units - a.units);
  const revenue = orders
    .filter((order) => order.status !== "cancelado")
    .reduce((sum, order) => sum + order.total, 0);
  stat([
    orders.length,
    money(revenue),
    money(orders.length ? revenue / orders.length : 0),
    rows.reduce((sum, item) => sum + item.units, 0),
  ]);
  const result = $("#resultado-cantidad");
  if (result) result.textContent = `${rows.length} productos`;
  clear(body);
  if (!rows.length) {
    body.append(
      emptyRow(
        3,
        "No hay pedidos con este estado",
        "Selecciona otro estado para consultar el resumen.",
      ),
    );
    return;
  }
  rows.forEach((item) => {
    const row = createElement("tr");
    row.append(
      rowCell(item.name),
      rowCell(item.units),
      rowCell(money(item.total)),
    );
    body.append(row);
  });
}

function initSettings() {
  const settings = storeService.getSettings();
  $$("[data-config-form]").forEach((form) => {
    Object.entries(settings).forEach(([key, value]) => {
      const input = $(`#${key}`, form);
      if (input) input.value = value;
    });
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      if (!form.reportValidity()) return;
      const values = Object.fromEntries(
        $$("input", form).map((input) => [input.id, input.value.trim()]),
      );
      try {
        storeService.saveSettings(values);
        showAdminMessage("Configuración guardada correctamente.");
      } catch (error) {
        showAdminMessage(error.message, "danger");
      }
    });
  });
}

function detailOrder() {
  const title = $("#titulo-detalle-pedido");
  if (!title) return;
  const params = new URLSearchParams(location.search);
  const id = params.get("orden") ?? params.get("pedido");
  const order = storeService.getOrder(id);
  if (!order) {
    title.textContent = "Pedido no encontrado";
    const content = $("#pedido-contenido");
    if (content) content.hidden = true;
    return;
  }
  const orderNumber = $("#detalle-numero-orden");
  if (orderNumber) orderNumber.textContent = order.id;
  const customer = storeService.getCustomer(order.customerId);
  const statusHost = $("#estado-actual");
  if (statusHost) {
    clear(statusHost);
    statusHost.append(statusBadge(order.status));
  }
  const statusTitle = $("#titulo-estado");
  if (statusTitle) statusTitle.firstChild.textContent = "Estado actual ";
  const description = $("#estado-descripcion");
  if (description)
    description.textContent = `El pedido está ${labels[order.status].toLowerCase()}.`;

  const products = $("#detalle-productos-pedido");
  if (products) {
    clear(products);
    order.items.forEach((product) => {
      const row = createElement("div", {
        className: "d-flex align-items-center gap-3 py-3 border-bottom",
      });
      row.append(
        createElement("img", {
          className: "rounded",
          attrs: {
            src: product.image,
            alt: product.name,
            width: 72,
            height: 72,
          },
        }),
      );
      const info = createElement("div", { className: "flex-grow-1" });
      info.append(
        createElement("strong", { text: product.name }),
        createElement("small", {
          className: "d-block text-body-secondary",
          text: `${product.category} · Cantidad: ${product.quantity}`,
        }),
      );
      row.append(
        info,
        createElement("strong", { text: money(product.lineTotal) }),
      );
      products.append(row);
    });
  }

  const changeButton = $("[data-abrir-estado]");
  if (changeButton) changeButton.dataset.abrirEstado = order.id;
  const values = {
    cliente: order.customerName,
    email: order.email,
    telefono: order.phone,
    desde: customer?.createdAt
      ? formatDate(customer.createdAt, { month: "long", year: "numeric" })
      : "—",
    modalidad: order.delivery.mode,
    direccion: order.delivery.address || "Recojo en tienda",
    referencia: order.delivery.reference || "—",
    fecha: `${formatDate(order.delivery.date)} · ${order.delivery.time}`,
    notas: order.delivery.notes || "—",
    subtotal: money(order.subtotal),
    delivery: order.deliveryFee ? money(order.deliveryFee) : "Gratis",
    total: money(order.total),
    metodo: order.payment.method,
    pago: order.payment.status,
    pagado: order.payment.status === "Pagado" ? money(order.total) : money(0),
    saldo: order.payment.status === "Pagado" ? money(0) : money(order.total),
  };
  $$("[data-orden]").forEach((node) => {
    node.textContent = values[node.dataset.orden] ?? "—";
  });

  const whatsapp = $("[data-orden-whatsapp]");
  if (whatsapp) {
    const phone = order.phone.replace(/\D/g, "");
    whatsapp.href = `https://wa.me/${phone}?text=${encodeURIComponent(`Hola ${order.customerName}, te contactamos por el pedido ${order.id}.`)}`;
  }

  const progress = $("#progreso-pedido");
  if (progress) {
    clear(progress);
    const statuses = ["pendiente", "proceso", "enviado", "entregado"];
    const activeIndex = statuses.indexOf(order.status);
    statuses.forEach((status, index) => {
      const className =
        order.status !== "cancelado" && index < activeIndex
          ? "complete"
          : order.status !== "cancelado" && index === activeIndex
            ? "current"
            : "";
      const step = createElement("li", { className });
      const historyEntry = order.history.find(
        (entry) => entry.status === status,
      );
      step.append(
        createElement("span", { text: labels[status] }),
        createElement("small", {
          text: historyEntry ? formatDate(historyEntry.date) : "Pendiente",
        }),
      );
      progress.append(step);
    });
    if (order.status === "cancelado")
      progress.append(
        createElement("li", { className: "current", text: "Pedido cancelado" }),
      );
  }

  const history = $("#historial-pedido");
  if (history) {
    clear(history);
    order.history.forEach((entry) => {
      const item = createElement("li");
      item.append(
        createElement("strong", { text: labels[entry.status] }),
        createElement("small", {
          text: formatDate(entry.date, {
            dateStyle: "medium",
            timeStyle: "short",
          }),
        }),
      );
      history.append(item);
    });
  }
}

function detailProduct() {
  const host = $("#producto-contenido");
  if (!host) return;
  const id = new URLSearchParams(location.search).get("producto");
  const product = storeService.getProduct(id);
  if (!product) {
    clear(host);
    host.append(
      createElement("div", {
        className: "alert alert-warning",
        text: "No se encontró el producto.",
        attrs: { role: "status" },
      }),
      createElement("a", {
        className: "btn btn-outline-primary",
        text: "Volver a productos",
        attrs: { href: pagePath("admin-productos.html") },
      }),
    );
    return;
  }
  $$("[data-producto]").forEach((node) => {
    const key = node.dataset.producto;
    if (key === "imagen") {
      node.src = product.image;
      node.alt = product.name;
    } else if (key === "nombre") node.textContent = product.name;
    else if (key === "descripcion") node.textContent = product.description;
    else if (key === "categoria") node.textContent = product.category;
    else if (key === "precio") node.textContent = money(product.price);
    else if (key === "estado")
      node.textContent = !product.available
        ? "No disponible"
        : product.stock
          ? "Disponible"
          : "Sin stock";
    else if (key === "destacado")
      node.textContent = product.featured ? "Sí" : "No";
    else node.textContent = product[key] ?? "—";
  });
  const editButton = $("[data-editar-producto]");
  if (editButton) editButton.dataset.editarProducto = product.id;
}

function bindEvents() {
  document.addEventListener("click", (event) => {
    const target = event.target.closest("button");
    if (!target) return;
    if (target.hasAttribute("data-filtro-pedido")) {
      currentOrderFilter = target.dataset.filtroPedido;
      renderOrders();
    }
    if (target.hasAttribute("data-estado-orden")) {
      const order = storeService.getOrder(target.dataset.estadoOrden);
      if (order) openOrderStatus(order, target);
    }
    if (target.hasAttribute("data-abrir-estado")) {
      const order = storeService.getOrder(target.dataset.abrirEstado);
      if (order) openOrderStatus(order, target);
    }
    if (target.hasAttribute("data-editar-producto")) {
      const product = storeService.getProduct(target.dataset.editarProducto);
      if (product) openProductModal(product, target);
    }
    if (target.hasAttribute("data-nuevo-producto"))
      openProductModal(null, target);
    if (target.hasAttribute("data-client-id")) {
      const client = storeService.getCustomer(target.dataset.clientId);
      if (client) openClient(client, target);
    }
    if (target.hasAttribute("data-imprimir-pedido")) window.print();
    if (target.id === "limpiar-productos") {
      [
        "#filtro-busqueda-producto",
        "#filtro-categoria-producto",
        "#filtro-estado-producto",
      ].forEach((selector) => {
        const input = $(selector);
        if (input) input.value = "";
      });
      renderProducts();
    }
  });
  $("#buscar-pedido")?.addEventListener("input", renderOrders);
  $("#filtro-busqueda-producto")?.addEventListener("input", renderProducts);
  $("#filtro-categoria-producto")?.addEventListener("change", renderProducts);
  $("#filtro-estado-producto")?.addEventListener("change", renderProducts);
  $("#buscar-cliente")?.addEventListener("input", renderClients);
  $("#orden-clientes")?.addEventListener("change", renderClients);
  $("#estado-reporte")?.addEventListener("change", renderReports);
  window.addEventListener("storage", (event) => {
    if (event.key !== STORAGE_KEY && event.key !== null) return;
    renderOrders();
    renderProducts();
    renderClients();
    renderReports();
    detailOrder();
    detailProduct();
  });
}

export function initAdmin() {
  if (
    !$("#contenido-admin") &&
    !$("#producto-contenido") &&
    !$("#detalle-productos-pedido")
  )
    return;
  if (!authService.canAccess("admin")) return;
  const adminNavigation = $(".admin-app .offcanvas-body nav");
  if (adminNavigation && !adminNavigation.querySelector("[data-logout]")) {
    adminNavigation.append(
      createElement("a", {
        className: "list-group-item list-group-item-action text-danger",
        text: "Cerrar sesión",
        attrs: { href: pagePath("login.html"), "data-logout": true },
      }),
    );
  }
  const name = $(".admin-brand strong");
  if (name) name.textContent = "Administración";
  initSettings();
  bindEvents();
  renderOrders();
  renderProducts();
  renderClients();
  renderReports();
  detailOrder();
  detailProduct();
}
