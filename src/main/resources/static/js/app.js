import { initializeCatalogFilters } from "./ui/catalog-filters.js";
import { initializeCheckoutWizard } from "./ui/checkout-wizard.js";
import { initializeMobileNavigation } from "./ui/mobile-navigation.js";
import { initializePasswordVisibility } from "./ui/password-visibility.js";
import { initializePrintAction } from "./ui/print-action.js";
import { initializeRegistrationValidation } from "./ui/registration-validation.js";

initializeCatalogFilters(document);
initializeCheckoutWizard(document);
initializeMobileNavigation(document);
initializePasswordVisibility(document);
initializePrintAction(document);
initializeRegistrationValidation(document);
