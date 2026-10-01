const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/resources/static/js/seat-booking.js', 'utf8');
const template = fs.readFileSync('src/main/resources/templates/booking/seat-map.html', 'utf8');
class Element {
    constructor(id) {
        this.id = id; this.dataset = {}; this.attributes = {}; this.listeners = {};
        this.disabled = false; this.hidden = false; this.checked = false; this.textContent = '';
        const classes = new Set();
        this.classList = {toggle: (name, yes) => yes ? classes.add(name) : classes.delete(name)};
    }
    addEventListener(name, callback) { (this.listeners[name] ||= []).push(callback); }
    setAttribute(name, value) { this.attributes[name] = String(value); }
    getAttribute(name) { return this.attributes[name]; }
    fire(name) { for (const fn of this.listeners[name] || []) fn({preventDefault(){}}); }
}
function snapshot(status = 'AVAILABLE', ids = null, expiry = 300000, capacity = 1, seatCount = 1) {
    return {seatMap: {seats: Array.from({length:seatCount}, (_,i)=>({id:i+1,status,price:75000*capacity}))},
        activeHold: ids ? {ticketIds:ids,seatIds:[1],seatLabels:['A1'],totalPrice:75000*capacity,expiresAtMillis:expiry} : null,
        serverTimeMillis:0,bookingOpen:true,paymentOpen:true,ageRating:'T13',ageMessage:'Từ 13 tuổi'};
}
function setup(initial = snapshot(), capacity = 1) {
    const ids = ['booking-form','booking-message','hold-button','terms-accepted','age-confirmed','selected-seats',
        'admission-count','total-price','selection-hint','suggest-button','hold-timer','held-seat-labels',
        'held-total-price','change-seats-button','cancel-edit-button','hold-cancel-button','hold-pay-link',
        'sync-status','age-message','hold-timer-value','refresh-seats-button','suggest-admissions','suggest-type','suggest-budget'];
    const elements = new Map(ids.map(id => [id,new Element(id)]));
    ids.forEach(id => assert.ok(template.includes('id="'+id+'"'), 'Missing template element '+id));
    const seats = initial.seatMap.seats.map((seat,i) => {
        const b = new Element('seat-'+seat.id); b.textContent = 'A'+(i+1);
        b.dataset = {seatId:String(seat.id),seatRow:'A',seatColumn:String(i+1),seatStatus:seat.status,
            capacity:String(capacity),price:String(seat.price),seatLabel:'Ghế A'+(i+1)};
        return b;
    });
    const form = elements.get('booking-form');
    form.dataset = {maxAdmissions:'8',stateUrl:'/state',holdUrl:'/hold',suggestionsUrl:'/suggestions'};
    form.querySelectorAll = () => seats;
    elements.get('hold-button').dataset.canHold = 'true';
    elements.get('hold-timer').dataset = {cancelUrl:'/cancel',payUrl:'/checkout'};
    elements.get('suggest-admissions').value = '2'; elements.get('suggest-type').value = 'ANY';
    elements.get('suggest-budget').value = '';
    const window = new Element('window'), document = new Element('document');
    document.getElementById = id => elements.get(id); document.hidden = false;
    let now = 0, server = structuredClone(initial), offline = false, nextPost = null, nextGet = null;
    const requests = [], intervals = [], broadcasts = [];
    class Channel extends Element { constructor() { super('channel'); broadcasts.push(this); } postMessage(value) { this.sent = value; } }
    const fetch = async (url, options) => {
        requests.push({url,options});
        if (options.method === 'POST' && nextPost) return nextPost(url, JSON.parse(options.body));
        if (nextGet) { const fn = nextGet; nextGet = null; return fn(); }
        if (offline) throw new Error('Offline');
        const body = url.startsWith('/suggestions') ? {seatIds:[1],seatLabels:['A1']} : server;
        return {ok:true,json:async()=>structuredClone(body)};
    };
    window.cinemaBookingInitial = structuredClone(initial);
    vm.runInNewContext(source,{window,document,fetch,performance:{now:()=>now},Intl,URLSearchParams,
        AbortController,BroadcastChannel:Channel,setTimeout:()=>1,clearTimeout(){},
        setInterval:(fn,period)=>intervals.push({fn,period})});
    return {get:id=>elements.get(id),seats,requests,window,broadcasts,
        advance:ms=>{now+=ms},tick:()=>intervals.find(i=>i.period===1000).fn(),
        server:next=>{server=structuredClone(next)},offline:value=>{offline=value},
        post:fn=>{nextPost=fn},nextGet:fn=>{nextGet=fn},
        consent(){elements.get('terms-accepted').checked=true;elements.get('age-confirmed').checked=true;elements.get('age-confirmed').fire('change');},
        select(index=0){seats[index].fire('click')},
        submit(){form.fire('submit')},refresh(){elements.get('refresh-seats-button').fire('click')}};
}
async function flush() { for(let i=0;i<4;i++) await new Promise(setImmediate); }

test('Expired timer keeps HELD until the server confirms; another tab may have paid', async()=>{
    const app=setup(snapshot('HELD',[10],1000)); await flush();
    app.offline(true); app.advance(1100); app.tick(); await flush();
    assert.equal(app.seats[0].dataset.seatStatus,'HELD');
    assert.equal(app.seats[0].disabled,true);
    assert.equal(app.get('hold-pay-link').getAttribute('aria-disabled'),'true');
    assert.ok(app.requests.length>=2);
    app.offline(false); app.server(snapshot('PAID')); app.refresh(); await flush();
    assert.equal(app.seats[0].dataset.seatStatus,'PAID'); assert.equal(app.seats[0].disabled,true);
});

