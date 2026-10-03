const { test } = require('node:test');
const assert = require('node:assert/strict');
const scanner = require('../../main/resources/static/js/ticket-scanner.js');
const prefix = 'UTE-CINEMA:TICKET:V2:';

test('QR accepts exactly 8 digits or a canonical booking QR and rejects legacy IDs', () => {
    assert.equal(scanner.parseQr(prefix + '12345678'), '12345678');
    const bundle = 'UTE-CINEMA:BOOKING:V2:UTE-20261002123000-A1B2C3';
    assert.equal(scanner.parseQr(bundle), bundle);
    for (const bad of ['12', '#12', 'https://wallet/pay/12', prefix + '00000012', prefix + '0', prefix + '-1',
        prefix + '1.2', prefix + '9'.repeat(1000), prefix + '１２', prefix + '12junk',
        'UTE-CINEMA:TICKET:12', bundle + ':A2', bundle.toLowerCase()])
        assert.equal(scanner.parseQr(bad), null);
});
test('Upload validates file format and size', () => {
    assert.equal(scanner.validateFile({ type: 'image/png', size: 1024 }), null);
    for (const file of [null, { type: 'image/svg+xml', size: 12 }, { type: 'image/png', size: 0 },
        { type: 'image/jpeg', size: 10 * 1024 * 1024 + 1 }]) assert.ok(scanner.validateFile(file));
});
const location = { topLeftCorner: { x: 2, y: 2 }, topRightCorner: { x: 8, y: 2 },
    bottomLeftCorner: { x: 2, y: 8 }, bottomRightCorner: { x: 8, y: 8 } };
test('An image with two distinct tickets is flagged instead of choosing one', () => {
    const results = [{ data: prefix + '12345678', location }, { data: prefix + '87654321', location }, null];
    const pixels = new Uint8ClampedArray(20 * 20 * 4);
    assert.deepEqual(scanner.decodeTickets(pixels, 20, 20, () => results.shift()).ids, ['12345678', '87654321']);
    assert.equal(pixels[0], 0, 'decoder must not mutate original image');
});
test('Repeated identical QR is one ticket; payment QR is rejected', () => {
    let results = [{ data: prefix + '12345678', location }, { data: prefix + '12345678', location }, null];
    assert.deepEqual(scanner.decodeTickets(new Uint8ClampedArray(1600), 20, 20, () => results.shift()).ids, ['12345678']);
    results = [{ data: 'https://momo/pay/12', location }, null];
    assert.deepEqual(scanner.decodeTickets(new Uint8ClampedArray(1600), 20, 20, () => results.shift()), { ids: [], found: true });
});
test('Overlapping image regions detect distinct tickets when whole-image decoder cannot read them', () => {
    const results = [null, { data: prefix + '12345678', location }, null, { data: prefix + '87654321', location }, null];
    const result = scanner.decodeImageTickets(new Uint8ClampedArray(80 * 40 * 4), 80, 40, () => results.shift());
    assert.deepEqual(result.ids, ['12345678', '87654321']);
});

