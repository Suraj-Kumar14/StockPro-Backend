document.addEventListener("DOMContentLoaded", () => {
    const loader = document.querySelector("[data-loader]");

    const showLoader = () => {
        if (loader) {
            loader.hidden = false;
        }
    };

    if (loader) {
        loader.hidden = true;
    }

    document.querySelectorAll("form[data-loading='true'], form:not([data-loading='false'])").forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (!form.checkValidity()) {
                form.classList.add("was-validated");
                return;
            }
            showLoader();
        });
    });

    document.querySelectorAll("[data-confirm]").forEach((element) => {
        element.addEventListener("click", (event) => {
            const message = element.getAttribute("data-confirm");
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("[data-page-target]").forEach((button) => {
        button.addEventListener("click", () => {
            if (button.disabled) {
                return;
            }
            const target = button.getAttribute("data-page-target");
            const url = new URL(window.location.href);
            url.searchParams.set("page", target);
            window.location.assign(url.toString());
        });
    });

    document.querySelectorAll("[data-auto-submit='true']").forEach((field) => {
        field.addEventListener("change", () => {
            if (field.form) {
                field.form.submit();
            }
        });
    });
});
