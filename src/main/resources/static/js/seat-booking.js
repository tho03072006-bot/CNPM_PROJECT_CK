/* Server-confirmed seat state. Client time never decides ownership or availability. */
(() => {
    'use strict';
    const form = document.getElementById('booking-form');
    if (!form) return;
    const el = id => document.getElementById(id);
    const buttons = [...form.querySelectorAll('[data-seat-id]')];
    const selected = new Set();
    const money = new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'});
    const max = Number(form.dataset.maxAdmissions);
    const channel = typeof BroadcastChannel === 'function' ? new BroadcastChannel('cinema-seats') : null;
    let state = null, busy = false, syncing = false, reliable = false, editingIds = null;
    let sequence = 0, sampleTime = 0, remainingAtSample = 0, expiryRequested = false;
    let syncErrorShown = false;

    function message(text, success = false) {
        el('booking-message').textContent = text;
        el('booking-message').className = success ? 'alert alert-success' : 'alert alert-error';
        el('booking-message').hidden = false;
    }
    const sameIds = (first, second) => first && second && first.length === second.length
        && [...first].sort((a, b) => a - b).every((id, i) => id === [...second].sort((a, b) => a - b)[i]);
    function gaps(blocked) {
        const rows = new Map(), result = new Set();
        buttons.forEach(b => {
            if (!rows.has(b.dataset.seatRow)) rows.set(b.dataset.seatRow, []);
            rows.get(b.dataset.seatRow).push(b);
        });
        rows.forEach(row => {
            row.sort((a, b) => Number(a.dataset.seatColumn) - Number(b.dataset.seatColumn));
            for (let i = 0; i < row.length;) {
                if (blocked.has(row[i].dataset.seatId)) { i++; continue; }
                let end = i;
                while (end + 1 < row.length && !blocked.has(row[end + 1].dataset.seatId)
                    && Number(row[end + 1].dataset.seatColumn) === Number(row[end].dataset.seatColumn) + 1) end++;
                if (end === i && Number(row[i].dataset.capacity) === 1) result.add(row[i].dataset.seatId);
                i = end + 1;
            }
        });
        return result;
    }
    function selectionError() {
        const count = buttons.filter(b => selected.has(b.dataset.seatId))
            .reduce((sum, b) => sum + Number(b.dataset.capacity), 0);
        if (count > max) return 'Mỗi lượt chỉ được đặt tối đa ' + max + ' chỗ. Ghế đôi tính là hai chỗ.';
        const own = new Set(editingIds && state?.activeHold ? state.activeHold.seatIds.map(String) : []);
        const blocked = new Set(buttons.filter(b => b.dataset.seatStatus !== 'AVAILABLE'
            && !own.has(b.dataset.seatId)).map(b => b.dataset.seatId));
        const before = gaps(blocked);
        const newGap = [...gaps(new Set([...blocked, ...selected]))].find(id => !before.has(id));
        if (newGap) return 'Lựa chọn này để ghế ' + buttons.find(b => b.dataset.seatId === newGap).textContent.trim()
            + ' trống một mình. Hãy chọn thêm ghế đó hoặc đổi vị trí.';
        return null;
    }
    function remaining() {
        return Math.max(0, remainingAtSample - (performance.now() - sampleTime));
    }
    function render() {
        const hold = state?.activeHold;
        const own = new Set(hold ? hold.seatIds.map(String) : []);
        const expired = hold && remaining() <= 0;
        buttons.forEach(b => {
            const id = b.dataset.seatId;
            const available = b.dataset.seatStatus === 'AVAILABLE' || (editingIds && own.has(id));
            b.disabled = busy || !reliable || !state?.bookingOpen || !available || (!!hold && !editingIds) || !!expired;
            b.classList.toggle('selected', selected.has(id));
            b.classList.toggle('available', b.dataset.seatStatus === 'AVAILABLE');
            b.classList.toggle('held', b.dataset.seatStatus === 'HELD' || b.dataset.seatStatus === 'UNAVAILABLE');
            b.classList.toggle('booked', b.dataset.seatStatus === 'PAID');
            b.setAttribute('aria-pressed', String(selected.has(id)));
            const label = b.dataset.seatStatus === 'PAID' ? 'Đã bán'
                : b.dataset.seatStatus === 'AVAILABLE' ? 'Còn trống' : 'Đang giữ / chưa mở lại';
            b.setAttribute('aria-label', b.dataset.seatLabel + ', ' + label);
            b.title = label;
        });
        const choices = buttons.filter(b => selected.has(b.dataset.seatId));
        el('selected-seats').textContent = choices.length ? choices.map(b => b.textContent.trim()).join(', ') : 'Bạn chưa chọn ghế.';
        el('admission-count').textContent = choices.reduce((sum, b) => sum + Number(b.dataset.capacity), 0) + '/' + max;
        el('total-price').textContent = money.format(choices.reduce((sum, b) => sum + Number(b.dataset.price), 0));
        const error = selectionError();
        el('selection-hint').textContent = error || 'Bấm ghế còn trống để chọn. Lựa chọn được kiểm tra lại khi gửi.';
        el('selection-hint').className = error ? 'form-error' : 'form-hint';
        el('hold-button').textContent = busy ? 'Đang xử lý…' : editingIds ? 'Xác nhận đổi ghế' : 'Giữ ghế';
        const consent = el('terms-accepted').checked && (state?.ageRating === 'P' || el('age-confirmed').checked);
        el('hold-button').disabled = busy || !reliable || !state?.bookingOpen || !choices.length || !!error
            || (!!hold && !editingIds) || !!expired || !consent || el('hold-button').dataset.canHold !== 'true';
        el('suggest-button').disabled = busy || !reliable || !state?.bookingOpen || (!!hold && !editingIds);
        el('hold-timer').hidden = !hold;
        el('held-seat-labels').textContent = hold ? hold.seatLabels.join(', ') : '';
        el('held-total-price').textContent = hold ? money.format(hold.totalPrice) : '';
        el('change-seats-button').hidden = !hold || !!editingIds;
        el('change-seats-button').disabled = busy || !reliable || !state?.bookingOpen || !!expired;
        el('cancel-edit-button').hidden = !editingIds;
        el('hold-cancel-button').disabled = busy || !reliable || !hold;
        el('hold-pay-link').href = el('hold-timer').dataset.payUrl;
        el('hold-pay-link').setAttribute('aria-disabled', String(busy || !reliable || !hold || expired || !state?.paymentOpen));
        el('sync-status').textContent = !reliable ? 'Đang xác minh trạng thái ghế…'
            : state?.bookingOpen ? 'Sơ đồ ghế tự cập nhật mỗi 8 giây.' : 'Đã đóng nhận lượt giữ mới cho suất chiếu này.';
        el('age-message').textContent = state?.ageMessage || 'Đang tải phân loại độ tuổi…';
    }
    function apply(next, elapsed = 0) {
        if (!next || !next.seatMap || !Array.isArray(next.seatMap.seats)) throw new Error('Invalid state');
        if (editingIds && !sameIds(editingIds, next.activeHold?.ticketIds)) {
            editingIds = null; selected.clear();
            message('Lượt giữ đã thay đổi ở tab khác. Sơ đồ đã được cập nhật.');
        }
        state = next; reliable = true; expiryRequested = false;
        sampleTime = performance.now();
        remainingAtSample = next.activeHold ? next.activeHold.expiresAtMillis - next.serverTimeMillis - elapsed : 0;
        const statuses = new Map(next.seatMap.seats.map(seat => [String(seat.id), seat]));
        buttons.forEach(b => {
            const seat = statuses.get(b.dataset.seatId);
            b.dataset.seatStatus = seat?.status || 'UNAVAILABLE';
            if (seat) b.dataset.price = String(seat.price);
            if (b.dataset.seatStatus !== 'AVAILABLE'
                && !(editingIds && next.activeHold?.seatIds.includes(Number(b.dataset.seatId)))) selected.delete(b.dataset.seatId);
        });
        render(); tick();
    }
    async function request(url, body) {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 12000);
        try {
            const response = await fetch(url, {
                method: body === undefined ? 'GET' : 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: {'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest',
                    ...(body === undefined ? {} : {'Content-Type': 'application/json'})},
                ...(body === undefined ? {} : {body: JSON.stringify(body)}), signal: controller.signal
            });
            const result = await response.json();
            if (!response.ok || result.success === false) {
                const error = new Error(result.message || (response.status >= 500
                    ? 'Máy chủ gặp lỗi khi cập nhật ghế (lỗi ' + response.status + '). Hệ thống sẽ thử lại.'
                    : 'Chưa thực hiện được thao tác. Vui lòng tải lại trang.'));
                error.httpStatus = response.status;
                throw error;
            }
            return result;
        } finally { clearTimeout(timeout); }
    }
    async function sync() {
        if (busy || syncing) return;
        syncing = true;
        const seq = sequence, started = performance.now();
        try {
            const next = await request(form.dataset.stateUrl);
            if (seq === sequence && !busy) {
                apply(next, performance.now() - started);
                if (syncErrorShown) { message('Đã cập nhật lại trạng thái ghế.', true); syncErrorShown = false; }
            }
        } catch (error) {
            if (seq === sequence && !busy) {
                reliable = false; render();
                syncErrorShown = true;
                message(error.httpStatus ? error.message
                    : 'Chưa kết nối được máy chủ. Ghế được mở lại sau khi xác minh. Hệ thống sẽ thử cập nhật tiếp.');
            }
        } finally { syncing = false; }
    }
    async function mutate(body, url) {
        busy = true; sequence++; render();
        let responseMessage = '';
        try {
            const result = await request(url, body);
            responseMessage = result.message || 'Đã cập nhật lượt giữ ghế.';
            selected.clear(); editingIds = null;
            channel?.postMessage({showtime: form.dataset.stateUrl});
        } catch (error) {
            message(error.name === 'AbortError' ? 'Chưa xác nhận được kết quả. Đang kiểm tra lại trên máy chủ.' : error.message);
        } finally {
            // Ignore any earlier in-flight poll and confirm the mutation against the server.
            try {
                const started = performance.now(), next = await request(form.dataset.stateUrl);
                apply(next, performance.now() - started);
                if (responseMessage) message(responseMessage, true);
            } catch (error) { reliable = false; }
            busy = false; render();
        }
    }
    function tick() {
        const hold = state?.activeHold;
        if (!hold) return;
        const seconds = Math.ceil(remaining() / 1000);
        el('hold-timer-value').textContent = seconds > 0
            ? String(Math.floor(seconds / 60)).padStart(2, '0') + ':' + String(seconds % 60).padStart(2, '0')
            : 'Đang xác minh hết hạn…';
        if (seconds <= 0) {
            render();
            if (!expiryRequested && !busy) { expiryRequested = true; sync(); }
        }
    }
    buttons.forEach(b => b.addEventListener('click', () => {
        if (b.disabled) return;
        const id = b.dataset.seatId;
        if (selected.has(id)) selected.delete(id); else selected.add(id);
        render();
    }));
    ['terms-accepted', 'age-confirmed'].forEach(id => el(id).addEventListener('change', render));
    form.addEventListener('submit', event => {
        event.preventDefault();
        if (el('hold-button').disabled) return;
        mutate({seatIds: [...selected].map(Number), ...(editingIds ? {expectedTicketIds: [...editingIds]} : {}),
            ageConfirmed: el('age-confirmed').checked, termsAccepted: el('terms-accepted').checked}, form.dataset.holdUrl);
    });
    el('change-seats-button').addEventListener('click', () => {
        if (el('change-seats-button').disabled) return;
        editingIds = [...state.activeHold.ticketIds]; selected.clear();
        state.activeHold.seatIds.forEach(id => selected.add(String(id))); render();
        message('Chọn ghế mới rồi xác nhận. Nếu đổi thất bại, ghế cũ vẫn được giữ; thời gian không được gia hạn.', true);
    });
    el('cancel-edit-button').addEventListener('click', () => { editingIds = null; selected.clear(); render(); });
    el('hold-cancel-button').addEventListener('click', () => {
        if (!state?.activeHold || el('hold-cancel-button').disabled) return;
        mutate({ticketIds: [...state.activeHold.ticketIds]}, el('hold-timer').dataset.cancelUrl);
    });
    el('hold-pay-link').addEventListener('click', event => {
        if (el('hold-pay-link').getAttribute('aria-disabled') === 'true') event.preventDefault();
    });
    el('refresh-seats-button').addEventListener('click', sync);
    el('suggest-button').addEventListener('click', async () => {
        if (el('suggest-button').disabled) return;
        const params = new URLSearchParams({admissions: el('suggest-admissions').value, seatType: el('suggest-type').value});
        if (el('suggest-budget').value) params.set('maxBudget', el('suggest-budget').value);
        const revision = sequence;
        el('suggest-button').disabled = true;
        try {
            const result = await request(form.dataset.suggestionsUrl + '?' + params);
            if (revision !== sequence || busy) return;
            selected.clear(); result.seatIds.forEach(id => selected.add(String(id)));
            message('Gợi ý: ' + result.seatLabels.join(', ') + '. Ghế được giữ sau khi bạn xác nhận.', true);
        } catch (error) { message(error.message); }
        finally { render(); }
    });
    channel?.addEventListener('message', event => { if (event.data?.showtime === form.dataset.stateUrl) sync(); });
    window.addEventListener('focus', sync);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) sync(); });
    if (window.cinemaBookingInitial) apply(window.cinemaBookingInitial);
    else render();
    sync();
    setInterval(() => { if (!document.hidden) sync(); }, 8000);
    setInterval(tick, 1000);
})();
