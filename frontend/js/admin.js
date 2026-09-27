/* Administración del prototipo: persistencia local y precios históricos separados. */
(() => {
  "use strict";
  const $ = (s, root = document) => root.querySelector(s);
  const $$ = (s, root = document) => [...root.querySelectorAll(s)];
  const esc = (s) =>
    String(s ?? "").replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[c],
    );
  const normalize = (s) =>
    String(s ?? "")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase()
      .trim();
  const amount = (s) => Number(String(s).replace(/[^\d.-]/g, "")) || 0;
  const money = (n) =>
    "S/ " +
    Number(n).toLocaleString("es-PE", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  const seed = window.OpalinaAdminDatos;
  if (!seed) return;
  const PRODUCT_KEY = "opalina-admin-productos-v2";
  const ORDER_KEY = "opalina-admin-pedidos-v2";
  const CLIENT_KEY = "opalina-admin-clientes-v1";
  const CONFIG_KEY = "opalina-admin-config-v1";
  const states = {
    pendiente: {
      label: "Pendiente",
      description: "El pedido está pendiente de revisión.",
    },
    proceso: {
      label: "En proceso",
      description: "El equipo está preparando los productos del pedido.",
    },
    enviado: {
      label: "Enviado",
      description: "El pedido salió para su entrega.",
    },
    entregado: {
      label: "Entregado",
      description: "El cliente recibió el pedido.",
    },
    cancelado: {
      label: "Cancelado",
      description: "El pedido no continuará con su preparación o entrega.",
    },
  };
  let timer;
  function notify(message, error = false) {
    const box = $("#admin-feedback");
    if (!box) return;
    box.textContent = message;
    box.hidden = false;
    box.classList.toggle("error", error);
    clearTimeout(timer);
    timer = setTimeout(() => {
      box.hidden = true;
    }, 7000);
  }
  function read(key) {
    try {
      const result = JSON.parse(localStorage.getItem(key) || "{}");
      return result && typeof result === "object" && !Array.isArray(result)
        ? result
        : {};
    } catch {
      notify(
        "No se pudieron leer los cambios locales. Se muestran los datos de demostración.",
        true,
      );
      return {};
    }
  }
  function save(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
      return true;
    } catch {
      notify(
        "No se pudo guardar. Revisa el espacio y los permisos de almacenamiento del navegador.",
        true,
      );
      return false;
    }
  }
  const imagePath = (id) =>
    `../assets/imagenes/productos/${id}.${/^[1-4]-/.test(id) ? "jpg" : "png"}`;
  const safeImage = (value) =>
    typeof value === "string" &&
    (/^data:image\/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$/.test(value) ||
      value === "../assets/imagenes/Opalina-Logo.png" ||
      /^\.\.\/assets\/imagenes\/productos\/[\p{L}\p{N}_-]+\.(?:png|jpe?g|webp)$/iu.test(
        value,
      ));
  function isValidProductChange(product) {
    return (
      product &&
      typeof product.nombre === "string" &&
      product.nombre.trim().length > 0 &&
      typeof product.sku === "string" &&
      product.sku.trim().length > 0 &&
      typeof product.descripcion === "string" &&
      typeof product.categoria === "string" &&
      Number.isFinite(product.precio) &&
      product.precio >= 0 &&
      Number.isInteger(product.stock) &&
      product.stock >= 0 &&
      typeof product.disponible === "boolean" &&
      typeof product.destacado === "boolean" &&
      safeImage(product.imagen)
    );
  }

  let productChanges = read(PRODUCT_KEY);
  let orderChanges = read(ORDER_KEY);
  let clientChanges = read(CLIENT_KEY);
  let products;
  function loadProducts() {
    products = new Map(
      Object.entries(seed.productos).map(([id, product]) => [
        id,
        {
          ...product,
          id,
          imagen: imagePath(id),
        },
      ]),
    );
    Object.entries(productChanges).forEach(([id, product]) => {
      if (!isValidProductChange(product)) return;

      products.set(id, {
        id,
        nombre: product.nombre.trim(),
        sku: product.sku.trim(),
        descripcion: product.descripcion.trim(),
        categoria: product.categoria,
        precio: product.precio,
        stock: product.stock,
        disponible: product.disponible,
        destacado: product.destacado,
        imagen: product.imagen,
      });
    });
  }
  loadProducts();
  function orderState(id) {
    if (states[orderChanges[id]?.estado]) return orderChanges[id].estado;
    try {
      const old = localStorage.getItem("opalina-pedido-" + id);
      if (states[old]) return old;
    } catch {}
    return seed.pedidos[id].estado;
  }
  const badge = (state) =>
    `<span class="admin-status admin-status--${esc(state)}">${esc(states[state]?.label || state)}</span>`;
  const productState = (p) =>
    !p.disponible
      ? "No disponible"
      : p.stock === 0
        ? "Sin stock"
        : "Disponible";
  const productBadge = (p) =>
    `<span class="admin-status admin-status--${!p.disponible ? "oculto" : p.stock === 0 ? "sin-stock" : "disponible"}">${productState(p)}</span>`;
  function stats(values) {
    $$("[data-stat]").forEach((e, i) => {
      e.textContent = values[i];
    });
  }
  let filter = "todos";
  function renderOrders() {
    const tbody = $("#filas-pedidos");
    if (!tbody) return;
    const all = Object.entries(seed.pedidos),
      query = normalize($("#buscar-pedido")?.value);
    const visible = all.filter(
      ([id, o]) =>
        (filter === "todos" || orderState(id) === filter) &&
        normalize(
          [
            id,
            o.cliente,
            o.productos.map((product) => product.nombre).join(" "),
          ].join(" "),
        ).includes(query),
    );
    stats([
      all.length,
      all.filter(([id]) => orderState(id) === "pendiente").length,
      all.filter(([id]) => orderState(id) === "entregado").length,
      money(
        all
          .filter(([id]) => orderState(id) !== "cancelado")
          .reduce((n, [, o]) => n + amount(o.total), 0),
      ),
    ]);
    $$("[data-filtro-pedido]").forEach((b) => {
      const state = b.dataset.filtroPedido;
      b.classList.toggle("active", filter === state);
      b.setAttribute("aria-pressed", String(filter === state));
      const count = $("[data-count]", b);
      if (count) {
        count.textContent =
          state === "todos"
            ? all.length
            : all.filter(([id]) => orderState(id) === state).length;
      }
    });
    $("#resultado-cantidad").textContent =
      `${visible.length} de ${all.length} pedidos`;
    tbody.innerHTML = visible.length
      ? visible
          .map(
            ([id, o]) => `<tr>
      <td><a class="admin-row-title" href="admin-pedido-detalle.html?orden=${encodeURIComponent(id)}">${esc(id)}</a><small class="admin-cell-subtitle">${esc(o.fecha === "—" ? "Sin fecha de entrega" : o.fecha)}</small></td>
      <td><strong>${esc(o.cliente)}</strong><small class="admin-cell-subtitle">${esc(o.telefono)}</small></td>
      <td><div class="admin-product-summary">${o.productos.map((p) => `<span>${esc(p.nombre)} <small>×${p.cantidad}</small></span>`).join("")}</div></td>
      <td class="text-nowrap fw-semibold">${money(o.total)}</td><td>${badge(orderState(id))}</td>
      <td><div class="admin-row-actions"><a class="btn btn-sm btn-outline-primary" href="admin-pedido-detalle.html?orden=${encodeURIComponent(id)}" aria-label="Ver pedido ${esc(id)}">Ver</a><button type="button" class="btn btn-sm btn-outline-primary" data-estado-orden="${esc(id)}" aria-label="Cambiar estado del pedido ${esc(id)}">Cambiar estado</button></div></td></tr>`,
          )
          .join("")
      : '<tr><td colspan="6"><div class="admin-empty"><strong>No se encontraron pedidos</strong><p>Prueba otro nombre, número de pedido o estado.</p></div></td></tr>';
  }
  function renderProducts() {
    const tbody = $("#filas-productos");
    if (!tbody) return;
    const all = [...products.values()],
      query = normalize($("#filtro-busqueda-producto").value),
      category = $("#filtro-categoria-producto").value,
      state = $("#filtro-estado-producto").value;
    const visible = all.filter(
      (p) =>
        normalize(p.nombre + " " + p.sku).includes(query) &&
        (!category || p.categoria === category) &&
        (!state || productState(p) === state),
    );
    stats([
      all.length,
      all.filter((p) => productState(p) === "Disponible").length,
      all.filter((p) => p.stock === 0).length,
      all.filter((p) => p.destacado).length,
    ]);
    $("#resultado-cantidad").textContent =
      `${visible.length} de ${all.length} productos`;
    tbody.innerHTML = visible.length
      ? visible
          .map(
            (p) => `<tr>
      <td><div class="admin-product-cell"><img src="${esc(p.imagen)}" alt="" width="48" height="48"><div><a class="admin-row-title" href="admin-producto-detalle.html?producto=${encodeURIComponent(p.id)}">${esc(p.nombre)}</a><small class="admin-cell-subtitle">${esc(p.sku)}${p.destacado ? " · Destacado" : ""}</small></div></div></td>
      <td>${esc(p.categoria)}</td><td class="text-nowrap fw-semibold">${money(p.precio)}</td><td><strong>${p.stock}</strong> <small>unid.</small></td><td>${productBadge(p)}</td>
      <td><div class="admin-row-actions"><a class="btn btn-sm btn-outline-primary" href="admin-producto-detalle.html?producto=${encodeURIComponent(p.id)}" aria-label="Ver ${esc(p.nombre)}">Ver</a><button type="button" class="btn btn-sm btn-primary" data-editar-producto="${esc(p.id)}" aria-label="Editar ${esc(p.nombre)}">Editar</button></div></td></tr>`,
          )
          .join("")
      : '<tr><td colspan="6"><div class="admin-empty"><strong>No se encontraron productos</strong><p>Prueba otra búsqueda o limpia los filtros.</p></div></td></tr>';
  }
  const params = new URLSearchParams(location.search);
  const selectedOrder = params.get("orden") || "OP-2026-0052";
  const selectedProduct = params.get("producto") || "6-JardinOpalinaGrande";
  function missing(kind) {
    $("#" + kind + "-contenido").hidden = true;
    const el = $("#detalle-error");
    el.hidden = false;
    el.className = "card p-4";
    el.innerHTML = `<h2>${kind === "pedido" ? "Pedido" : "Producto"} no encontrado</h2><p class="mb-0">El identificador no corresponde a un registro. Vuelve a la lista para seleccionar uno.</p>`;
  }
  function formatLocalDate(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? "Fecha no disponible"
      : date.toLocaleString("es-PE");
  }

  function renderOrderDetail() {
    if (!$("#pedido-contenido")) return;
    const o = seed.pedidos[selectedOrder];
    if (!o) {
      missing("pedido");
      return;
    }
    const state = orderState(selectedOrder),
      knownPayment = selectedOrder === "OP-2026-0052";
    $("#detalle-numero-orden").textContent = selectedOrder + " · " + o.cliente;
    $("#estado-actual").innerHTML = badge(state);
    $("#estado-descripcion").textContent = states[state].description;
    const sequence = ["pendiente", "proceso", "enviado", "entregado"];
    $("#progreso-pedido").innerHTML =
      state === "cancelado"
        ? '<li class="current">Pedido cancelado</li>'
        : sequence
            .map(
              (s, i) =>
                `<li class="${i < sequence.indexOf(state) ? "complete" : s === state ? "current" : ""}" ${s === state ? 'aria-current="step"' : ""}><span>${i + 1}</span>${states[s].label}</li>`,
            )
            .join("");
    const subtotal = o.productos.reduce((n, p) => n + amount(p.precio), 0);
    const values = {
      ...o,
      total: money(o.total),
      subtotal: money(subtotal),
      delivery:
        amount(o.total) === subtotal
          ? "Gratis"
          : money(amount(o.total) - subtotal),
      metodo: knownPayment ? "Yape" : "No registrado",
      pago: knownPayment ? "Pendiente de verificación" : "No registrado",
      pagado: knownPayment ? money(0) : "No registrado",
      saldo: knownPayment ? money(o.total) : "No registrado",
    };
    $$("[data-orden]").forEach((e) => {
      const v = values[e.dataset.orden];
      e.textContent = v && v !== "—" ? v : "No registrado";
    });
    const contact = $("[data-orden-whatsapp]");
    const phone = o.telefono.replace(/\D/g, "");
    if (contact) {
      contact.hidden = !phone;
      if (phone) contact.href = "https://wa.me/" + phone;
    }
    $("#detalle-productos-pedido").innerHTML = o.productos
      .map(
        (p) =>
          `<div class="admin-order-item"><img src="${imagePath(p.id)}" alt="" width="64" height="64"><div><a class="admin-row-title" href="admin-producto-detalle.html?producto=${encodeURIComponent(p.id)}">${esc(p.nombre)}</a><small class="admin-cell-subtitle">${esc(p.categoria)} · ${p.cantidad} unid. × ${money(amount(p.precio) / p.cantidad)}</small></div><strong>${money(p.precio)}</strong></div>`,
      )
      .join("");
    const history = Array.isArray(orderChanges[selectedOrder]?.historial)
      ? orderChanges[selectedOrder].historial
      : [];
    $("#historial-pedido").innerHTML =
      [...history]
        .reverse()
        .filter((h) => states[h.estado])
        .map(
          (h) =>
            `<li><strong>${esc(states[h.anterior]?.label || "Estado anterior")} → ${states[h.estado].label}</strong><small>${esc(formatLocalDate(h.fecha))}</small></li>`,
        )
        .join("") +
      `<li><strong>Registro de demostración</strong><small>Estado inicial: ${states[o.estado].label}. Sin fecha de creación registrada.</small></li>`;
  }
  function renderProductDetail() {
    if (!$("#producto-contenido")) return;
    const p = products.get(selectedProduct);
    if (!p) {
      missing("producto");
      return;
    }
    $$("[data-producto]").forEach((el) => {
      const key = el.dataset.producto;
      if (key === "imagen") {
        el.src = p.imagen;
        el.alt = p.nombre;
      } else if (key === "estado") el.innerHTML = productBadge(p);
      else
        el.textContent =
          key === "precio"
            ? money(p.precio)
            : key === "destacado"
              ? p.destacado
                ? "Sí"
                : "No"
              : key === "stock"
                ? p.stock
                : p[key];
    });
    $("[data-editar-producto]").dataset.editarProducto = p.id;
  }
  function render() {
    renderOrders();
    renderProducts();
    renderOrderDetail();
    renderProductDetail();
  }
  function modal(markup) {
    document.body.insertAdjacentHTML("beforeend", markup);
    const element = document.body.lastElementChild;
    return { element, instance: new bootstrap.Modal(element) };
  }
  function restoreFocus(trigger, fallback) {
    requestAnimationFrame(() => {
      (trigger?.isConnected ? trigger : $(fallback))?.focus();
    });
  }
  let stateDialog, changingOrder, stateTrigger;
  function openState(id, trigger) {
    if (!seed.pedidos[id]) return;
    if (!stateDialog) {
      stateDialog = modal(
        `<div class="modal fade" id="modalEstado" tabindex="-1" aria-labelledby="titulo-modal-estado" aria-describedby="subtitulo-modal-estado"><div class="modal-dialog modal-dialog-centered modal-dialog-scrollable"><form class="modal-content" id="form-estado"><div class="modal-header"><div><h2 class="modal-title" id="titulo-modal-estado">Cambiar estado del pedido</h2><p class="text-body-secondary mb-0 mt-1" id="subtitulo-modal-estado"></p></div><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button></div><div class="modal-body"><fieldset><legend class="fs-6">Selecciona el nuevo estado</legend><div id="opciones-estado" class="admin-state-options"></div></fieldset><p class="form-text mb-0 mt-3">El cambio quedará en el historial. No confirma ni modifica el pago.</p></div><div class="modal-footer"><button type="button" class="btn btn-outline-primary" data-bs-dismiss="modal">Cancelar</button><button type="submit" class="btn btn-primary" id="guardar-estado" disabled>Guardar estado</button></div></form></div></div>`,
      );
      $("#form-estado").addEventListener("change", () => {
        $("#guardar-estado").disabled =
          $('input[name="nuevo-estado"]:checked').value ===
          orderState(changingOrder);
      });
      $("#form-estado").addEventListener("submit", (e) => {
        e.preventDefault();
        const state = $('input[name="nuevo-estado"]:checked').value,
          previous = orderState(changingOrder);
        if (state === previous) return;
        const next = read(ORDER_KEY),
          history = Array.isArray(next[changingOrder]?.historial)
            ? next[changingOrder].historial
            : [];
        next[changingOrder] = {
          estado: state,
          historial: [
            ...history,
            {
              anterior: previous,
              estado: state,
              fecha: new Date().toISOString(),
            },
          ],
        };
        if (!save(ORDER_KEY, next)) return;
        orderChanges = next;
        render();
        stateDialog.instance.hide();
        notify(
          "Pedido " +
            changingOrder +
            ": estado actualizado a " +
            states[state].label +
            ".",
        );
      });
      stateDialog.element.addEventListener("shown.bs.modal", () =>
        $('input[name="nuevo-estado"]:checked').focus(),
      );
      stateDialog.element.addEventListener("hidden.bs.modal", () =>
        restoreFocus(stateTrigger, "#buscar-pedido"),
      );
    }
    changingOrder = id;
    stateTrigger = trigger;
    $("#subtitulo-modal-estado").textContent =
      id + " · " + seed.pedidos[id].cliente;
    $("#opciones-estado").innerHTML = Object.entries(states)
      .map(
        ([key, state]) =>
          `<label class="admin-state-option"><input class="form-check-input" type="radio" name="nuevo-estado" value="${key}" ${orderState(id) === key ? "checked" : ""}><span><strong>${esc(state.label)}</strong><small>${esc(state.description)}</small></span></label>`,
      )
      .join("");
    $("#guardar-estado").disabled = true;
    stateDialog.instance.show();
  }
  let productDialog,
    editingId,
    productTrigger,
    busy = false;
  function openProduct(id, trigger) {
    const p = products.get(id);
    if (id && !p) return;
    if (!productDialog) {
      const categories = [
        "Romántico",
        "Cumpleaños",
        "Temporada",
        "Premium",
        "Condolencias",
        "Personalizado",
      ];
      const categoryOptions = categories
        .map(
          (category) =>
            `<option value="${esc(category)}">${esc(category)}</option>`,
        )
        .join("");

      productDialog = modal(`
        <div
          class="modal fade"
          id="modalProducto"
          tabindex="-1"
          aria-labelledby="titulo-modal-producto"
        >
          <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <form class="modal-content" id="form-producto">
              <div class="modal-header">
                <div>
                  <h2 class="modal-title" id="titulo-modal-producto"></h2>
                  <p class="text-body-secondary mb-0 mt-1">
                    Los campos con * son obligatorios.
                  </p>
                </div>
                <button
                  type="button"
                  class="btn-close"
                  data-bs-dismiss="modal"
                  aria-label="Cerrar"
                ></button>
              </div>

              <div class="modal-body">
                <div
                  id="producto-error"
                  class="alert alert-danger"
                  role="alert"
                  hidden
                ></div>

                <div class="row g-3">
                  <div class="col-md-8">
                    <label class="form-label" for="producto-nombre">
                      Nombre del producto *
                    </label>
                    <input
                      class="form-control"
                      id="producto-nombre"
                      maxlength="100"
                      required
                    >
                  </div>

                  <div class="col-md-4">
                    <label class="form-label" for="producto-sku">
                      Código / SKU *
                    </label>
                    <input
                      class="form-control"
                      id="producto-sku"
                      maxlength="40"
                      required
                    >
                  </div>

                  <div class="col-12">
                    <label class="form-label" for="producto-descripcion">
                      Descripción
                    </label>
                    <textarea
                      class="form-control"
                      id="producto-descripcion"
                      rows="3"
                      maxlength="1200"
                    ></textarea>
                  </div>

                  <div class="col-md-6">
                    <label class="form-label" for="producto-categoria">
                      Categoría *
                    </label>
                    <select
                      class="form-select"
                      id="producto-categoria"
                      required
                    >
                      ${categoryOptions}
                    </select>
                  </div>

                  <div class="col-6 col-md-3">
                    <label class="form-label" for="producto-precio">
                      Precio (S/) *
                    </label>
                    <input
                      class="form-control"
                      id="producto-precio"
                      type="number"
                      min="0"
                      max="999999.99"
                      step="0.01"
                      required
                    >
                  </div>

                  <div class="col-6 col-md-3">
                    <label class="form-label" for="producto-stock">Stock *</label>
                    <input
                      class="form-control"
                      id="producto-stock"
                      type="number"
                      min="0"
                      max="999999"
                      step="1"
                      required
                    >
                  </div>

                  <div class="col-12">
                    <label class="form-label" for="producto-imagen">
                      Imagen del producto
                    </label>
                    <div class="d-flex gap-3 align-items-center">
                      <img
                        id="producto-preview"
                        class="admin-image-preview"
                        alt="Vista previa del producto"
                        width="72"
                        height="72"
                      >
                      <div class="flex-grow-1">
                        <input
                          class="form-control"
                          id="producto-imagen"
                          type="file"
                          accept="image/png,image/jpeg,image/webp"
                          aria-describedby="ayuda-imagen"
                        >
                        <p class="form-text mb-0" id="ayuda-imagen">
                          JPG, PNG o WebP, máximo 2 MB. Conserva la imagen actual si no eliges otra.
                        </p>
                      </div>
                    </div>
                  </div>

                  <div class="col-12">
                    <label class="form-check">
                      <input
                        class="form-check-input"
                        type="checkbox"
                        id="producto-disponible"
                      >
                      <span class="form-check-label">Disponible para venta</span>
                    </label>
                    <p class="form-text">
                      Con stock 0 se mostrará como «Sin stock».
                    </p>
                    <label class="form-check mb-0">
                      <input
                        class="form-check-input"
                        type="checkbox"
                        id="producto-destacado"
                      >
                      <span class="form-check-label">Producto destacado</span>
                    </label>
                  </div>
                </div>
              </div>

              <div class="modal-footer">
                <button
                  type="button"
                  class="btn btn-outline-primary"
                  data-bs-dismiss="modal"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  id="guardar-producto"
                  class="btn btn-primary"
                >
                  Guardar producto
                </button>
              </div>
            </form>
          </div>
        </div>
      `);
      $("#form-producto").addEventListener("submit", saveProduct);
      productDialog.element.addEventListener("hide.bs.modal", (e) => {
        if (busy) e.preventDefault();
      });
      productDialog.element.addEventListener("shown.bs.modal", () =>
        $("#producto-nombre").focus(),
      );
      productDialog.element.addEventListener("hidden.bs.modal", () =>
        restoreFocus(productTrigger, "[data-nuevo-producto]"),
      );
    }
    editingId = id || null;
    productTrigger = trigger;
    $("#form-producto").reset();
    $("#producto-error").hidden = true;
    $("#titulo-modal-producto").textContent = p
      ? "Editar producto"
      : "Nuevo producto";
    for (const key of [
      "nombre",
      "sku",
      "descripcion",
      "categoria",
      "precio",
      "stock",
    ])
      $("#producto-" + key).value =
        p?.[key] ??
        {
          sku: "OPA-" + Date.now().toString().slice(-6),
          categoria: "Romántico",
          stock: 0,
        }[key] ??
        "";
    $("#producto-disponible").checked = p?.disponible ?? true;
    $("#producto-destacado").checked = p?.destacado ?? false;
    $("#producto-preview").src =
      p?.imagen || "../assets/imagenes/Opalina-Logo.png";
    productDialog.instance.show();
  }
  function fileImage(file) {
    if (file.size > 2 * 1024 * 1024)
      return Promise.reject(
        new Error("La imagen supera los 2 MB. Elige una más pequeña."),
      );
    if (!["image/png", "image/jpeg", "image/webp"].includes(file.type))
      return Promise.reject(new Error("Elige una imagen JPG, PNG o WebP."));
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject(new Error("No se pudo leer la imagen."));
      reader.onload = async () => {
        try {
          const img = new Image();
          img.src = reader.result;
          await img.decode();
          resolve(reader.result);
        } catch {
          reject(new Error("El archivo no contiene una imagen válida."));
        }
      };
      reader.readAsDataURL(file);
    });
  }
  async function saveProduct(e) {
    e.preventDefault();
    if (busy || !e.target.reportValidity()) return;
    const error = $("#producto-error");
    error.hidden = true;
    const value = (key) => $("#producto-" + key).value.trim();
    const p = {
      id:
        editingId ||
        "nuevo-" + Date.now() + "-" + Math.random().toString(36).slice(2, 7),
      nombre: value("nombre"),
      sku: value("sku"),
      descripcion: value("descripcion"),
      categoria: value("categoria"),
      precio: Number(value("precio")),
      stock: Number(value("stock")),
      disponible: $("#producto-disponible").checked,
      destacado: $("#producto-destacado").checked,
      imagen:
        products.get(editingId)?.imagen ||
        "../assets/imagenes/Opalina-Logo.png",
    };
    if (!p.nombre || !p.sku) {
      error.textContent = "Escribe un nombre y un código válidos.";
      error.hidden = false;
      return;
    }
    if (
      [...products.values()].some(
        (other) =>
          other.id !== p.id && normalize(other.sku) === normalize(p.sku),
      )
    ) {
      error.textContent = "Ese código / SKU ya pertenece a otro producto.";
      error.hidden = false;
      $("#producto-sku").focus();
      return;
    }
    busy = true;
    $("#guardar-producto").disabled = true;
    $("#guardar-producto").textContent = "Guardando…";
    try {
      const file = $("#producto-imagen").files[0];
      if (file) p.imagen = await fileImage(file);
      const next = { ...read(PRODUCT_KEY), [p.id]: p };
      if (!save(PRODUCT_KEY, next)) {
        error.textContent =
          "No se guardaron los cambios. El almacenamiento está bloqueado o lleno.";
        error.hidden = false;
        return;
      }
      productChanges = next;
      loadProducts();
      render();
      busy = false;
      productDialog.instance.hide();
      notify(
        editingId
          ? "Producto actualizado correctamente."
          : "Producto creado correctamente.",
      );
    } catch (reason) {
      error.textContent = reason.message;
      error.hidden = false;
    } finally {
      busy = false;
      $("#guardar-producto").disabled = false;
      $("#guardar-producto").textContent = "Guardar producto";
    }
  }
  document.addEventListener("click", (e) => {
    const b = e.target.closest("button");
    if (!b) return;
    if (b.hasAttribute("data-filtro-pedido")) {
      filter = b.dataset.filtroPedido;
      renderOrders();
    }
    if (b.hasAttribute("data-estado-orden"))
      openState(b.dataset.estadoOrden, b);
    if (b.hasAttribute("data-abrir-estado")) openState(selectedOrder, b);
    if (b.hasAttribute("data-editar-producto"))
      openProduct(b.dataset.editarProducto || selectedProduct, b);
    if (b.hasAttribute("data-nuevo-producto")) openProduct(null, b);
    if (b.hasAttribute("data-imprimir-pedido")) window.print();
    if (b.id === "limpiar-productos") {
      [
        "#filtro-busqueda-producto",
        "#filtro-categoria-producto",
        "#filtro-estado-producto",
      ].forEach((s) => {
        $(s).value = "";
      });
      renderProducts();
    }
  });
  $("#buscar-pedido")?.addEventListener("input", renderOrders);
  $("#filtro-busqueda-producto")?.addEventListener("input", renderProducts);
  ["#filtro-categoria-producto", "#filtro-estado-producto"].forEach((s) =>
    $(s)?.addEventListener("change", renderProducts),
  );
  // Secciones secundarias: usan los mismos registros que Pedidos.
  function clients() {
    const grouped = new Map();
    Object.entries(seed.pedidos).forEach(([id, o]) => {
      const key = o.telefono.replace(/\D/g, "") || normalize(o.cliente);
      if (!grouped.has(key))
        grouped.set(key, {
          id: key,
          nombre: o.cliente,
          email: o.email === "—" ? "" : o.email,
          telefono: o.telefono,
          pedidos: [],
          total: 0,
        });
      const c = grouped.get(key);
      c.pedidos.push(id);
      if (orderState(id) !== "cancelado") c.total += amount(o.total);
    });
    return [...grouped.values()].map((c) => ({
      ...c,
      ...clientChanges[c.id],
      id: c.id,
      pedidos: c.pedidos,
      total: c.total,
    }));
  }
  function renderClients() {
    const tbody = $("#filas-clientes");
    if (!tbody) return;
    const all = clients(),
      query = normalize($("#buscar-cliente").value),
      sort = $("#orden-clientes").value;
    const visible = all
      .filter((c) =>
        normalize(c.nombre + " " + c.email + " " + c.telefono).includes(query),
      )
      .sort((a, b) =>
        sort === "pedidos"
          ? b.pedidos.length - a.pedidos.length
          : sort === "total"
            ? b.total - a.total
            : a.nombre.localeCompare(b.nombre, "es"),
      );
    const count = all.reduce((n, c) => n + c.pedidos.length, 0),
      total = all.reduce((n, c) => n + c.total, 0);
    stats([all.length, count, money(total), money(count ? total / count : 0)]);
    $("#resultado-cantidad").textContent =
      visible.length + " de " + all.length + " clientes";
    tbody.innerHTML =
      visible
        .map(
          (c) =>
            `<tr><td><strong>${esc(c.nombre)}</strong></td><td>${esc(c.email || "No registrado")}</td><td class="text-nowrap">${esc(c.telefono)}</td><td>${c.pedidos.length}</td><td class="text-nowrap">${money(c.total)}</td><td><button type="button" class="btn btn-outline-primary btn-sm" data-cliente="${esc(c.id)}">Ver / editar</button></td></tr>`,
        )
        .join("") ||
      '<tr><td colspan="6"><div class="admin-empty"><strong>No se encontraron clientes</strong><p>Prueba otro nombre, correo o teléfono.</p></div></td></tr>';
  }
  let clientDialog, clientId, clientTrigger;
  function openClient(id, trigger) {
    const c = clients().find((c) => c.id === id);
    if (!c) return;
    if (!clientDialog) {
      clientDialog = modal(
        `<div class="modal fade" tabindex="-1" aria-labelledby="titulo-cliente"><div class="modal-dialog modal-dialog-centered modal-dialog-scrollable"><form class="modal-content" id="form-cliente"><div class="modal-header"><h2 class="modal-title" id="titulo-cliente">Información del cliente</h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button></div><div class="modal-body"><div class="mb-3"><label class="form-label" for="cliente-nombre">Nombre *</label><input id="cliente-nombre" class="form-control" maxlength="100" required></div><div class="mb-3"><label class="form-label" for="cliente-email">Correo</label><input id="cliente-email" type="email" class="form-control" maxlength="150"></div><div class="mb-4"><label class="form-label" for="cliente-telefono">Teléfono *</label><input id="cliente-telefono" type="tel" class="form-control" maxlength="25" pattern="[+0-9 ]{6,25}" required></div><h3>Pedidos registrados</h3><div id="pedidos-cliente" class="list-group"></div><p class="form-text mt-3 mb-0">Los cambios de contacto no modifican pedidos anteriores.</p></div><div class="modal-footer"><button type="button" class="btn btn-outline-primary" data-bs-dismiss="modal">Cancelar</button><button type="submit" class="btn btn-primary">Guardar contacto</button></div></form></div></div>`,
      );
      $("#form-cliente").addEventListener("submit", (e) => {
        e.preventDefault();
        const name = $("#cliente-nombre");
        name.setCustomValidity(
          name.value.trim() ? "" : "Escribe un nombre válido.",
        );
        if (!e.target.reportValidity()) return;
        const next = {
          ...read(CLIENT_KEY),
          [clientId]: {
            nombre: name.value.trim(),
            email: $("#cliente-email").value.trim(),
            telefono: $("#cliente-telefono").value.trim(),
          },
        };
        if (save(CLIENT_KEY, next)) {
          clientChanges = next;
          renderClients();
          clientDialog.instance.hide();
          notify("Contacto actualizado correctamente.");
        }
      });
      $("#cliente-nombre").addEventListener("input", (e) =>
        e.target.setCustomValidity(""),
      );
      clientDialog.element.addEventListener("hidden.bs.modal", () =>
        restoreFocus(clientTrigger, "#buscar-cliente"),
      );
      clientDialog.element.addEventListener("shown.bs.modal", () =>
        $("#cliente-nombre").focus(),
      );
    }
    clientId = id;
    clientTrigger = trigger;
    for (const key of ["nombre", "email", "telefono"])
      $("#cliente-" + key).value = c[key];
    $("#cliente-nombre").setCustomValidity("");
    $("#pedidos-cliente").innerHTML = c.pedidos
      .map(
        (id) =>
          `<a class="list-group-item list-group-item-action d-flex justify-content-between gap-3" href="admin-pedido-detalle.html?orden=${encodeURIComponent(id)}"><strong>${esc(id)}</strong>${badge(orderState(id))}</a>`,
      )
      .join("");
    clientDialog.instance.show();
  }
  function reportOrders() {
    const state = $("#estado-reporte")?.value || "todos";
    return Object.entries(seed.pedidos).filter(
      ([id]) => state === "todos" || orderState(id) === state,
    );
  }
  function renderReports() {
    const tbody = $("#filas-reporte");
    if (!tbody) return;
    const orders = reportOrders(),
      grouped = new Map();
    orders.forEach(([, o]) =>
      o.productos.forEach((p) => {
        const item = grouped.get(p.id) || {
          id: p.id,
          nombre: p.nombre,
          unidades: 0,
          total: 0,
        };
        item.unidades += p.cantidad;
        item.total += amount(p.precio);
        grouped.set(p.id, item);
      }),
    );
    const total = orders.reduce((n, [, o]) => n + amount(o.total), 0);
    const rows = [...grouped.values()].sort((a, b) => b.unidades - a.unidades);
    stats([
      orders.length,
      money(total),
      money(orders.length ? total / orders.length : 0),
      rows.reduce((n, p) => n + p.unidades, 0),
    ]);
    $("#resultado-cantidad").textContent = rows.length + " productos";
    tbody.innerHTML =
      rows
        .map(
          (p) =>
            `<tr><td><a class="admin-row-title" href="admin-producto-detalle.html?producto=${encodeURIComponent(p.id)}">${esc(p.nombre)}</a></td><td>${p.unidades}</td><td>${money(p.total)}</td></tr>`,
        )
        .join("") ||
      '<tr><td colspan="3"><div class="admin-empty"><strong>No hay pedidos con este estado</strong><p>Selecciona otro estado para consultar el resumen.</p></div></td></tr>';
  }
  $("#buscar-cliente")?.addEventListener("input", renderClients);
  $("#orden-clientes")?.addEventListener("change", renderClients);
  $("#estado-reporte")?.addEventListener("change", renderReports);
  document.addEventListener("click", (e) => {
    const b = e.target.closest("[data-cliente]");
    if (b) openClient(b.dataset.cliente, b);
  });
  const config = read(CONFIG_KEY);
  $$("[data-config-form]").forEach((form) => {
    $$("input", form).forEach((input) => {
      if (typeof config[input.id] === "string") input.value = config[input.id];
      input.required = !["instagram", "facebook", "tiktok"].includes(input.id);
      if (input.type !== "number") input.maxLength = 150;
    });
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      $$("input", form).forEach((input) =>
        input.setCustomValidity(
          input.required && !input.value.trim() ? "Completa este campo." : "",
        ),
      );
      if (!form.reportValidity()) return;
      const next = read(CONFIG_KEY);
      $$("input", form).forEach((input) => {
        next[input.id] = input.value.trim();
      });
      if (save(CONFIG_KEY, next))
        notify("Configuración guardada en este navegador.");
    });
    form.addEventListener("input", (e) => e.target.setCustomValidity?.(""));
  });
  window.addEventListener("storage", (event) => {
    if (event.storageArea && event.storageArea !== localStorage) return;
    if (
      event.key !== null &&
      ![PRODUCT_KEY, ORDER_KEY, CLIENT_KEY, CONFIG_KEY].includes(event.key)
    ) {
      return;
    }

    productChanges = read(PRODUCT_KEY);
    orderChanges = read(ORDER_KEY);
    clientChanges = read(CLIENT_KEY);
    loadProducts();
    render();
    renderClients();
    renderReports();

    if (event.key === CONFIG_KEY || event.key === null) {
      const latestConfig = read(CONFIG_KEY);
      $$("[data-config-form]").forEach((form) => {
        $$("input", form).forEach((input) => {
          if (typeof latestConfig[input.id] === "string") {
            input.value = latestConfig[input.id];
          }
        });
      });
    }
  });

  renderClients();
  renderReports();

  render();
})();
