(function (root, factory) {
    'use strict';
    const api = factory();
    if (typeof module === 'object' && module.exports) module.exports = api;
    else {
        root.TicketScanner = api;
        const panel = root.document.getElementById('ticket-scanner');
        if (panel) api.createScanner(root, panel);
    }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    'use strict';
    const PREFIX = 'UTE-CINEMA:TICKET:V2:';
    const BOOKING_PREFIX = 'UTE-CINEMA:BOOKING:V2:';
    function parseQr(value) {
        if (typeof value !== 'string') return null;
        if (value.startsWith(PREFIX)) {
            const code = value.slice(PREFIX.length);
            return /^[1-9][0-9]{7}$/.test(code) ? code : null;
        }
        if (value.startsWith(BOOKING_PREFIX) && /^UTE-[0-9]{14}-[A-F0-9]{6}$/.test(value.slice(BOOKING_PREFIX.length)))
            return value;
        return null;
    }
    function validateFile(file) {
        if (!file || !['image/png', 'image/jpeg', 'image/webp', 'image/bmp'].includes(file.type))
            return 'Hãy chọn ảnh PNG, JPG, WEBP hoặc BMP.';
        if (!file.size || file.size > 10 * 1024 * 1024) return 'Ảnh phải có dữ liệu và không vượt quá 10 MB.';
        return null;
    }
    function decodeTickets(pixels, width, height, decoder) {
        const working = new Uint8ClampedArray(pixels);
        const ids = new Set();
        let found = false;
        for (let n = 0; n < 9; n++) {
            const qr = decoder(working, width, height, { inversionAttempts: 'attemptBoth' });
            if (!qr) break;
            found = true;
            const id = parseQr(qr.data);
            if (id) ids.add(id);
            if (ids.size > 1) break;
            const points = qr.location && ['topLeftCorner', 'topRightCorner', 'bottomLeftCorner', 'bottomRightCorner']
                .map(name => qr.location[name]);
            if (!points || points.some(point => !point || !Number.isFinite(point.x) || !Number.isFinite(point.y))) break;
            const minX = Math.max(0, Math.floor(Math.min(...points.map(p => p.x))) - 4);
            const maxX = Math.min(width - 1, Math.ceil(Math.max(...points.map(p => p.x))) + 4);
            const minY = Math.max(0, Math.floor(Math.min(...points.map(p => p.y))) - 4);
            const maxY = Math.min(height - 1, Math.ceil(Math.max(...points.map(p => p.y))) + 4);
            for (let y = minY; y <= maxY; y++) for (let x = minX; x <= maxX; x++) {
                const i = (y * width + x) * 4;
                working[i] = working[i + 1] = working[i + 2] = working[i + 3] = 255;
            }
        }
        return { ids: [...ids], found };
    }
    function cameraError(error) {
        if (error && ['NotAllowedError', 'PermissionDeniedError', 'SecurityError'].includes(error.name))
            return 'Quyền camera bị từ chối. Hãy cấp quyền trong trình duyệt, chọn ảnh QR hoặc nhập mã vé.';
        if (error && error.name === 'NotFoundError') return 'Không tìm thấy camera. Bạn có thể chọn ảnh QR hoặc nhập mã vé.';
        return 'Không mở được camera. Hãy kiểm tra camera có đang được ứng dụng khác sử dụng, hoặc chọn ảnh/nhập mã vé.';
    }
    function decodeImageTickets(pixels, width, height, decoder) {
        const result = decodeTickets(pixels, width, height, decoder);
        const ids = new Set(result.ids);
        if (ids.size > 1) return result;
        // Ảnh hóa đơn có thể chứa chữ/bảng và nhiều QR. Đọc thêm các vùng chồng lấn
        // để không chọn nhầm mã đầu tiên và để nhận QR nhỏ trong ảnh lớn.
        for (const [rows, cols] of [[1, 2], [2, 1], [6, 1], [1, 6], [6, 6]])
            for (let row = 0; row < rows; row++) for (let col = 0; col < cols; col++) {
            const left = Math.max(0, Math.floor((col - 0.15) * width / cols));
            const top = Math.max(0, Math.floor((row - 0.15) * height / rows));
            const right = Math.min(width, Math.ceil((col + 1.15) * width / cols));
            const bottom = Math.min(height, Math.ceil((row + 1.15) * height / rows));
            const w = right - left, h = bottom - top;
            const region = new Uint8ClampedArray(w * h * 4);
            for (let y = 0; y < h; y++) region.set(pixels.subarray(((top + y) * width + left) * 4,
                ((top + y) * width + right) * 4), y * w * 4);
            const tile = decodeTickets(region, w, h, decoder);
            result.found = result.found || tile.found;
            tile.ids.forEach(id => ids.add(id));
            if (ids.size > 1) return { ids: [...ids], found: true };
        }
        return { ids: [...ids], found: result.found };
    }
    function createScanner(env, panel) {
        const find = id => panel.querySelector('#' + id);
        const startButton = find('scanner-start'), stopButton = find('scanner-stop');
        const fileInput = find('scanner-file'), video = find('scanner-video'), canvas = find('scanner-canvas');
        const status = find('scanner-status'), form = env.document.getElementById('ticket-code-form');
        const input = env.document.getElementById('ma');
        const bookingInput = env.document.getElementById('scanner-lookup');
        let stream = null, timer = null, generation = 0;
        function message(text, error) {
            status.textContent = text;
            status.classList.toggle('is-error', !!error);
        }
        function stop() {
            generation++;
            if (timer !== null) env.clearTimeout(timer);
            timer = null;
            if (stream) stream.getTracks().forEach(track => track.stop());
            stream = null;
            video.srcObject = null; video.hidden = true;
            startButton.disabled = false; stopButton.disabled = true;
        }
        function ready() {
            if (typeof env.jsQR !== 'function') {
                message('Không tải được bộ đọc QR. Hãy tải lại trang hoặc nhập mã vé.', true);
                return false;
            }
            return true;
        }
        function read(source, width, height, maxSide, fromFile) {
            const scale = Math.min(1, maxSide / Math.max(width, height));
            canvas.width = Math.max(1, Math.round(width * scale));
            canvas.height = Math.max(1, Math.round(height * scale));
            const context = canvas.getContext('2d', { willReadFrequently: true });
            if (!context) throw new Error('Canvas unavailable');
            context.drawImage(source, 0, 0, canvas.width, canvas.height);
            return (fromFile ? decodeImageTickets : decodeTickets)(context.getImageData(0, 0, canvas.width, canvas.height).data,
                canvas.width, canvas.height, env.jsQR);
        }
        function useResult(result, fromFile) {
            if (result.ids.length > 1) {
                message('Ảnh có nhiều mã vé. Hãy đưa một QR vào camera hoặc cắt ảnh chỉ còn một QR vé hoặc QR chung của hóa đơn.', true);
                return false;
            }
            if (result.ids.length === 1) {
                stop();
                const value = result.ids[0];
                const grouped = value.startsWith(BOOKING_PREFIX);
                input.disabled = grouped; input.value = grouped ? '' : value;
                bookingInput.disabled = !grouped; bookingInput.value = grouped ? value : '';
                message(grouped ? 'Đã đọc QR hóa đơn. Đang tra cứu các ghế…' : 'Đã đọc mã vé ' + value + '. Đang tra cứu…');
                form.requestSubmit(); // GET tra cứu; tuyệt đối không gửi form xác nhận vào phòng.
                return true;
            }
            if (result.found) message('QR không phải mã vé UTE Cinema hợp lệ. Hãy dùng QR trên vé hoặc hóa đơn.', true);
            else if (fromFile) message('Không đọc được QR. Chọn ảnh rõ hơn, cắt ảnh quanh một QR hoặc nhập mã vé.', true);
            return false;
        }
        async function start() {
            stop();
            if (!ready()) return;
            if (!env.isSecureContext || !env.navigator.mediaDevices || !env.navigator.mediaDevices.getUserMedia) {
                message('Camera cần HTTPS hoặc localhost và trình duyệt hỗ trợ. Bạn có thể chọn ảnh QR hoặc nhập mã vé.', true);
                return;
            }
            const current = generation;
            startButton.disabled = true; stopButton.disabled = false;
            message('Hãy cho phép trình duyệt truy cập camera để quét vé.');
            try {
                const opened = await env.navigator.mediaDevices.getUserMedia({ audio: false, video: true });
                if (generation !== current) { opened.getTracks().forEach(track => track.stop()); return; }
                stream = opened; video.srcObject = opened; video.hidden = false;
                await video.play();
                if (generation !== current) return;
                message('Camera đã mở. Đưa một QR vé hoặc QR chung của hóa đơn vào khung hình.');
                const frame = () => {
                    if (generation !== current) return;
                    try {
                        if (video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0
                                && useResult(read(video, video.videoWidth, video.videoHeight, 960), false)) return;
                    } catch (error) { stop(); message('Không đọc được hình ảnh camera. Hãy chọn ảnh QR hoặc nhập mã vé.', true); return; }
                    timer = env.setTimeout(frame, 250);
                };
                frame();
            } catch (error) {
                if (generation === current) { stop(); message(cameraError(error), true); }
            }
        }
        async function chooseFile() {
            stop();
            const file = fileInput.files && fileInput.files[0];
            fileInput.value = '';
            if (!file) return;
            const invalid = validateFile(file);
            if (invalid) { message(invalid, true); return; }
            if (!ready()) return;
            const current = generation;
            message('Đang đọc QR từ ảnh…');
            let bitmap = null, url = null;
            try {
                if (env.createImageBitmap) bitmap = await env.createImageBitmap(file);
                else {
                    url = env.URL.createObjectURL(file);
                    bitmap = await new Promise((resolve, reject) => {
                        const img = new env.Image(); img.onload = () => resolve(img); img.onerror = reject; img.src = url;
                    });
                }
                if (generation !== current) return;
                const width = bitmap.width, height = bitmap.height;
                if (!width || !height || width * height > 25000000) {
                    message('Ảnh vượt quá 25 triệu điểm ảnh. Hãy cắt ảnh chỉ còn một QR vé hoặc QR chung của hóa đơn.', true); return;
                }
                useResult(read(bitmap, width, height, 2000, true), true);
            } catch (error) {
                if (generation === current) message('Ảnh bị lỗi hoặc không đọc được. Hãy chọn ảnh khác hoặc nhập mã vé.', true);
            } finally {
                if (bitmap && typeof bitmap.close === 'function') bitmap.close();
                if (url) env.URL.revokeObjectURL(url);
            }
        }
        startButton.addEventListener('click', start);
        stopButton.addEventListener('click', () => { stop(); message('Đã tắt camera.'); });
        fileInput.addEventListener('change', chooseFile);
        env.addEventListener('pagehide', stop);
        env.document.addEventListener('visibilitychange', () => { if (env.document.hidden) stop(); });
        form.addEventListener('submit', stop);
        return { start, stop, chooseFile };
    }
    return { parseQr, validateFile, decodeTickets, decodeImageTickets, cameraError, createScanner };
}));