test('Lost hold response is recovered from server state without another booking',async()=>{
    const app=setup(); await flush(); app.consent(); app.select();
    app.post(async()=>{app.server(snapshot('HELD',[10]));throw Error('Response lost')});
    app.submit(); await flush();
    assert.equal(app.requests.filter(r=>r.options.method==='POST').length,1);
    assert.equal(app.seats[0].dataset.seatStatus,'HELD');
    assert.equal(app.get('hold-timer').hidden,false);
    assert.equal(app.get('hold-pay-link').getAttribute('aria-disabled'),'false');
});

test('Cancel request includes the exact ticket identities and never frees seats while confirmation is unavailable',async()=>{
    const app=setup(snapshot('HELD',[10])); await flush();
    let payload;
    app.post(async(url,body)=>{payload=body;app.offline(true);return{ok:true,json:async()=>({success:true})}});
    app.get('hold-cancel-button').fire('click'); await flush();
    assert.deepEqual(payload.ticketIds,[10]); assert.equal(app.seats[0].dataset.seatStatus,'HELD');
    assert.equal(app.seats[0].disabled,true);
});

test('Selecting four of five whole couple seats is valid and counts eight admissions',async()=>{
    const app=setup(snapshot('AVAILABLE',null,300000,2,5),2);await flush();app.consent();
    for(let i=0;i<4;i++)app.select(i);
    assert.equal(app.get('admission-count').textContent,'8/8');
    assert.equal(app.get('hold-button').disabled,false);
});

test('Consent is required before holding seats',async()=>{
    const app=setup();await flush();app.select();assert.equal(app.get('hold-button').disabled,true);
    app.consent();assert.equal(app.get('hold-button').disabled,false);
});

test('Countdown follows elapsed monotonic time without parsing a local date',async()=>{
    const app=setup(snapshot('HELD',[10]));await flush();
    assert.equal(app.get('hold-timer-value').textContent,'05:00');
    app.advance(1000);app.tick();assert.equal(app.get('hold-timer-value').textContent,'04:59');
    assert.equal(source.includes('Date.now()'),false);
    assert.equal(source.includes('new Date('),false);
});

test('Changing seats captures the old hold identity and sends it with the selection',async()=>{
    const state=snapshot('AVAILABLE',null,300000,1,2);state.seatMap.seats[0].status='HELD';
    state.activeHold={ticketIds:[10],seatIds:[1],seatLabels:['A1'],totalPrice:75000,expiresAtMillis:180000};
    const app=setup(state);await flush();app.consent();app.get('change-seats-button').fire('click');
    app.select(0);app.select(0); // Keeping the same selection is an idempotent replacement request.
    let payload;
    app.post(async(url,body)=>{payload=body;return{ok:true,json:async()=>({success:true})}});
    // Select both to avoid leaving a new singleton in this two-seat row.
    app.select(1);app.submit();await flush();
    assert.deepEqual(payload.expectedTicketIds,[10]);assert.deepEqual(payload.seatIds,[1,2]);
});

test('Polling an updated hold discards an edit prepared for the old hold',async()=>{
    const app=setup(snapshot('HELD',[10]));await flush();app.get('change-seats-button').fire('click');
    app.server(snapshot('HELD',[20]));app.refresh();await flush();
    assert.equal(app.get('cancel-edit-button').hidden,true);
    assert.match(app.get('booking-message').textContent,/tab khác/);
});

test('Focus and cross-tab notifications trigger a server refresh',async()=>{
    const app=setup();await flush();let count=app.requests.length;
    app.window.fire('focus');await flush();assert.ok(app.requests.length>count);count=app.requests.length;
    for(const fn of app.broadcasts[0].listeners.message)fn({data:{showtime:'/state'}});
    await flush();assert.ok(app.requests.length>count);
});

test('Server validation errors show the actual reason instead of claiming a network outage',async()=>{
    const app=setup();await flush();
    app.nextGet(async()=>({ok:false,status:400,json:async()=>({success:false,message:'Phim chưa có phân loại độ tuổi hợp lệ.'})}));
    app.refresh();await flush();
    assert.match(app.get('booking-message').textContent,/phân loại độ tuổi/);
    assert.equal(app.get('booking-message').textContent.includes('Chưa kết nối'),false);
    assert.equal(app.get('hold-button').disabled,true);
    app.refresh();await flush();
    assert.match(app.get('booking-message').textContent,/Đã cập nhật/);
});
test('An old in-flight poll cannot overwrite a completed booking',async()=>{
    const app=setup();await flush();
    let resolveOld;
    app.nextGet(()=>new Promise(resolve=>{resolveOld=resolve}));
    app.refresh();await flush();app.consent();app.select();
    app.post(async()=>{app.server(snapshot('HELD',[10]));return{ok:true,json:async()=>({success:true})}});
    app.submit();await flush();
    resolveOld({ok:true,json:async()=>snapshot()});await flush();
    assert.equal(app.seats[0].dataset.seatStatus,'HELD');
    assert.equal(app.get('hold-timer').hidden,false);
});
