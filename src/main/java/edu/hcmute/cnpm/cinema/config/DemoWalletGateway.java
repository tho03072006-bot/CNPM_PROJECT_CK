package edu.hcmute.cnpm.cinema.config;
import com.sun.net.httpserver.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** © Nhóm 8. Cổng điện thoại chỉ công khai ví, không công khai web rạp/quản trị. */
@Component
public class DemoWalletGateway implements SmartLifecycle {
    private final boolean enabled;
    private final int port;
    private final String address;
    private final URI upstream;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private HttpServer server;
    private ExecutorService executor;
    private final DemoWalletSettings settings;
    @org.springframework.beans.factory.annotation.Autowired
    public DemoWalletGateway(@Value("${demo-wallet.gateway-enabled:false}") boolean enabled,
            @Value("${demo-wallet.gateway-port:8084}") int port,
            @Value("${server.port:8082}") int appPort,DemoWalletSettings settings,
            @Value("${demo-wallet.gateway-address:127.0.0.1}") String address){
        if (!Set.of("127.0.0.1", "0.0.0.0").contains(address)) throw new IllegalArgumentException("Dia chi cong vi khong hop le.");
        this.address=address;this.enabled=enabled;this.port=port;this.upstream=URI.create("http://127.0.0.1:"+appPort);this.settings=settings;
    }
    public DemoWalletGateway(boolean enabled,int port,int appPort,DemoWalletSettings settings){
        this(enabled,port,appPort,settings,"127.0.0.1");
    }
    static boolean allowed(String method,String path){
        String uuid="[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";
        if("GET".equals(method))return Set.of("/demo-wallet","/demo-wallet/health","/css/style.css","/js/demo-wallet.js","/js/money.js","/images/favicon.svg").contains(path)
                || path.matches("/demo-wallet/pay/"+uuid);
        return "POST".equals(method)&&path.matches("/demo-wallet/api/"+uuid+"/(status|confirm|cancel)");
    }
    @Override public synchronized void start(){
        if(!enabled||server!=null)return;
        settings.requireEnabled();
        try{
            server=HttpServer.create(new InetSocketAddress(address,port),64);
            executor=new ThreadPoolExecutor(4,8,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),
                    runnable->{Thread thread=new Thread(runnable,"demo-wallet-gateway");thread.setDaemon(true);return thread;});
            server.setExecutor(executor);server.createContext("/",this::handle);server.start();
        }catch(IOException exception){stop();throw new IllegalStateException("Không mở được cổng ví điện thoại "+port,exception);}
    }
    private void handle(HttpExchange exchange)throws IOException{
        try(exchange){
            URI uri=exchange.getRequestURI();String path=uri.getRawPath(),method=exchange.getRequestMethod();
            if(uri.getRawQuery()!=null||!allowed(method,path)){reply(exchange,404,"Không có trang này trên ví giả lập.");return;}
            byte[] body=exchange.getRequestBody().readNBytes(4097);
            if(body.length>4096){reply(exchange,413,"Nội dung yêu cầu quá dài.");return;}
            if("GET".equals(method)&&body.length>0){reply(exchange,400,"Yêu cầu không hợp lệ.");return;}
            HttpRequest.Builder request=HttpRequest.newBuilder(upstream.resolve(path)).timeout(Duration.ofSeconds(12))
                    .header("X-Forwarded-Proto","https");
            for(String header:List.of("Accept","Content-Type","Cookie","X-Demo-Wallet-CSRF","X-Requested-With")){
                String value=exchange.getRequestHeaders().getFirst(header);
                if(value!=null&&value.length()<=8192)request.header(header,value);
            }
            if("POST".equals(method)){
                String contentType=exchange.getRequestHeaders().getFirst("Content-Type");
                if(contentType==null||!contentType.split(";")[0].strip().equalsIgnoreCase("application/json")){
                    reply(exchange,415,"Ví chỉ nhận yêu cầu JSON.");return;
                }
                request.POST(HttpRequest.BodyPublishers.ofByteArray(body));
            }else request.GET();
            try{
                HttpResponse<byte[]> response=client.send(request.build(),HttpResponse.BodyHandlers.ofByteArray());
                if(response.statusCode()>=300&&response.statusCode()<400){reply(exchange,502,"Ví chưa sẵn sàng. Vui lòng tải lại.");return;}
                for(String header:List.of("Content-Type","Cache-Control","Content-Security-Policy",
                        "X-Frame-Options","X-Content-Type-Options","Referrer-Policy","X-Robots-Tag"))
                    response.headers().allValues(header).forEach(value->exchange.getResponseHeaders().add(header,value));
                for(String cookie:response.headers().allValues("Set-Cookie")){
                    String secured=secureCookie(cookie);
                    if(secured!=null)exchange.getResponseHeaders().add("Set-Cookie",secured);
                }
                exchange.sendResponseHeaders(response.statusCode(),response.body().length);
                exchange.getResponseBody().write(response.body());
            }catch(InterruptedException exception){Thread.currentThread().interrupt();reply(exchange,503,"Ví đang tạm dừng. Vui lòng thử lại.");}
            catch(IOException exception){reply(exchange,502,"Chưa kết nối được dịch vụ thanh toán. Vui lòng thử lại.");}
        }
    }
    /** TLS kết thúc tại Render hoặc Cloudflare; chỉ cookie ví ra Internet cần thêm Secure. */
    static String secureCookie(String cookie){
        if(!cookie.startsWith("JSESSIONID="))return null;
        Set<String> attributes=new HashSet<>();
        for(String part:cookie.split(";"))attributes.add(part.strip().toLowerCase(Locale.ROOT));
        String result=cookie;
        if(!attributes.contains("secure"))result+="; Secure";
        if(!attributes.contains("httponly"))result+="; HttpOnly";
        if(attributes.stream().noneMatch(value->value.startsWith("samesite=")))result+="; SameSite=Lax";
        return result;
    }
    private void reply(HttpExchange exchange,int status,String message)throws IOException{
        byte[] body=("{\"success\":false,\"message\":\""+message+"\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(status,body.length);
        exchange.getResponseBody().write(body);
    }
    @Override public synchronized void stop(){
        if(server!=null){server.stop(0);server=null;}
        if(executor!=null){executor.shutdownNow();executor=null;}
    }
    @Override public boolean isRunning(){return server!=null;}
    int actualPort(){return server.getAddress().getPort();}
}
