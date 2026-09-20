// Scales the window to the viewport and drives the scroll bar, which uses the game's
// button texture rather than the browser's.

(function () {
    "use strict";

    const WINDOW = 256;
    const TABS = 32;
    const TAB_SHIFT = 20;
    const MARGIN = 8;
    const MAX_SCALE = 4;

    const root = document.documentElement;
    const view = document.getElementById("document");
    const bar = document.getElementById("scrollbar");
    const thumb = document.getElementById("thumb");

    function unit() {
        const byWidth = (window.innerWidth - 2 * MARGIN) / (WINDOW + TABS + TAB_SHIFT);
        const byHeight = (window.innerHeight - 2 * MARGIN) / WINDOW;
        return Math.max(1, Math.min(MAX_SCALE, Math.floor(Math.min(byWidth, byHeight))));
    }

    function rescale() {
        root.style.setProperty("--u", unit() + "px");
        update();
    }

    function range() {
        return Math.max(0, view.scrollHeight - view.clientHeight);
    }

    function travel() {
        return bar.clientHeight - thumb.offsetHeight;
    }

    function update() {
        const scrollable = range();
        bar.classList.toggle("idle", scrollable <= 0);
        if (scrollable <= 0) {
            thumb.style.top = "0px";
            return;
        }
        const progress = view.scrollTop / scrollable;
        thumb.style.top = Math.round(progress * travel()) + "px";
        thumb.title = Math.round(progress * 100) + "%";
    }

    function scrollToPointer(event) {
        const offset = event.clientY - bar.getBoundingClientRect().top - thumb.offsetHeight / 2;
        const progress = travel() > 0 ? offset / travel() : 0;
        view.scrollTop = Math.max(0, Math.min(1, progress)) * range();
    }

    view.addEventListener("scroll", update, {passive: true});
    window.addEventListener("resize", rescale);

    bar.addEventListener("pointerdown", function (event) {
        bar.setPointerCapture(event.pointerId);
        bar.classList.add("dragging");
        scrollToPointer(event);
        event.preventDefault();
    });

    bar.addEventListener("pointermove", function (event) {
        if (bar.classList.contains("dragging")) {
            scrollToPointer(event);
        }
    });

    ["pointerup", "pointercancel"].forEach(function (name) {
        bar.addEventListener(name, function () {
            bar.classList.remove("dragging");
        });
    });

    rescale();
    new ResizeObserver(update).observe(view.firstElementChild);
    document.fonts.ready.then(update);
})();
