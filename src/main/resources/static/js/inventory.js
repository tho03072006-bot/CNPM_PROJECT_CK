document.addEventListener("DOMContentLoaded", () => {
    const controls = document.querySelector("[data-inventory-controls]");
    const items = [...document.querySelectorAll("[data-stock-item]")];
    if (controls && items.length) {
        controls.hidden = false;
        const search = document.getElementById("inventory-search");
        const filters = [...controls.querySelectorAll("[data-stock-filter]")];
        const count = document.getElementById("inventory-result-count");
        const empty = document.getElementById("inventory-no-results");
        let level = "ALL";

        const normalize = value => value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
        const render = () => {
            const term = normalize(search.value.trim());
            let visible = 0;
            items.forEach(item => {
                const matchesText = normalize(item.dataset.search || "").includes(term);
                const matchesLevel = level === "ALL" || item.dataset.stockLevel === level;
                item.hidden = !(matchesText && matchesLevel);
                if (!item.hidden) visible++;
            });
            count.textContent = `${visible} món`;
            empty.hidden = visible !== 0;
        };

        search.addEventListener("input", render);
        filters.forEach(button => button.addEventListener("click", () => {
            level = button.dataset.stockFilter;
            filters.forEach(filter => {
                const selected = filter === button;
                filter.classList.toggle("is-selected", selected);
                filter.setAttribute("aria-pressed", String(selected));
            });
            render();
        }));
        document.getElementById("inventory-clear-filters")?.addEventListener("click", () => {
            search.value = "";
            filters[0].click();
            search.focus();
        });
    }

    const form = document.getElementById("stock-adjust-form");
    if (!form) return;
    const stock = Number(document.getElementById("current-stock")?.dataset.stock || 0);
    const quantity = document.getElementById("adjust-quantity");
    const quantityLabel = document.getElementById("adjust-quantity-label");
    const noteLabel = document.getElementById("adjust-note-label");
    const note = document.getElementById("adjust-note");
    const preview = document.getElementById("stock-preview");
    const previewBox = preview.closest(".inventory-preview");
    const submit = document.getElementById("stock-submit");
    const labels = {
        IMPORT: ["2. Số phần nhập thêm", "3. Ghi chú (không bắt buộc)", false, "Xác nhận nhập kho"],
        WRITE_OFF: ["2. Số phần xuất hủy", "3. Lý do xuất hủy (bắt buộc)", true, "Xác nhận xuất hủy"],
        STOCKTAKE: ["2. Số đếm được thực tế", "3. Ghi chú kiểm kê (không bắt buộc)", false, "Lưu kết quả kiểm kê"]
    };
    const selectedType = () => form.querySelector('input[name="type"]:checked')?.value || "IMPORT";
    const update = () => {
        const type = selectedType();
        const [quantityText, noteText, noteRequired, submitText] = labels[type];
        quantityLabel.textContent = quantityText;
        noteLabel.textContent = noteText;
        note.required = noteRequired;
        quantity.min = type === "STOCKTAKE" ? 0 : 1;
        quantity.max = type === "WRITE_OFF" ? Math.max(stock, 1) : (type === "STOCKTAKE" ? 100000 : 10000);
        const value = quantity.value === "" ? null : Number(quantity.value);
        let after = stock;
        if (value !== null && Number.isFinite(value)) {
            after = type === "IMPORT" ? stock + value : (type === "WRITE_OFF" ? stock - value : value);
        }
        preview.textContent = after.toLocaleString("vi-VN");
        previewBox.classList.toggle("is-invalid", after < 0 || after > 100000);
        submit.textContent = submitText;
        submit.classList.toggle("inventory-submit-danger", type === "WRITE_OFF");
    };
    form.addEventListener("change", update);
    quantity.addEventListener("input", update);
    form.querySelectorAll("[data-quick-quantity]").forEach(button => button.addEventListener("click", () => {
        quantity.value = button.dataset.quickQuantity;
        quantity.focus();
        update();
    }));
    update();
});