class Element {
    constructor() { this.listeners = {}; this.hidden = true; this.disabled = false; this.value = '';
        this.classList = { toggle() {} }; }
    addEventListener(name, callback) { this.listeners[name] = callback; }
}
function setup(getUserMedia) {
    const names = ['scanner-start', 'scanner-stop', 'scanner-file', 'scanner-video', 'scanner-canvas', 'scanner-status'];
    const elements = Object.fromEntries(names.map(n => [n, new Element()]));
    const input = new Element(), form = new Element(), bookingInput = new Element();
    let submits = 0, permissionRequests = 0;
    form.requestSubmit = () => { submits++; };
    const document = new Element();
    document.getElementById = id => id === 'ma' ? input : id === 'scanner-lookup' ? bookingInput : form;
    const env = new Element(); Object.assign(env, { document, isSecureContext: true,
        navigator: { mediaDevices: { getUserMedia: options => { permissionRequests++; assert.equal(options.audio, false); return getUserMedia(); } } },
        jsQR: () => null, setTimeout: () => 1, clearTimeout() {} });
    elements['scanner-video'].play = async () => {};
    elements['scanner-video'].readyState = 0;
    const app = scanner.createScanner(env, { querySelector: selector => elements[selector.slice(1)] });
    return { app, env, elements, input, bookingInput, form, submits: () => submits, requests: () => permissionRequests };
}
test('Camera permission is requested only after explicit start; denial gives manual fallback', async () => {
    const app = setup(async () => { throw Object.assign(new Error(), { name: 'NotAllowedError' }); });
    assert.equal(app.requests(), 0);
    await app.app.start();
    assert.equal(app.requests(), 1); assert.equal(app.submits(), 0);
    assert.match(app.elements['scanner-status'].textContent, /từ chối/);
    assert.equal(app.elements['scanner-video'].hidden, true);
});
test('A late permission grant after stop closes tracks and cannot restart scanning', async () => {
    let grant, stopped = 0;
    const app = setup(() => new Promise(resolve => { grant = resolve; }));
    const pending = app.app.start(); app.app.stop();
    grant({ getTracks: () => [{ stop: () => stopped++ }] }); await pending;
    assert.equal(stopped, 1); assert.equal(app.submits(), 0);
    assert.equal(app.elements['scanner-video'].srcObject, null);
});
test('Camera is released when stopped or page becomes hidden', async () => {
    let stopped = 0;
    const app = setup(async () => ({ getTracks: () => [{ stop: () => stopped++ }] }));
    await app.app.start(); assert.equal(stopped, 0);
    app.env.document.hidden = true; app.env.document.listeners.visibilitychange();
    assert.equal(stopped, 1); assert.equal(app.elements['scanner-video'].hidden, true);
});
test('Insecure host never requests camera', async () => {
    const app = setup(async () => { throw new Error('must not run'); }); app.env.isSecureContext = false;
    await app.app.start(); assert.equal(app.requests(), 0);
    assert.match(app.elements['scanner-status'].textContent, /HTTPS/);
});
test('Image decoding submits only the GET lookup form and releases bitmap', async () => {
    const app = setup(async () => {}); let released = 0;
    app.env.createImageBitmap = async () => ({ width: 20, height: 20, close: () => released++ });
    app.elements['scanner-file'].files = [{ type: 'image/png', size: 500 }];
    app.elements['scanner-canvas'].getContext = () => ({ drawImage() {}, getImageData: () => ({ data: new Uint8ClampedArray(1600) }) });
    let calls = 0; app.env.jsQR = () => calls++ === 0 ? { data: prefix + '12345678', location } : null;
    await app.app.chooseFile();
    assert.equal(app.input.value, '12345678'); assert.equal(app.submits(), 1); assert.equal(released, 1);
});
test('Ambiguous multi-ticket upload never submits lookup', async () => {
    const app = setup(async () => {});
    app.env.createImageBitmap = async () => ({ width: 20, height: 20, close() {} });
    app.elements['scanner-file'].files = [{ type: 'image/png', size: 500 }];
    app.elements['scanner-canvas'].getContext = () => ({ drawImage() {}, getImageData: () => ({ data: new Uint8ClampedArray(1600) }) });
    let calls = 0; app.env.jsQR = () => ({ data: prefix + (calls++ === 0 ? '12345678' : '87654321'), location });
    await app.app.chooseFile(); assert.equal(app.submits(), 0);
    assert.match(app.elements['scanner-status'].textContent, /nhiều mã vé/);
});
test('Broken and oversized images show errors without submitting', async () => {
    const app = setup(async () => {});
    app.elements['scanner-file'].files = [{ type: 'image/png', size: 500 }];
    app.env.createImageBitmap = async () => { throw new Error('corrupt'); };
    await app.app.chooseFile(); assert.match(app.elements['scanner-status'].textContent, /Ảnh bị lỗi/);
    app.env.createImageBitmap = async () => ({ width: 10000, height: 10000, close() {} });
    await app.app.chooseFile(); assert.match(app.elements['scanner-status'].textContent, /25 triệu/);
    assert.equal(app.submits(), 0);
});

test('Booking QR submits only canonical booking lookup, never a seat ID or check-in', async () => {
    const app = setup(async () => {});
    const payload = 'UTE-CINEMA:BOOKING:V2:UTE-20261002123000-A1B2C3';
    app.env.createImageBitmap = async () => ({ width: 20, height: 20, close() {} });
    app.elements['scanner-file'].files = [{ type: 'image/png', size: 500 }];
    app.elements['scanner-canvas'].getContext = () => ({ drawImage() {}, getImageData: () => ({ data: new Uint8ClampedArray(1600) }) });
    let count = 0; app.env.jsQR = () => count++ === 0 ? { data: payload, location } : null;
    await app.app.chooseFile();
    assert.equal(app.submits(), 1); assert.equal(app.input.disabled, true);
    assert.equal(app.bookingInput.disabled, false); assert.equal(app.bookingInput.value, payload);
});
