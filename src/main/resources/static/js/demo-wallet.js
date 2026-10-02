/* © 2026 Nhóm 8. MoMo giả lập — không phát sinh tiền thật. */
(() => {
    'use strict';
    const root = document.getElementById('demo-wallet');
    if (!root) return;
    const el = id => document.getElementById(id);
    const wallet = root.dataset.role === 'wallet';
    const publicId = root.dataset.paymentId;
    const validId = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/;
    const validToken = /^[A-Za-z0-9_-]{43}$/;
    const states = new Set(['PENDING', 'SUCCESS', 'CANCELLED', 'EXPIRED', 'INVALIDATED']);
    const money = window.CinemaMoney;
    let token = null, current = null, reliable = false, busy = false, syncing = false, revision = 0;
    let sampledAt = 0, remainingAtSample = 0, expiryAsked = false;
    let pendingTimer = null, actionError = null;
    function show(text, success = false) {
        el('wallet-message').textContent = text;
        el('wallet-message').className = success ? 'alert alert-success' : 'alert alert-error';
        el('wallet-message').hidden = false;
    }
    function remaining() { return Math.max(0, remainingAtSample - (performance.now() - sampledAt)); }
    function render() {
        if (!current) return;
        const seconds = Math.ceil(remaining() / 1000);
        el('wallet-countdown').textContent = current.status !== 'PENDING' ? 'Đã kết thúc'
            : seconds > 0 ? String(Math.floor(seconds / 60)).padStart(2, '0') + ':' + String(seconds % 60).padStart(2, '0')
            : 'Đang xác minh hết hạn…';
        el('wallet-status').textContent = reliable ? current.message : 'Đang xác minh trạng thái thanh toán…';
        if (wallet) {
            const pending = current.status === 'PENDING';
            el('wallet-actions').hidden = !pending;
            el('wallet-confirm').disabled = busy || !reliable || !pending || !seconds || !el('wallet-consent').checked;
            el('wallet-cancel').disabled = busy || !reliable || !pending;
            el('wallet-confirm').textContent = busy ? 'Đang xác nhận…' : 'Xác nhận thanh toán mô phỏng';
        }
    }
    function apply(state, elapsed = 0) {
        if (!state || !states.has(state.status) || !Array.isArray(state.seatLabels)
            || !Number.isSafeInteger(state.amount) || state.amount <= 0
            || !Number.isFinite(state.serverTimeMillis) || !Number.isFinite(state.expiresAtMillis))
            throw new Error('Phản hồi trạng thái chưa hợp lệ. Vui lòng kiểm tra lại.');
        current = state; reliable = true; sampledAt = performance.now();
        remainingAtSample = state.expiresAtMillis - state.serverTimeMillis - elapsed;
        expiryAsked = false;
        if (wallet) {
            el('wallet-movie').textContent = state.movieTitle;
            el('wallet-room').textContent = state.roomName;
            el('wallet-showtime').textContent = new Intl.DateTimeFormat('vi-VN', {
                dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh'
            }).format(new Date(state.showtimeStart + '+07:00'));
            el('wallet-seats').textContent = state.seatLabels.join(', ');
            el('wallet-order').textContent = state.publicId;
            el('wallet-amount').textContent = money.format(state.amount);
        }
        if (!wallet) {
            const visual = el('wallet-qr-visual');
            if (visual) visual.hidden = state.status !== 'PENDING';
            const copy = el('wallet-copy');
            if (copy) copy.disabled = state.status !== 'PENDING';
        }
        render();
        if (state.status !== 'PENDING') {
            clearTimeout(pendingTimer);
            show(state.message, state.status === 'SUCCESS');
            if (!wallet && state.status === 'SUCCESS') {
                setTimeout(() => location.replace(root.dataset.finishUrl), 600);
            }
        } else if (actionError) show(actionError);
        else el('wallet-message').hidden = true;
    }
    async function request(url, body) {
        const controller = new AbortController(), timeout = setTimeout(() => controller.abort(), 12000);
        try {
            const response = await fetch(url, {
                method: body === undefined ? 'GET' : 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: {'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest',
                    ...(body === undefined ? {} : {'Content-Type': 'application/json', 'X-Demo-Wallet-CSRF': root.dataset.csrf})},
                ...(body === undefined ? {} : {body: JSON.stringify(body)}), signal: controller.signal
            });
            const result = await response.json();
            if (!response.ok || result.success === false) throw new Error(result.message || 'Chưa thực hiện được yêu cầu. Vui lòng tải lại trang.');
            return result;
        } finally { clearTimeout(timeout); }
    }
    const api = action => '/demo-wallet/api/' + publicId + '/' + action;
    function schedule() {
        clearTimeout(pendingTimer);
        if (!current || current.status === 'PENDING') pendingTimer = setTimeout(sync, 3000);
    }
    async function sync() {
        if (busy || syncing || (wallet && !token)) return;
        syncing = true;
        const sequence = revision, started = performance.now();
        try {
            const state = await request(wallet ? api('status') : root.dataset.statusUrl, wallet ? {token} : undefined);
            if (sequence === revision && !busy) apply(state, performance.now() - started);
        } catch (error) {
            if (sequence === revision && !busy) {
                reliable = false; render();
                show(error.name === 'AbortError' ? 'Kết nối chậm. Đang kiểm tra lại trạng thái.' : error.message);
            }
        } finally { syncing = false; schedule(); }
    }
    async function mutate(action) {
        if (busy || !reliable || current?.status !== 'PENDING') return;
        if (action === 'confirm' && (!el('wallet-consent').checked || remaining() <= 0)) return;
        busy = true; revision++; actionError = null; render();
        try {
            const started = performance.now();
            const state = await request(api(action), {token,
                ...(action === 'confirm' ? {expectedAmount: current.amount, confirmed: el('wallet-consent').checked} : {})});
            apply(state, performance.now() - started);
        } catch (error) {
            // Không tự gửi lại thanh toán khi mất phản hồi; hỏi database qua backend trước.
            reliable = false; render();
            actionError = error.name === 'AbortError' ? 'Chưa nhận được kết quả. Đang kiểm tra lại, không cần thanh toán lần nữa.' : error.message;
            show(actionError);
        } finally {
            busy = false; render();
            if (!reliable) await sync();
            else schedule();
        }
    }
    const theme = el('wallet-theme');
    if (theme) {
        try {
            const saved = localStorage.getItem('ute-theme');
            if (saved === 'light' || saved === 'dark') document.documentElement.setAttribute('data-theme', saved);
        } catch {}
        theme.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme')
                || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
            const next = currentTheme === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', next);
            try { localStorage.setItem('ute-theme', next); } catch {}
        });
    }
    if (wallet && !publicId) {
        el('wallet-import-form').addEventListener('submit', event => {
            event.preventDefault();
            try {
                const url = new URL(el('wallet-link-input').value.trim());
                const id = url.pathname.match(/^\/demo-wallet\/pay\/([a-f0-9-]+)$/)?.[1];
                const key = new URLSearchParams(url.hash.slice(1)).get('token');
                if (url.origin !== location.origin || !validId.test(id || '') || !validToken.test(key || '')
                    || url.username || url.password || url.search)
                    throw new Error('Hãy dán đầy đủ liên kết thanh toán được tạo từ web rạp đang dùng.');
                location.assign(url.pathname + url.hash);
            } catch (error) { show(error.message); }
        });
        return;
    }
    if (wallet) {
        if (!validId.test(publicId || '')) { show('Mã giao dịch không hợp lệ.'); return; }
        const key = 'g8-wallet-' + publicId;
        token = new URLSearchParams(location.hash.slice(1)).get('token');
        try {
            if (token && validToken.test(token)) sessionStorage.setItem(key, token);
            if (!token) token = sessionStorage.getItem(key);
        } catch {}
        // Khoá nằm trong fragment, không đi vào access log; xoá khỏi thanh địa chỉ sau khi đọc.
        if (location.hash) history.replaceState(null, '', location.pathname);
        if (!validToken.test(token || '')) { show('Liên kết thiếu khoá xác nhận. Hãy quét lại QR hoặc dán đầy đủ liên kết từ web rạp.'); return; }
        el('wallet-consent').addEventListener('change', render);
        el('wallet-confirm').addEventListener('click', () => mutate('confirm'));
        el('wallet-cancel').addEventListener('click', () => mutate('cancel'));
    } else {
        el('wallet-copy')?.addEventListener('click', async () => {
            const input = el('wallet-copy-link');
            try { await navigator.clipboard.writeText(input.value); show('Đã sao chép liên kết thanh toán.', true); }
            catch { input.select(); show('Liên kết đã được chọn. Bạn sao chép để gửi sang điện thoại.', true); }
        });
    }
    el('wallet-refresh').addEventListener('click', sync);
    window.addEventListener('focus', sync);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) sync(); });
    setInterval(() => {
        render();
        if (current?.status === 'PENDING' && remaining() <= 0 && !expiryAsked) {
            expiryAsked = true; sync();
        }
    }, 1000);
    sync();
})();
