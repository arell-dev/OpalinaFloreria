/* Estado compartido de la tienda y carrito de demostración. */
(() => {
  "use strict";

  const CART_KEY = "opalina-carrito-v1";
  const SELECTED_PRODUCT_KEY = "opalina-producto-seleccionado-v1";
  const FREE_DELIVERY_LIMIT = 200;
  const DELIVERY_FEE = 15;
  const PRODUCT_IMAGE_PATTERN =
    /^\.\.\/assets\/imagenes\/productos\/[\p{L}\p{N}_-]+\.(?:png|jpe?g|webp)$/iu;

  const initialCart = [
    {
      id: "12-RamoOpalinaMorado",
      nombre: "Ramo Opalina Morado",
      precio: 55,
      stock: 28,
      cantidad: 2,
      imagen: "../assets/imagenes/productos/12-RamoOpalinaMorado.png",
    },
    {
      id: "3-Buchon12Rosas",
      nombre: "Buchón 12 Rosas",
      precio: 85,
      stock: 7,
      cantidad: 1,
      imagen: "../assets/imagenes/productos/3-Buchon12Rosas.jpg",
    },
  ];

  const productsByCard = {
    1: { id: "8-MiniRamoCotidiano", stock: 18 },
    2: { id: "12-RamoOpalinaMorado", stock: 28 },
    3: { id: "9-RamoAmarillo21Marzo", stock: 12 },
    4: { id: "3-Buchon12Rosas", stock: 7 },
    5: { id: "2-BoxFloralConChocolates", stock: 9 },
    6: { id: "11-RamoLiriosPremium", stock: 4 },
    7: { id: "6-JardinOpalinaGrande", stock: 3 },
    8: { id: "10-RamoGraduación", stock: 15 },
    9: { id: "7-LagrimaCondolencias", stock: 6 },
    10: { id: "4-CoronaPremium", stock: 0 },
    11: { id: "5-DuoBobaFlor", stock: 22 },
    12: { id: "1-arregloPersonalizado", stock: 11 },
  };
  const cardIdByProduct = Object.fromEntries(
    Object.entries(productsByCard).map(([cardId, product]) => [
      product.id,
      cardId,
    ]),
  );

  const escapeHTML = (value) =>
    String(value ?? "").replace(/[&<>"']/g, (character) => {
      const entities = {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      };
      return entities[character];
    });

  const formatMoney = (value) =>
    `S/ ${Number(value).toLocaleString("es-PE", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;

  function isSafeProductImage(value) {
    return typeof value === "string" && PRODUCT_IMAGE_PATTERN.test(value);
  }

  function copyInitialCart() {
    return initialCart.map((item) => ({ ...item }));
  }

  function isValidCartItem(item) {
    return (
      item &&
      typeof item.id === "string" &&
      typeof item.nombre === "string" &&
      Number.isFinite(item.precio) &&
      item.precio >= 0 &&
      Number.isInteger(item.stock) &&
      item.stock >= 0 &&
      Number.isInteger(item.cantidad) &&
      item.cantidad > 0 &&
      item.cantidad <= item.stock &&
      isSafeProductImage(item.imagen)
    );
  }

  function readCart() {
    try {
      const stored = localStorage.getItem(CART_KEY);
      if (stored === null) return copyInitialCart();

      const parsed = JSON.parse(stored);
      return Array.isArray(parsed) ? parsed.filter(isValidCartItem) : [];
    } catch {
      return copyInitialCart();
    }
  }

  function saveCart(items) {
    try {
      localStorage.setItem(CART_KEY, JSON.stringify(items));
      return true;
    } catch {
      return false;
    }
  }

  function countItems(items = readCart()) {
    return items.reduce((total, item) => total + item.cantidad, 0);
  }

  window.OpalinaCarrito = Object.freeze({
    key: CART_KEY,
    read: readCart,
    save: saveCart,
    count: countItems,
    money: formatMoney,
  });

  function updateCartCounter(items = readCart()) {
    const count = countItems(items);
    const label = `${count} artículo${count === 1 ? "" : "s"} en el carrito`;

    document.querySelectorAll("#contador-carrito").forEach((counter) => {
      if (counter.firstChild) counter.firstChild.nodeValue = String(count);
      counter.setAttribute("aria-label", label);
      counter.hidden = count === 0;
    });
  }

  function setCurrentNavigation() {
    const currentPage = location.pathname.split("/").pop() || "index.html";

    document.querySelectorAll(".navbar a.nav-link[href]").forEach((link) => {
      const targetPage = new URL(link.href, location.href).pathname
        .split("/")
        .pop();
      const isCurrentPage =
        targetPage === currentPage ||
        (currentPage === "producto.html" && targetPage === "catalogo.html");

      link.classList.toggle("active", isCurrentPage);
      if (isCurrentPage) link.setAttribute("aria-current", "page");
      else link.removeAttribute("aria-current");
    });

    document
      .querySelectorAll('nav[aria-label="Enlaces legales"] a[href]')
      .forEach((link) => {
        const targetPage = new URL(link.href, location.href).pathname
          .split("/")
          .pop();
        if (targetPage === currentPage) {
          link.setAttribute("aria-current", "page");
        } else {
          link.removeAttribute("aria-current");
        }
      });
  }

  function saveSelectedProduct(product) {
    try {
      sessionStorage.setItem(SELECTED_PRODUCT_KEY, JSON.stringify(product));
      return true;
    } catch {
      return false;
    }
  }

  function readSelectedProduct(cardId) {
    const expectedProduct = productsByCard[cardId];
    if (!expectedProduct) return null;

    try {
      const product = JSON.parse(
        sessionStorage.getItem(SELECTED_PRODUCT_KEY) || "null",
      );
      if (
        !product ||
        product.id !== expectedProduct.id ||
        !Number.isFinite(product.precio) ||
        !Number.isInteger(product.stock) ||
        !isSafeProductImage(product.imagen)
      ) {
        return null;
      }
      return product;
    } catch {
      return null;
    }
  }

  function bindCatalogNavigation() {
    const grid = document.querySelector("#grid-productos");
    if (!grid) return;

    grid.addEventListener("click", (event) => {
      if (!(event.target instanceof Element)) return;

      const link = event.target.closest('a[href="producto.html"]');
      const card = link?.closest("article[data-id]");
      if (!link || !card) return;

      const mapping = productsByCard[card.dataset.id];
      if (!mapping) return;

      const image = card.querySelector("img");
      const priceText = card.querySelector("strong")?.textContent ?? "";
      const product = {
        id: mapping.id,
        nombre:
          card.querySelector("h3")?.textContent.trim() || "Arreglo floral",
        categoria:
          card.querySelector(".insignia")?.textContent.trim() || "Temporada",
        descripcion: card.querySelector(".card-text")?.textContent.trim() || "",
        precio: Number(priceText.replace(/[^\d.]/g, "")),
        stock: mapping.stock,
        imagen: image?.getAttribute("src") || "",
        alt: image?.alt || "",
      };

      if (
        !Number.isFinite(product.precio) ||
        !isSafeProductImage(product.imagen)
      ) {
        return;
      }

      event.preventDefault();
      saveSelectedProduct(product);
      location.href = `producto.html?producto=${encodeURIComponent(card.dataset.id)}`;
    });
  }

  function renderSelectedProduct() {
    if (location.pathname.split("/").pop() !== "producto.html") return;

    const cardId = new URLSearchParams(location.search).get("producto");
    const product = readSelectedProduct(cardId);
    if (!product) return;

    const title = document.querySelector("#nombre-producto");
    const image = document.querySelector("#detalle-producto img");
    const category = document.querySelector("#detalle-producto .insignia");
    const price = document.querySelector("main .text-success.fw-bold");
    const description = title?.parentElement.querySelector(
      "p.text-body-secondary",
    );
    const stockText = document.querySelector(
      "main .list-group-item .text-body-secondary",
    );
    const quantity = document.querySelector("#cantidad");
    const addButton = document.querySelector("#btn-agregar");

    if (title) title.textContent = product.nombre;
    document.title = `${product.nombre} | Opalina Florería Piura`;
    if (image) {
      image.src = product.imagen;
      image.alt = product.alt || product.nombre;
    }
    if (category) category.textContent = product.categoria;
    if (price) price.textContent = formatMoney(product.precio);
    if (description) description.textContent = product.descripcion;
    if (stockText) {
      stockText.textContent = product.stock
        ? `${product.stock} en stock`
        : "Sin stock";
    }
    if (quantity) {
      quantity.max = String(product.stock);
      quantity.value = "1";
      quantity.disabled = product.stock < 1;
    }
    if (addButton) {
      const unavailable = product.stock < 1;
      addButton.dataset.productoId = product.id;
      addButton.classList.toggle("disabled", unavailable);
      addButton.setAttribute("aria-disabled", String(unavailable));
      addButton.textContent = unavailable ? "Agotado" : "Agregar al carrito";
    }
  }

  function showCartMessage(message) {
    const notice = document.querySelector("#aviso-carrito");
    if (!notice) return;

    notice.textContent = message;
    notice.hidden = false;
    window.clearTimeout(showCartMessage.timer);
    showCartMessage.timer = window.setTimeout(() => {
      notice.hidden = true;
    }, 4500);
  }

  function renderCart() {
    const cart = document.querySelector("#items-carrito");
    if (!cart) return;

    const items = readCart();
    const subtotalNode = document.querySelector("#subtotal-carrito");
    const deliveryNode = document.querySelector("#delivery-carrito");
    const totalNode = document.querySelector("#total-carrito");
    const checkout = document.querySelector("[data-ir-a-pedido]");
    const emptyState = document.querySelector("#carrito-vacio");
    const subtotal = items.reduce(
      (total, item) => total + item.precio * item.cantidad,
      0,
    );
    const delivery =
      items.length === 0
        ? 0
        : subtotal >= FREE_DELIVERY_LIMIT
          ? 0
          : DELIVERY_FEE;

    cart.innerHTML = items
      .map(
        (item) => `
          <article class="card shadow-sm carrito-item" data-item-carrito data-id="${escapeHTML(item.id)}">
            <div class="card-body p-3 p-md-4">
              <div class="row g-3 align-items-center">
                <div class="col-4 col-sm-auto">
                  <div class="ratio ratio-1x1 rounded overflow-hidden miniatura-carrito">
                    <img src="${escapeHTML(item.imagen)}" alt="${escapeHTML(item.nombre)}" class="w-100 h-100 object-fit-cover">
                  </div>
                </div>
                <div class="col-8 col-sm">
                  <h2 class="h5 mb-1">${escapeHTML(item.nombre)}</h2>
                  <p class="mb-0 fw-bold">${formatMoney(item.precio)} <span class="text-body-secondary fw-normal">por unidad</span></p>
                </div>
                <div class="col-7 col-sm-auto">
                  <div class="input-group input-group-sm">
                    <button class="btn btn-outline-secondary" type="button" data-accion="restar" aria-label="Disminuir ${escapeHTML(item.nombre)}">−</button>
                    <span class="input-group-text bg-white cantidad-carrito" aria-live="polite">${item.cantidad}</span>
                    <button class="btn btn-outline-secondary" type="button" data-accion="sumar" aria-label="Aumentar ${escapeHTML(item.nombre)}" ${item.cantidad >= item.stock ? "disabled" : ""}>+</button>
                  </div>
                  <small class="text-body-secondary">${item.stock} disponibles</small>
                </div>
                <div class="col-5 col-sm-auto text-end">
                  <button class="btn btn-outline-primary btn-sm" type="button" data-accion="eliminar" aria-label="Quitar ${escapeHTML(item.nombre)}">Quitar</button>
                  <strong class="d-block mt-2">${formatMoney(item.precio * item.cantidad)}</strong>
                </div>
              </div>
            </div>
          </article>`,
      )
      .join("");

    if (subtotalNode) subtotalNode.textContent = formatMoney(subtotal);
    if (deliveryNode) {
      deliveryNode.textContent =
        items.length > 0 && delivery === 0 ? "Gratis" : formatMoney(delivery);
    }
    if (totalNode) totalNode.textContent = formatMoney(subtotal + delivery);
    if (emptyState) emptyState.hidden = items.length > 0;
    cart.hidden = items.length === 0;

    if (checkout) {
      const hasItems = items.length > 0;
      checkout.classList.toggle("disabled", !hasItems);
      checkout.setAttribute("aria-disabled", String(!hasItems));
      checkout.tabIndex = hasItems ? 0 : -1;
      checkout.href = hasItems ? "pedido.html" : "#carrito-vacio";
    }

    updateCartCounter(items);
  }

  function updateCartItem(event) {
    if (!(event.target instanceof Element)) return;

    const button = event.target.closest("button[data-accion]");
    if (!button) return;

    const article = button.closest("[data-item-carrito]");
    if (!article) return;

    const items = readCart();
    const item = items.find((cartItem) => cartItem.id === article.dataset.id);
    if (!item) return;

    if (button.dataset.accion === "eliminar") {
      items.splice(items.indexOf(item), 1);
    } else if (button.dataset.accion === "sumar") {
      if (item.cantidad >= item.stock) {
        showCartMessage("No hay más unidades disponibles.");
        return;
      }
      item.cantidad += 1;
    } else if (button.dataset.accion === "restar") {
      item.cantidad = Math.max(1, item.cantidad - 1);
    } else {
      return;
    }

    if (!saveCart(items)) {
      showCartMessage("No se pudieron guardar los cambios del carrito.");
      return;
    }

    renderCart();
    showCartMessage(
      button.dataset.accion === "eliminar"
        ? "Producto quitado del carrito."
        : "Carrito actualizado.",
    );
  }

  function parsePrice(value) {
    const parsed = Number(String(value ?? "").replace(/[^\d.]/g, ""));
    return Number.isFinite(parsed) ? parsed : 0;
  }

  function getProductForCart(button) {
    const quantityInput = document.querySelector("#cantidad");
    const selectedProduct = readSelectedProduct(
      new URLSearchParams(location.search).get("producto"),
    );
    const name = document.querySelector("#nombre-producto");
    const image =
      document.querySelector("[data-producto-imagen]") ||
      document.querySelector(".imagen-producto-principal") ||
      document.querySelector("#detalle-producto img");
    const price =
      document.querySelector("[data-producto-precio]") ||
      document.querySelector("main .text-success.fw-bold");
    const stock = selectedProduct?.stock ?? Number(quantityInput?.max || 1);
    const quantity = Number(quantityInput?.value || 1);
    const unitPrice = selectedProduct?.precio ?? parsePrice(price?.textContent);
    const id =
      button.dataset.productoId ||
      selectedProduct?.id ||
      "12-RamoOpalinaMorado";
    const itemImage = selectedProduct?.imagen || image?.getAttribute("src");

    return {
      id,
      nombre:
        selectedProduct?.nombre || name?.textContent.trim() || "Arreglo floral",
      precio: unitPrice,
      stock,
      cantidad: quantity,
      imagen: itemImage,
    };
  }

  function addProductToCart(event) {
    event.preventDefault();

    const button = event.currentTarget;
    if (button.getAttribute("aria-disabled") === "true") {
      showCartMessage("Este producto no tiene stock disponible.");
      return;
    }
    const quantityInput = document.querySelector("#cantidad");
    const product = getProductForCart(button);
    const existingItem = readCart().find((item) => item.id === product.id);

    if (
      !product.id ||
      !Number.isInteger(product.cantidad) ||
      product.cantidad < 1 ||
      product.cantidad > product.stock
    ) {
      quantityInput?.setCustomValidity("Selecciona una cantidad disponible.");
      quantityInput?.reportValidity();
      return;
    }

    if ((existingItem?.cantidad || 0) + product.cantidad > product.stock) {
      quantityInput?.setCustomValidity(
        "La cantidad supera las unidades disponibles.",
      );
      quantityInput?.reportValidity();
      return;
    }

    const items = readCart();
    const item = items.find((cartItem) => cartItem.id === product.id);
    if (item) {
      item.cantidad += product.cantidad;
    } else {
      items.push({
        id: product.id,
        nombre: product.nombre,
        precio: product.precio,
        stock: product.stock,
        cantidad: product.cantidad,
        imagen: isSafeProductImage(product.imagen)
          ? product.imagen
          : "../assets/imagenes/productos/12-RamoOpalinaMorado.png",
      });
    }

    if (saveCart(items)) {
      location.href = "carrito.html?agregado=1";
    } else {
      showCartMessage("No se pudo guardar el producto en el carrito.");
    }
  }

  function showAddedNotice() {
    const params = new URLSearchParams(location.search);
    if (!params.has("agregado")) return;

    showCartMessage("Producto agregado al carrito.");
    history.replaceState(null, "", location.pathname);
  }

  updateCartCounter();
  setCurrentNavigation();
  bindCatalogNavigation();
  renderSelectedProduct();
  renderCart();
  showAddedNotice();

  window.addEventListener("storage", (event) => {
    if (event.key === CART_KEY || event.key === null) updateCartCounter();
  });

  const cart = document.querySelector("#items-carrito");
  cart?.addEventListener("click", updateCartItem);

  const quantityInput = document.querySelector("#cantidad");
  quantityInput?.addEventListener("input", () => {
    quantityInput.setCustomValidity("");
  });

  const addButton = document.querySelector("#btn-agregar");
  addButton?.addEventListener("click", addProductToCart);
})();
