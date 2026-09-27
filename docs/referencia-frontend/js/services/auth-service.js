import { storeService } from "./store-service.js";

const SESSION_KEY = "opalina-demo-session-v1";
const DEMO_ACCOUNTS = Object.freeze([
  {
    email: "maria.lopez@opalina.test",
    password: "Cliente2026!",
    role: "cliente",
    customerId: "cliente-maria",
    name: "María López",
  },
  {
    email: "admin@opalina.test",
    password: "Admin2026!",
    role: "admin",
    customerId: null,
    name: "Administración Opalina",
  },
]);

function readSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(SESSION_KEY) || "null");
    return session && ["cliente", "admin"].includes(session.role)
      ? session
      : null;
  } catch {
    return null;
  }
}

export const authService = Object.freeze({
  current() {
    const session = readSession();
    if (!session?.customerId) return session;
    const customer = storeService.getCustomer(session.customerId);
    return customer
      ? { ...session, name: customer.name, email: customer.email }
      : session;
  },

  login(email, password) {
    const normalizedEmail = email.trim().toLowerCase();
    const account = DEMO_ACCOUNTS.find((item) => {
      if (item.password !== password) return false;
      const currentEmail = item.customerId
        ? storeService.getCustomer(item.customerId)?.email
        : item.email;
      return [item.email, currentEmail].some(
        (candidate) => candidate?.toLowerCase() === normalizedEmail,
      );
    });
    if (!account)
      throw new Error(
        "Correo o contraseña incorrectos. Revisa tus datos e inténtalo de nuevo.",
      );
    const customer = account.customerId
      ? storeService.getCustomer(account.customerId)
      : null;
    const session = {
      email: account.email,
      role: account.role,
      customerId: account.customerId,
      name: customer?.name ?? account.name,
      createdAt: new Date().toISOString(),
    };
    try {
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {
      throw new Error("El navegador no permite iniciar una sesión temporal.");
    }
    return session;
  },

  logout() {
    try {
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      // La sesión solo existe en este navegador; no hay credenciales almacenadas.
    }
  },

  customer() {
    const session = readSession();
    return session?.customerId
      ? storeService.getCustomer(session.customerId)
      : null;
  },

  canAccess(role) {
    const session = readSession();
    return role === "cliente"
      ? session?.role === "cliente"
      : session?.role === role;
  },

  demoAccounts() {
    return DEMO_ACCOUNTS.map(({ email, role }) => ({ email, role }));
  },
});

export { SESSION_KEY };
