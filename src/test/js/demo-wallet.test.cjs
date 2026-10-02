const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync('src/main/resources/static/js/demo-wallet.js','utf8');
const id='aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',token='A'.repeat(43);
class Element {
    constructor(){this.dataset={};this.listeners={};this.hidden=false;this.disabled=true;this.checked=false;this.value='';}
    addEventListener(name,fn){(this.listeners[name]||=[]).push(fn)}
    fire(name){for(const fn of this.listeners[name]||[])fn({preventDefault(){}})}
    setAttribute(name,value){this[name]=value}getAttribute(name){return this[name]}
}
function state(status='PENDING',expires=300000){return {publicId:id,status,seatLabels:['A1','A2'],movieTitle:'Phim Nhóm 8',roomName:'Cinema 1',showtimeStart:'2026-10-03T10:00:00',amount:150000,expiresAtMillis:expires,serverTimeMillis:0,message:status};}
function setup({role='wallet',paymentId=id,hash='#token='+token,stored=null}={}){
    const ids=['demo-wallet','wallet-message','wallet-countdown','wallet-status','wallet-actions','wallet-confirm','wallet-cancel','wallet-consent','wallet-movie','wallet-room','wallet-showtime','wallet-seats','wallet-order','wallet-amount','wallet-theme','wallet-import-form','wallet-link-input','wallet-copy','wallet-copy-link','wallet-refresh','wallet-qr-visual'];
    const elements=new Map(ids.map(i=>[i,new Element()])),root=elements.get('demo-wallet');
    root.dataset={role,paymentId,csrf:'device-csrf',statusUrl:'/merchant/status',finishUrl:'/merchant/finish'};
    const document=new Element();document.getElementById=i=>elements.get(i);document.documentElement=new Element();document.hidden=false;
    const window=new Element();window.matchMedia=()=>({matches:false});
    const storage=new Map(stored?[['g8-wallet-'+id,stored]]:[]);
    const sessionStorage={getItem:k=>storage.get(k),setItem:(k,v)=>storage.set(k,v)};
    const location={hash,pathname:'/demo-wallet/pay/'+id,origin:'https://g8.test',assign:v=>{location.assigned=v},replace:v=>{location.replaced=v}};
    let now=0,current=state(),offline=false,postHandler=null,statusHandler=null;
    const requests=[],timers=[],intervals=[];
    const fetch=async(url,options)=>{
        requests.push({url,options,body:options.body?JSON.parse(options.body):null});
        if(offline)throw Error('Offline');
        if(url.endsWith('/confirm')||url.endsWith('/cancel')) {
            if(postHandler)return postHandler(url,options);
            current=state(url.endsWith('/confirm')?'SUCCESS':'CANCELLED');
        }else if(statusHandler){const fn=statusHandler;statusHandler=null;return fn()}
        return {ok:true,json:async()=>structuredClone(current)};
    };
    vm.runInNewContext(source,{document,window,location,history:{replaceState:(_,__,v)=>{location.hash='';location.cleaned=v}},
        sessionStorage,localStorage:{getItem(){},setItem(){}},navigator:{clipboard:{writeText:async()=>{}}},fetch,
        performance:{now:()=>now},Intl,URL,URLSearchParams,AbortController,
        setTimeout:(fn,ms)=>{timers.push({fn,ms});return timers.length},clearTimeout(){},
        setInterval:(fn,ms)=>{intervals.push({fn,ms});return intervals.length}});
    return {el:i=>elements.get(i),location,storage,requests,timers,
        consent(){elements.get('wallet-consent').checked=true;elements.get('wallet-consent').fire('change')},
        confirm(){elements.get('wallet-confirm').fire('click')},
        refresh(){elements.get('wallet-refresh').fire('click')},
        server:s=>{current=s},offline:v=>{offline=v},post:fn=>{postHandler=fn},nextStatus:fn=>{statusHandler=fn},
        advance:ms=>{now+=ms},tick(){intervals.find(i=>i.ms===1000).fn()}};
}
async function flush(){for(let i=0;i<5;i++)await new Promise(setImmediate)}
test('QR fragment is removed and only retained in this tab session',async()=>{
    const app=setup();await flush();assert.equal(app.location.hash,'');assert.equal(app.storage.get('g8-wallet-'+id),token);
    assert.equal(app.requests[0].body.token,token);assert.equal(app.requests[0].options.headers['X-Demo-Wallet-CSRF'],'device-csrf');
    const reload=setup({hash:'',stored:token});await flush();assert.equal(reload.requests[0].body.token,token);
});
test('Missing bearer token cannot request or confirm a payment',async()=>{
    const app=setup({hash:''});await flush();assert.equal(app.requests.length,0);assert.equal(app.el('wallet-confirm').disabled,true);
});
test('Consent is required and exact server amount is sent to confirm',async()=>{
    const app=setup();await flush();assert.equal(app.el('wallet-confirm').disabled,true);
    app.confirm();await flush();assert.equal(app.requests.length,1);
    app.consent();assert.equal(app.el('wallet-confirm').disabled,false);app.confirm();await flush();
    const posts=app.requests.filter(r=>r.url.endsWith('/confirm'));assert.equal(posts.length,1);
    assert.deepEqual(posts[0].body,{token,expectedAmount:150000,confirmed:true});assert.equal(app.el('wallet-actions').hidden,true);
});
test('Lost confirmation response reads SUCCESS without another confirm request',async()=>{
    const app=setup();await flush();app.consent();
    app.post(async()=>{app.server(state('SUCCESS'));throw Error('Response lost')});
    app.confirm();await flush();assert.equal(app.requests.filter(r=>r.url.endsWith('/confirm')).length,1);
    assert.equal(app.el('wallet-status').textContent,'SUCCESS');assert.equal(app.el('wallet-confirm').disabled,true);
});
test('Offline status disables confirmation until backend state is verified',async()=>{
    const app=setup();await flush();app.consent();app.offline(true);app.refresh();await flush();
    assert.equal(app.el('wallet-confirm').disabled,true);app.confirm();await flush();
    assert.equal(app.requests.filter(r=>r.url.endsWith('/confirm')).length,0);
    app.offline(false);app.refresh();await flush();assert.equal(app.el('wallet-confirm').disabled,false);
});
test('An old polling response cannot overwrite successful confirmation',async()=>{
    const app=setup();await flush();app.consent();let resolve;
    app.nextStatus(()=>new Promise(done=>{resolve=done}));app.refresh();await flush();
    app.confirm();await flush();resolve({ok:true,json:async()=>state()});await flush();
    assert.equal(app.el('wallet-status').textContent,'SUCCESS');assert.equal(app.el('wallet-actions').hidden,true);
});
test('Local deadline disables payment and asks backend for expiry',async()=>{
    const app=setup();await flush();app.consent();app.server(state('EXPIRED'));app.advance(300001);app.tick();await flush();
    assert.equal(app.el('wallet-status').textContent,'EXPIRED');assert.equal(app.el('wallet-confirm').disabled,true);
});
test('Pasting a payment link rejects foreign origins and accepts the current HTTPS origin',async()=>{
    const app=setup({paymentId:''});await flush();app.el('wallet-link-input').value='https://evil.test/demo-wallet/pay/'+id+'#token='+token;
    app.el('wallet-import-form').fire('submit');assert.equal(app.location.assigned,undefined);
    app.el('wallet-link-input').value='https://g8.test/demo-wallet/pay/'+id+'#token='+token;
    app.el('wallet-import-form').fire('submit');assert.equal(app.location.assigned,'/demo-wallet/pay/'+id+'#token='+token);
});
test('Validation errors stay visible after synchronizing an unchanged pending payment',async()=>{
    const app=setup();await flush();app.consent();
    app.post(async()=>({ok:false,json:async()=>({message:'Số tiền không khớp'})}));
    app.confirm();await flush();assert.equal(app.el('wallet-message').hidden,false);
    assert.equal(app.el('wallet-message').textContent,'Số tiền không khớp');
});

test('Merchant hides QR and disables copying after backend reports an ended payment',async()=>{
    const app=setup({role:'merchant'});await flush();assert.equal(app.el('wallet-qr-visual').hidden,false);
    app.server(state('EXPIRED'));app.refresh();await flush();
    assert.equal(app.el('wallet-qr-visual').hidden,true);assert.equal(app.el('wallet-copy').disabled,true);
});
