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
const addLineButton = document.querySelector("#add-line-button");
const cartLines = document.querySelector("#cart-lines");
const cartEmpty = document.querySelector("#cart-empty");
const cartCount = document.querySelector("#cart-count");
const invoiceCartLines = document.querySelector("#invoice-cart-lines");
const priceIndicator = document.querySelector("#price-indicator");
const priceRows = [...document.querySelectorAll(".price-row")];
const emptyInvoice = document.querySelector("#empty-invoice");
const invoiceContent = document.querySelector("#invoice-content");
let lastInvoice = null;
let visibleProducts = [];
let activeProductIndex = -1;
let cart = [];

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

  const adjustment = activeRow.querySelector("strong").textContent;
  const discount = activeRow.querySelector("span:last-child").textContent;
  const selectedProduct = productInput.value
    ? ` · ${productInput.value}`
    : "";
  priceIndicator.textContent = `${adjustment} sobre precio referencial${selectedProduct} · ${discount} descuento`;
}

function renderCart() {
  cartCount.textContent =
    `${cart.length} ${cart.length === 1 ? "línea" : "líneas"}`;
  cartLines.replaceChildren();
  if (cart.length === 0) {
    const empty = document.createElement("p");
    empty.className = "cart-empty";
    empty.id = "cart-empty";
    empty.textContent = "Aún no hay componentes agregados.";
    cartLines.append(empty);
  } else {
    cart.forEach((line, index) => {
      const item = document.createElement("div");
      item.className = "cart-line";
      const name = document.createElement("div");
      name.className = "cart-line-name";
      name.innerHTML = `<strong></strong><span></span>`;
      name.querySelector("strong").textContent = line.producto;
      name.querySelector("span").textContent = `${line.cantidad} UNIDADES`;
      const total = document.createElement("strong");
      total.className = "cart-line-total";
      total.textContent = `${line.cantidad} UNID.`;
      const remove = document.createElement("button");
      remove.type = "button";
      remove.className = "remove-line";
      remove.dataset.index = String(index);
      remove.setAttribute("aria-label", `Eliminar ${line.producto}`);
      remove.textContent = "×";
      item.append(name, total, remove);
      cartLines.append(item);
    });
  }
}

addLineButton.addEventListener("click", () => {
  message.classList.remove("is-success");
  const producto = productInput.value;
  const cantidad = Number(quantityInput.value);
  if (!producto) {
    message.textContent = "Selecciona el componente que quieres agregar.";
    productSearch.focus();
    return;
  }
  if (!Number.isSafeInteger(cantidad) || cantidad <= 0) {
    message.textContent = "Ingresa una cantidad entera positiva.";
    quantityInput.focus();
    return;
  }
  if (cart.some((line) => line.producto === producto)) {
    message.textContent = "Ese componente ya está agregado. Elimínalo para cambiarlo.";
    return;
  }

  cart.push({
    producto,
    cantidad,
  });
  productInput.value = "";
  productSearch.value = "";
  quantityInput.value = "";
  productSelection.textContent =
    "Busca y agrega otro componente a la factura.";
  updatePriceBand();
  renderCart();
  message.classList.remove("is-success");
  message.textContent = "Componente agregado a la factura.";
  productSearch.focus();
});

cartLines.addEventListener("click", (event) => {
  const remove = event.target.closest(".remove-line");
  if (!remove) {
    return;
  }
  cart.splice(Number(remove.dataset.index), 1);
  renderCart();
  message.textContent = "Componente eliminado.";
});

function showInvoice(invoice) {
  closeProductList();
  lastInvoice = invoice;
  document.querySelector("#invoice-code").textContent =
    `SEN / ${new Date().toLocaleDateString("es-PE").replaceAll("/", ".")}`;
  document.querySelector("#invoice-client").textContent = invoice.nombre_cliente;
  if (invoice.lineas) {
    document.querySelector("#invoice-lines-heading").hidden = false;
    document.querySelector("#invoice-product").textContent =
      `${invoice.lineas.length} ${invoice.lineas.length === 1 ? "componente" : "componentes"}`;
    document.querySelector("#invoice-referential").textContent =
      `${invoice.cantidad_total} unidades en total`;
    invoiceCartLines.replaceChildren();
    invoice.lineas.forEach((line) => {
      const row = document.createElement("div");
      row.className = "invoice-cart-line";
      const name = document.createElement("span");
      name.textContent = `${line.cantidad} × ${line.producto}`;
      const amount = document.createElement("strong");
      amount.textContent = money(line.importe_compra);
      row.append(name, amount);
      invoiceCartLines.append(row);
    });
  } else {
    document.querySelector("#invoice-lines-heading").hidden = true;
    document.querySelector("#invoice-product").textContent = invoice.producto;
    document.querySelector("#invoice-referential").textContent =
      `PRECIO REFERENCIAL S/ ${Number(invoice.precio_referencial).toLocaleString("es-PE", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })}`;
    invoiceCartLines.replaceChildren();
  }
  document.querySelector("#invoice-units").textContent =
    invoice.cantidad_total
      ? `${invoice.cantidad_total.toLocaleString("es-PE")} UNIDADES`
      : `${invoice.cantidad.toLocaleString("es-PE")} UNIDADES`;
  document.querySelector("#unit-price").textContent =
    invoice.lineas ? "VARIOS" : money(invoice.precio_unitario);
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
  if (!nombre) {
    message.textContent = "Ingresa el nombre del cliente.";
    clientInput.focus();
    return;
  }
  if (cart.length === 0) {
    message.textContent =
      "Agrega al menos un componente antes de generar la factura.";
    productSearch.focus();
    return;
  }

  submitButton.disabled = true;
  submitButton.querySelector("span").textContent = "Calculando…";
  form.setAttribute("aria-busy", "true");

  try {
    const body = {
      nombre_cliente: nombre,
      lineas: cart.map((line) => ({
        producto: line.producto,
        cantidad: line.cantidad,
      })),
    };
    const response = await fetch("/api/factura/multiple", {
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
  cart = [];
  renderCart();
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
    const response = await fetch(
      lastInvoice.lineas
        ? "/api/factura/multiple/pdf"
        : "/api/factura/pdf",
      {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(lastInvoice.lineas
        ? {
            nombre_cliente: lastInvoice.nombre_cliente,
            lineas: lastInvoice.lineas.map((line) => ({
              producto: line.producto,
              cantidad: line.cantidad,
            })),
          }
        : {
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
