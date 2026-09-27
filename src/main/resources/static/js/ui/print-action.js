export function initializePrintAction(root = document) {
  root.addEventListener("click", (event) => {
    const button = event.target.closest("[data-print-order]");
    if (!button || !root.contains(button)) return;

    window.print();
  });
}
