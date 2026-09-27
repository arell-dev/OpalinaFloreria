import { demoRepository } from "../repositories/demo-repository.js";

const DELIVERY_FEE = 15;
const FREE_DELIVERY_LIMIT = 200;
const validStatuses = new Set([
  "pendiente",
  "proceso",
  "enviado",
  "entregado",
  "cancelado",
]);
const clone = (value) => structuredClone(value);
const normalize = (value) =>
  String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("es")
    .trim();

function updateState(mutator) {
  const state = demoRepository.readState();
  const result = mutator(state);
  if (!demoRepository.writeState(state))
    throw new Error(
      "No se pudo guardar el cambio. Revisa el almacenamiento del navegador.",
    );
  return result === undefined ? state : result;
}

export const storeService = Object.freeze({
  listProducts({ includeUnavailable = false } = {}) {
    return demoRepository
      .readState()
      .products.filter(
        (product) =>
          includeUnavailable || (product.available && product.stock > 0),
      )
      .map(clone);
  },

  getProduct(id) {
    const product = demoRepository
      .readState()
      .products.find((item) => item.id === id);
    return product ? clone(product) : null;
  },

  searchProducts({ query = "", category = "todos", price = "todos" } = {}) {
    const normalizedQuery = normalize(query);
    return this.listProducts().filter((product) => {
      const matchesQuery = normalize(
        `${product.name} ${product.category} ${product.description}`,
      ).includes(normalizedQuery);
      const matchesCategory =
        category === "todos" ||
        normalize(product.category) === normalize(category);
      const matchesPrice =
        price === "todos" ||
        (price === "economico" && product.price < 60) ||
        (price === "medio" && product.price >= 60 && product.price < 120) ||
        (price === "premium" && product.price >= 120 && product.price < 200) ||
        (price === "ceremonial" && product.price >= 200);
      return matchesQuery && matchesCategory && matchesPrice;
    });
  },

  listFeaturedProducts() {
    return this.listProducts()
      .filter((product) => product.featured)
      .slice(0, 3);
  },

  listOrders() {
    return demoRepository.readState().orders.map(clone);
  },

  getOrder(id) {
    const order = demoRepository
      .readState()
      .orders.find((item) => item.id === id);
    return order ? clone(order) : null;
  },

  listCustomers() {
    return demoRepository.readState().customers.map((customer) => {
      const orders = demoRepository
        .readState()
        .orders.filter((order) => order.customerId === customer.id);
      return {
        ...clone(customer),
        orderCount: orders.length,
        spent: orders
          .filter((order) => order.status !== "cancelado")
          .reduce((sum, order) => sum + order.total, 0),
        lastOrder:
          orders.sort((a, b) => b.createdAt.localeCompare(a.createdAt))[0] ??
          null,
      };
    });
  },

  getCustomer(id) {
    const customer = demoRepository
      .readState()
      .customers.find((item) => item.id === id);
    return customer ? clone(customer) : null;
  },

  getCustomerByEmail(email) {
    const normalizedEmail = normalize(email);
    const customer = demoRepository
      .readState()
      .customers.find((item) => normalize(item.email) === normalizedEmail);
    return customer ? clone(customer) : null;
  },

  saveCustomer(customer) {
    if (
      !customer.name?.trim() ||
      !customer.email?.trim() ||
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(customer.email.trim())
    )
      throw new Error("Completa el nombre y el correo del cliente.");
    return updateState((state) => {
      const duplicateEmail = state.customers.some(
        (item) =>
          item.id !== customer.id &&
          normalize(item.email) === normalize(customer.email),
      );
      if (duplicateEmail)
        throw new Error("Ese correo ya pertenece a otro cliente.");
      const index = state.customers.findIndex(
        (item) => item.id === customer.id,
      );
      if (index < 0) throw new Error("No se encontró el cliente.");
      state.customers[index] = {
        ...state.customers[index],
        ...clone(customer),
      };
      return clone(state.customers[index]);
    });
  },

  getSettings() {
    return clone(demoRepository.readState().settings);
  },

  saveSettings(settings) {
    return updateState((state) => {
      state.settings = { ...state.settings, ...clone(settings) };
    });
  },

  saveProduct(product) {
    let imageUrl;
    try {
      imageUrl = new URL(product.image, location.href);
    } catch {
      throw new Error("La imagen debe ser una URL o ruta válida.");
    }
    if (
      !product.name?.trim() ||
      product.name.trim().length > 100 ||
      !product.sku?.trim() ||
      product.sku.trim().length > 40 ||
      String(product.description ?? "").length > 1200 ||
      !Number.isFinite(product.price) ||
      product.price < 0 ||
      product.price > 999999.99 ||
      !Number.isInteger(product.stock) ||
      product.stock < 0 ||
      product.stock > 999999 ||
      ![
        "Romántico",
        "Cumpleaños",
        "Temporada",
        "Premium",
        "Condolencias",
        "Personalizado",
      ].includes(product.category) ||
      !["http:", "https:"].includes(imageUrl.protocol)
    )
      throw new Error("Revisa el nombre, SKU, precio y stock del producto.");
    return updateState((state) => {
      const duplicateSku = state.products.some(
        (item) =>
          item.id !== product.id &&
          normalize(item.sku) === normalize(product.sku),
      );
      if (duplicateSku)
        throw new Error("Ese SKU ya pertenece a otro producto.");
      const index = state.products.findIndex((item) => item.id === product.id);
      if (index < 0) state.products.push(clone(product));
      else state.products[index] = clone(product);
      return clone(product);
    });
  },

  changeOrderStatus(orderId, status) {
    if (!validStatuses.has(status))
      throw new Error("El estado seleccionado no es válido.");
    return updateState((state) => {
      const order = state.orders.find((item) => item.id === orderId);
      if (!order) throw new Error("No se encontró el pedido.");
      if (order.status === status) return clone(order);
      if (status === "cancelado" && order.status !== "cancelado") {
        order.items.forEach((item) => {
          const product = state.products.find(
            (entry) => entry.id === item.productId,
          );
          if (product) product.stock += item.quantity;
        });
      } else if (order.status === "cancelado" && status !== "cancelado") {
        const products = order.items.map((item) => ({
          item,
          product: state.products.find((entry) => entry.id === item.productId),
        }));
        if (
          products.some(
            ({ item, product }) =>
              !product || !product.available || product.stock < item.quantity,
          )
        )
          throw new Error(
            "No hay stock suficiente para reactivar este pedido.",
          );
        products.forEach(({ item, product }) => {
          product.stock -= item.quantity;
        });
        if (order.payment.status === "Reembolso pendiente")
          order.payment.status = "Pagado";
      }
      order.status = status;
      order.history.push({ status, date: new Date().toISOString() });
      if (status === "cancelado" && order.payment.status === "Pagado")
        order.payment.status = "Reembolso pendiente";
      return clone(order);
    });
  },

  getCart() {
    return demoRepository
      .readCart()
      .flatMap((entry) => {
        const product = this.getProduct(entry.productId);
        if (
          !product ||
          !product.available ||
          !Number.isInteger(entry.quantity) ||
          entry.quantity < 1
        )
          return [];
        return [
          {
            productId: product.id,
            quantity: Math.min(entry.quantity, product.stock),
            product,
          },
        ];
      })
      .filter((entry) => entry.quantity > 0);
  },

  addToCart(productId, quantity = 1) {
    const product = this.getProduct(productId);
    if (!product || !product.available || product.stock < 1)
      throw new Error("Este producto no está disponible.");
    if (!Number.isInteger(quantity) || quantity < 1)
      throw new Error("Selecciona una cantidad válida.");
    const cart = this.getCart();
    const existing = cart.find((item) => item.productId === productId);
    const nextQuantity = quantity + (existing?.quantity ?? 0);
    if (nextQuantity > product.stock)
      throw new Error("La cantidad supera las unidades disponibles.");
    if (existing) existing.quantity = nextQuantity;
    else cart.push({ productId, quantity });
    if (
      !demoRepository.writeCart(
        cart.map(({ productId: id, quantity: count }) => ({
          productId: id,
          quantity: count,
        })),
      )
    )
      throw new Error("No se pudo guardar el carrito en este navegador.");
    return this.getCart();
  },

  updateCartItem(productId, quantity) {
    const cart = this.getCart();
    if (!Number.isInteger(quantity) || quantity < 0)
      throw new Error("La cantidad no es válida.");
    const product = this.getProduct(productId);
    if (!product) throw new Error("El producto ya no está disponible.");
    if (quantity > product.stock)
      throw new Error("La cantidad supera las unidades disponibles.");
    const next =
      quantity === 0
        ? cart.filter((item) => item.productId !== productId)
        : cart.map((item) =>
            item.productId === productId ? { ...item, quantity } : item,
          );
    if (
      !demoRepository.writeCart(
        next.map(({ productId: id, quantity: count }) => ({
          productId: id,
          quantity: count,
        })),
      )
    )
      throw new Error("No se pudo guardar el carrito.");
    return this.getCart();
  },

  clearCart() {
    if (!demoRepository.writeCart([]))
      throw new Error("No se pudo vaciar el carrito.");
  },

  cartSummary() {
    const items = this.getCart();
    const subtotal = items.reduce(
      (sum, item) => sum + item.product.price * item.quantity,
      0,
    );
    const deliveryFee =
      items.length === 0 || subtotal >= FREE_DELIVERY_LIMIT ? 0 : DELIVERY_FEE;
    return {
      items,
      subtotal,
      deliveryFee,
      total: subtotal + deliveryFee,
      count: items.reduce((sum, item) => sum + item.quantity, 0),
    };
  },

  createOrder({ customer, delivery }) {
    const summary = this.cartSummary();
    if (!summary.items.length)
      throw new Error("Agrega productos antes de confirmar el pedido.");
    if (!customer?.id || !customer.name?.trim() || !customer.email)
      throw new Error("Inicia sesión para asociar el pedido a tu cuenta.");
    if (!/^(?:\+?51\s?)?9(?:[\s-]?\d){8}$/.test(customer.phone ?? ""))
      throw new Error("Ingresa un número celular peruano válido de 9 dígitos.");
    const deliveryDate = new Date(`${delivery.date}T00:00:00`);
    const minimumDate = new Date();
    minimumDate.setHours(0, 0, 0, 0);
    minimumDate.setDate(minimumDate.getDate() + 1);
    if (
      !["Delivery a domicilio", "Recojo en tienda"].includes(delivery.mode) ||
      (delivery.mode !== "Recojo en tienda" && !delivery.address?.trim()) ||
      Number.isNaN(deliveryDate.valueOf()) ||
      deliveryDate < minimumDate ||
      !/^([01]\d|2[0-3]):[0-5]\d$/.test(delivery.time ?? "")
    )
      throw new Error("Completa la dirección, fecha y hora de entrega.");
    const deliveryFee =
      delivery.mode === "Recojo en tienda" ? 0 : summary.deliveryFee;
    return updateState((state) => {
      if (!state.customers.some((item) => item.id === customer.id))
        throw new Error("No se encontró la cuenta para asociar el pedido.");
      for (const item of summary.items) {
        const current = state.products.find(
          (product) => product.id === item.productId,
        );
        if (!current || !current.available || current.stock < item.quantity)
          throw new Error(
            `El stock de ${item.product.name} cambió. Actualiza el carrito antes de continuar.`,
          );
      }
      for (const item of summary.items)
        state.products.find((product) => product.id === item.productId).stock -=
          item.quantity;
      const nextNumber =
        Math.max(
          52,
          ...state.orders.map((order) =>
            Number(order.id.match(/(\d+)$/)?.[1] ?? 0),
          ),
        ) + 1;
      const order = {
        id: `OP-${new Date().getFullYear()}-${String(nextNumber).padStart(4, "0")}`,
        customerId: customer.id,
        customerName: customer.name,
        email: customer.email,
        phone: customer.phone,
        delivery: clone(delivery),
        payment: {
          method: delivery.paymentMethod || "Yape",
          status: "Pendiente",
        },
        subtotal: summary.subtotal,
        deliveryFee,
        total: summary.subtotal + deliveryFee,
        status: "pendiente",
        createdAt: new Date().toISOString(),
        items: summary.items.map(({ product, quantity }) => ({
          productId: product.id,
          name: product.name,
          category: product.category,
          image: product.image,
          quantity,
          unitPrice: product.price,
          lineTotal: product.price * quantity,
        })),
        history: [{ status: "pendiente", date: new Date().toISOString() }],
      };
      state.orders.unshift(order);
      return clone(order);
    });
  },

  removeOrderFromCart() {
    this.clearCart();
  },
});

export { DELIVERY_FEE, FREE_DELIVERY_LIMIT };
