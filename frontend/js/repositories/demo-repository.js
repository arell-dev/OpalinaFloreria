import { seedCustomers, seedOrders, seedProducts, seedSettings } from "../data/seed.js";

const STORAGE_KEY = "opalina-demo-state-v3";
const CART_KEY = "opalina-cart-v3";
const clone = (value) => structuredClone(value);

function freshState() {
  return {
    products: clone(seedProducts),
    customers: clone(seedCustomers),
    orders: clone(seedOrders),
    settings: clone(seedSettings),
  };
}

function isState(value) {
  return (
    value &&
    Array.isArray(value.products) &&
    value.products.every(
      (product) =>
        product &&
        typeof product.id === "string" &&
        typeof product.name === "string" &&
        typeof product.price === "number" &&
        Number.isInteger(product.stock),
    ) &&
    Array.isArray(value.customers) &&
    value.customers.every(
      (customer) =>
        customer &&
        typeof customer.id === "string" &&
        typeof customer.name === "string" &&
        typeof customer.email === "string",
    ) &&
    Array.isArray(value.orders) &&
    value.orders.every(
      (order) =>
        order &&
        typeof order.id === "string" &&
        typeof order.customerId === "string" &&
        Array.isArray(order.items),
    ) &&
    value.settings &&
    typeof value.settings === "object"
  );
}

function readStorage(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw === null ? clone(fallback) : JSON.parse(raw);
  } catch {
    return clone(fallback);
  }
}

export const demoRepository = Object.freeze({
  readState() {
    const stored = readStorage(STORAGE_KEY, freshState());
    if (!isState(stored)) return freshState();
    return stored;
  },

  writeState(state) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
      return true;
    } catch {
      return false;
    }
  },

  readCart() {
    const state = readStorage(CART_KEY, []);
    return Array.isArray(state) ? state : [];
  },

  writeCart(cart) {
    try {
      localStorage.setItem(CART_KEY, JSON.stringify(cart));
      return true;
    } catch {
      return false;
    }
  },

  reset() {
    try {
      localStorage.removeItem(STORAGE_KEY);
      localStorage.removeItem(CART_KEY);
      return true;
    } catch {
      return false;
    }
  },
});

export { STORAGE_KEY, CART_KEY };
