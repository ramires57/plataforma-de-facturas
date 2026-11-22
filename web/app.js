const form = document.querySelector("#invoice-form");
const clientInput = document.querySelector("#client-name");
const productInput = document.querySelector("#product");
const productSearch = document.querySelector("#product-search");
const productPicker = document.querySelector("#product-picker");
const productOptionsList = document.querySelector("#product-options");
const productSelection = document.querySelector("#product-selection");
const componentOptions = [...productInput.options].filter((option) => option.value);
const quantityInput = document.querySelector("#quantity");
const message = document.querySelector("#form-message");
const submitButton = document.querySelector("#submit-button");
const clearButton = document.querySelector("#clear-button");
const printButton = document.querySelector("#print-button");
const downloadPdfButton = document.querySelector("#download-pdf-button");
const priceIndicator = document.querySelector("#price-indicator");
const priceRows = [...document.querySelectorAll(".price-row")];
const emptyInvoice = document.querySelector("#empty-invoice");
const invoiceContent = document.querySelector("#invoice-content");
let lastInvoice = null;
let visibleProducts = [];
let activeProductIndex = -1;

const money = (amount) => `S/ ${Number(amount).toLocaleString("es-PE", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})}`;

function normalizeText(value) {
  return value.normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");
}

function closeProductList() {
  productOptionsList.hidden = true;
  productSearch.setAttribute("aria-expanded", "false");
  productSearch.removeAttribute("aria-activedescendant");
  activeProductIndex = -1;
}

function setActiveProduct(index) {
  activeProductIndex = index;
  const options = [...productOptionsList.querySelectorAll('[role="option"]')];
  options.forEach((option, optionIndex) => {
    option.classList.toggle("is-active", optionIndex === index);
  });
  if (index >= 0 && options[index]) {
    productSearch.setAttribute("aria-activedescendant", options[index].id);
  } else {
    productSearch.removeAttribute("aria-activedescendant");
  }
}

function renderProductOptions(query) {
  const term = normalizeText(query.trim());
  visibleProducts = componentOptions
    .map((option, index) => ({ option, index }))
    .filter(({ option }) => normalizeText(option.textContent).includes(term));
  productOptionsList.replaceChildren();

  if (visibleProducts.length === 0) {
    const emptyState = document.createElement("div");
    emptyState.className = "product-empty";
    emptyState.setAttribute("role", "status");
    emptyState.textContent = "No se encontraron componentes.";
    productOptionsList.append(emptyState);
  } else {
    visibleProducts.forEach(({ option, index }) => {
      const item = document.createElement("div");
      item.className = "product-option";
      item.id = `product-option-${index}`;
      item.setAttribute("role", "option");
      item.setAttribute("aria-selected", String(option.value === productInput.value));
      item.dataset.productValue = option.value;
      item.dataset.index = String(index);

      const number = document.createElement("span");
      number.className = "product-option-index";
      number.textContent = String(index + 1).padStart(2, "0");
      const name = document.createElement("span");
      name.className = "product-option-name";
      name.textContent = option.textContent;
      const marker = document.createElement("span");
      marker.className = "product-option-marker";
      marker.setAttribute("aria-hidden", "true");
      marker.textContent = option.value === productInput.value ? "SELECCIONADO" : "";
      item.append(number, name, marker);
      productOptionsList.append(item);
    });
  }

  productOptionsList.hidden = false;
  productSearch.setAttribute("aria-expanded", "true");
  setActiveProduct(-1);
}

function chooseProduct(value) {
  const option = componentOptions.find((item) => item.value === value);
  if (!option) {
    return;
  }
  productInput.value = option.value;
  productSearch.value = option.textContent;
  productSelection.textContent =
    `Seleccionado: ${option.textContent}. El precio depende del volumen.`;
  productInput.dispatchEvent(new Event("change", { bubbles: true }));
  closeProductList();
}

productSearch.addEventListener("focus", () => {
  if (productInput.value && productSearch.value === productInput.value) {
    productSearch.select();
    renderProductOptions("");
  } else {
    renderProductOptions(productSearch.value);
  }
});

productSearch.addEventListener("input", () => {
  if (productSearch.value !== productInput.value) {
    productInput.value = "";
    productSelection.textContent =
      "Busca y selecciona el componente que compra el cliente.";
  }
  renderProductOptions(productSearch.value);
});

productSearch.addEventListener("keydown", (event) => {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    if (productOptionsList.hidden) {
      renderProductOptions(productSearch.value);
    }
    if (visibleProducts.length > 0) {
      setActiveProduct(Math.min(activeProductIndex + 1, visibleProducts.length - 1));
    }
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    if (productOptionsList.hidden) {
      renderProductOptions(productSearch.value);
    }
    if (visibleProducts.length > 0) {
      setActiveProduct(activeProductIndex < 0
        ? visibleProducts.length - 1
        : Math.max(activeProductIndex - 1, 0));
    }
  } else if (event.key === "Enter" && !productOptionsList.hidden) {
    event.preventDefault();
    if (activeProductIndex >= 0) {
      chooseProduct(visibleProducts[activeProductIndex].option.value);
    } else {
      const exactMatch = visibleProducts.find(
        ({ option }) => normalizeText(option.textContent) === normalizeText(productSearch.value.trim()),
      );
      if (exactMatch) {
        chooseProduct(exactMatch.option.value);
      }
    }
  } else if (event.key === "Escape" && !productOptionsList.hidden) {
    productSearch.value = productInput.value;
    closeProductList();
  }
});

productOptionsList.addEventListener("mousedown", (event) => {
  event.preventDefault();
});

