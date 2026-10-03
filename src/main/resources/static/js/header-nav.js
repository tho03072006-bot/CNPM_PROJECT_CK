// Gom các liên kết không đủ chỗ vào nút mũi tên; giữ nguyên quyền, nhãn và badge.
(() => {
    'use strict';
    const nav = document.querySelector('.header-primary-nav');
    if (!nav) return;
    const more = nav.querySelector('.nav-more');
    const menu = more.querySelector('.nav-more-menu');
    const summary = more.querySelector('summary');
    const links = [...nav.children].filter(node => node.tagName === 'A');
    let scheduled = false;

    function positionMenu() {
        if (!more.open) return;
        const left = more.getBoundingClientRect().left;
        const width = menu.getBoundingClientRect().width;
        const clamped = Math.max(8, Math.min(left, window.innerWidth - width - 8));
        menu.style.left = (clamped - left) + 'px';
        menu.style.right = 'auto';
    }
    more.addEventListener('toggle', positionMenu);

    function arrange() {
        scheduled = false;
        links.forEach(link => nav.insertBefore(link, more));
        more.hidden = true;
        const gap = parseFloat(getComputedStyle(nav).columnGap) || 0;
        const widths = links.map(link => link.getBoundingClientRect().width);
        const available = nav.clientWidth;
        const allWidth = widths.reduce((sum, width) => sum + width, 0) + gap * Math.max(0, links.length - 1);
        if (allWidth > available) {
            more.hidden = false;
            let used = summary.getBoundingClientRect().width;
            let visibleCount = 0;
            for (const width of widths) {
                if (used + width + gap > available) break;
                used += width + gap;
                visibleCount++;
            }
            links.slice(visibleCount).forEach(link => menu.appendChild(link));
            const active = menu.querySelector('[aria-current="page"]');
            summary.classList.toggle('is-active', !!active);
            summary.setAttribute('aria-label', active ? 'Các danh mục khác, đang xem ' + active.textContent.trim() : 'Hiện các danh mục khác');
        } else {
            more.open = false;
            summary.classList.remove('is-active');
        }
        nav.classList.add('nav-overflow-ready');
        positionMenu();
    }
    function schedule() {
        if (!scheduled) { scheduled = true; requestAnimationFrame(arrange); }
    }
    if (typeof ResizeObserver === 'function') new ResizeObserver(schedule).observe(nav);
    window.addEventListener('resize', schedule);
    if (document.fonts) document.fonts.ready.then(schedule);
    schedule();
})();
