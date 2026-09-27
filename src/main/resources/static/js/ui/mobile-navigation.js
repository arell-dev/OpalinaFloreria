const NAVBAR_SELECTOR = ".navbar-opalina";
const OFFCANVAS_SELECTOR = ".navbar-mobile-panel";
const OPEN_NAVBAR_CLASS = "navbar-opalina--menu-open";

export function initializeMobileNavigation(root = document) {
  const navbars = root.querySelectorAll(NAVBAR_SELECTOR);

  navbars.forEach((navbar) => {
    const offcanvas = navbar.querySelector(OFFCANVAS_SELECTOR);

    if (!offcanvas) {
      return;
    }

    offcanvas.addEventListener("show.bs.offcanvas", () => {
      navbar.classList.add(OPEN_NAVBAR_CLASS);
    });

    offcanvas.addEventListener("hidden.bs.offcanvas", () => {
      navbar.classList.remove(OPEN_NAVBAR_CLASS);
    });
  });
}