productOptionsList.addEventListener("mousemove", (event) => {
  const item = event.target.closest("[data-index]");
  if (item) {
    setActiveProduct(visibleProducts.findIndex(
      ({ index }) => index === Number(item.dataset.index),
    ));
  }
});

productOptionsList.addEventListener("click", (event) => {
  const item = event.target.closest("[data-product-value]");
  if (item) {
    chooseProduct(item.dataset.productValue);
  }
});

document.addEventListener("pointerdown", (event) => {
  if (!productPicker.contains(event.target)) {
    closeProductList();
  }
});

function updatePriceBand() {
  const quantity = Number.parseInt(quantityInput.value, 10);
  const activeRow = priceRows.find((row) => {
    const min = Number(row.dataset.min);
    const max = row.dataset.max ? Number(row.dataset.max) : Number.POSITIVE_INFINITY;
    return Number.isInteger(quantity) && quantity >= min && quantity <= max;
  });

  priceRows.forEach((row) => row.classList.toggle("is-active", row === activeRow));
  if (!activeRow) {
    priceIndicator.textContent = "El precio unitario se asigna según el volumen.";
    return;
  }

  const price = activeRow.querySelector("strong").textContent;
  const discount = activeRow.querySelector("span:last-child").textContent;
  priceIndicator.textContent = `${price} por unidad · ${discount} de descuento`;
}

function showInvoice(invoice) {
  closeProductList();
  lastInvoice = invoice;
  document.querySelector("#invoice-code").textContent =
    `SEN / ${new Date().toLocaleDateString("es-PE").replaceAll("/", ".")}`;
  document.querySelector("#invoice-client").textContent = invoice.nombre_cliente;
  document.querySelector("#invoice-product").textContent = invoice.producto;
  document.querySelector("#invoice-units").textContent =
    `${invoice.cantidad.toLocaleString("es-PE")} UNIDADES`;
  document.querySelector("#unit-price").textContent = money(invoice.precio_unitario);
  document.querySelector("#purchase-amount").textContent = money(invoice.importe_compra);
  document.querySelector("#discount").textContent = `− ${money(invoice.descuento)}`;
  document.querySelector("#subtotal").textContent = money(invoice.subtotal);
  document.querySelector("#igv").textContent = money(invoice.igv);
  document.querySelector("#total").textContent = money(invoice.total_pagar);
  emptyInvoice.hidden = true;
  invoiceContent.hidden = false;
  message.textContent = "Factura generada correctamente.";
  message.classList.add("is-success");
  if (window.matchMedia("(max-width: 780px)").matches) {
    const behavior = window.matchMedia("(prefers-reduced-motion: reduce)").matches
      ? "auto"
      : "smooth";
    invoiceContent.scrollIntoView({ behavior, block: "center" });
  }
}

quantityInput.addEventListener("input", updatePriceBand);

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  message.classList.remove("is-success");
  message.textContent = "";

  const nombre = clientInput.value.trim();
  const cantidad = Number(quantityInput.value);
  if (!nombre) {
    message.textContent = "Ingresa el nombre del cliente.";
    clientInput.focus();
    return;
  }
  if (!productInput.value) {
    message.textContent = "Selecciona el componente que compra el cliente.";
    productSearch.focus();
    return;
  }
  if (!Number.isSafeInteger(cantidad) || cantidad <= 0) {
    message.textContent = "La cantidad debe ser un entero positivo.";
    quantityInput.focus();
    return;
  }

  submitButton.disabled = true;
  submitButton.querySelector("span").textContent = "Calculando…";
  form.setAttribute("aria-busy", "true");

  try {
    const body = {
      nombre_cliente: nombre,
      producto: productInput.value,
      cantidad,
    };
    const response = await fetch("/api/factura", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.error || "No fue posible calcular la factura.");
    }
    showInvoice(result);
  } catch (error) {
    message.classList.remove("is-success");
    message.textContent = error instanceof TypeError
      ? "No se pudo conectar con el servicio. Verifica que el servidor esté activo."
      : error.message;
  } finally {
    submitButton.disabled = false;
    submitButton.querySelector("span").textContent = "Generar factura";
    form.removeAttribute("aria-busy");
  }
});

clearButton.addEventListener("click", () => {
  form.reset();
  productSearch.value = "";
  closeProductList();
  productSelection.textContent = "Busca y selecciona el componente que compra el cliente.";
  message.textContent = "";
  message.classList.remove("is-success");
  lastInvoice = null;
  invoiceContent.hidden = true;
  emptyInvoice.hidden = false;
  document.querySelector("#invoice-code").textContent = "SEN / —";
  updatePriceBand();
  clientInput.focus();
});

printButton.addEventListener("click", () => window.print());

downloadPdfButton.addEventListener("click", async () => {
  if (!lastInvoice) {
    return;
  }

  downloadPdfButton.disabled = true;
  message.classList.remove("is-success");
  message.textContent = "Preparando el PDF…";

  try {
    const response = await fetch("/api/factura/pdf", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        nombre_cliente: lastInvoice.nombre_cliente,
        producto: lastInvoice.producto,
        cantidad: lastInvoice.cantidad,
      }),
    });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || "No fue posible generar el PDF.");
    }

    const pdf = await response.blob();
    if (pdf.type !== "application/pdf") {
      throw new Error("El servidor no devolvió un archivo PDF válido.");
    }
    const url = URL.createObjectURL(pdf);
    const link = document.createElement("a");
    link.href = url;
    link.download = "factura-senati.pdf";
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    message.textContent = "PDF descargado correctamente.";
    message.classList.add("is-success");
  } catch (error) {
    message.textContent = error instanceof TypeError
      ? "No se pudo conectar con el servicio para generar el PDF."
      : error.message;
  } finally {
    downloadPdfButton.disabled = false;
  }
});
