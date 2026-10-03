package edu.hcmute.cnpm.cinema.config;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
class DemoWalletGatewayTest {
    private static final String ID="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    @ParameterizedTest @ValueSource(strings={"/","/ve-cua-toi","/thanh-toan/1","/staff","/admin","/dang-nhap","/demo-wallet/api/"+ID+"/confirm",
        "/demo-wallet/pay/bad-id","/demo-wallet/pay/"+ID+"/..","/demo-wallet/%2e%2e/admin","/demo-wallet;param=1","//demo-wallet"})
    void blocksNonWalletPages(String path){assertThat(DemoWalletGateway.allowed("GET",path)).isFalse();}
    @Test void forwardsOnlyWalletResourcesAndSupportedMethods(){
        for(String path:new String[]{"/demo-wallet","/demo-wallet/health","/demo-wallet/pay/"+ID,"/css/style.css","/js/demo-wallet.js","/js/money.js","/images/favicon.svg"})
            assertThat(DemoWalletGateway.allowed("GET",path)).isTrue();
        for(String action:new String[]{"status","confirm","cancel"}){
            assertThat(DemoWalletGateway.allowed("POST","/demo-wallet/api/"+ID+"/"+action)).isTrue();
            assertThat(DemoWalletGateway.allowed("DELETE","/demo-wallet/api/"+ID+"/"+action)).isFalse();
        }
    }
    @Test void permitsOnlySingleBoundedAssetVersionQuery(){
        for(String path: new String[]{"/css/style.css","/js/money.js","/js/demo-wallet.js","/images/favicon.svg"})
            assertThat(DemoWalletGateway.allowed("GET",path,"v=20261003-money-1")).isTrue();
        for(String path: new String[]{"/demo-wallet","/demo-wallet/health","/demo-wallet/pay/"+ID,"/demo-wallet/api/"+ID+"/confirm"})
            assertThat(DemoWalletGateway.allowed("GET",path,"v=20261003-money-1")).isFalse();
        assertThat(DemoWalletGateway.allowed("POST","/demo-wallet/api/"+ID+"/confirm","v=1")).isFalse();
        assertThat(DemoWalletGateway.allowed("GET","/js/money.js",null)).isTrue();
        assertThat(DemoWalletGateway.allowed("GET","/js/money.js","v="+"a".repeat(81))).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"", "v=", "v=1&v=2", "v=1&next=/admin", "token=secret", "next=/admin",
            "v=../admin", "v=%31", "v=has+space", "v=%0d%0aheader"})
    void rejectsAmbiguousOrUnrelatedAssetQuery(String query){
        assertThat(DemoWalletGateway.allowed("GET","/js/money.js",query)).isFalse();
    }
    @Test void restrictsActualProxyAndDoesNotForwardAuthorization() throws Exception{
        AtomicInteger requests=new AtomicInteger();AtomicReference<String> nonce=new AtomicReference<>(),authorization=new AtomicReference<>(),query=new AtomicReference<>();
        HttpServer backend=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        backend.createContext("/",exchange->{
            requests.incrementAndGet();nonce.set(exchange.getRequestHeaders().getFirst("X-Demo-Wallet-CSRF"));
            query.set(exchange.getRequestURI().getRawQuery());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body="{\"status\":\"PENDING\"}".getBytes();exchange.getResponseHeaders().add("Set-Cookie","JSESSIONID=phone; Path=/; HttpOnly");
            exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });backend.start();
        var gateway=new DemoWalletGateway(true,0,backend.getAddress().getPort(),new DemoWalletSettings(true,"https://g8.test"),"0.0.0.0");
        try{
            gateway.start();URI base=URI.create("http://127.0.0.1:"+gateway.actualPort());HttpClient client=HttpClient.newHttpClient();
            var blocked=client.send(HttpRequest.newBuilder(base.resolve("/ve-cua-toi")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(blocked.statusCode()).isEqualTo(404);assertThat(requests.get()).isZero();
            var blockedQuery=client.send(HttpRequest.newBuilder(base.resolve("/demo-wallet?next=/admin")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(blockedQuery.statusCode()).isEqualTo(404);assertThat(requests.get()).isZero();
            var result=client.send(HttpRequest.newBuilder(base.resolve("/demo-wallet/api/"+ID+"/status"))
                .header("Content-Type","application/json").header("X-Demo-Wallet-CSRF","phone-nonce")
                .header("Authorization","Bearer should-not-pass").POST(HttpRequest.BodyPublishers.ofString("{\"token\":\"demo\"}")).build(),HttpResponse.BodyHandlers.ofString());
            assertThat(result.statusCode()).isEqualTo(200);assertThat(nonce.get()).isEqualTo("phone-nonce");
            assertThat(authorization.get()).isNull();assertThat(result.headers().firstValue("Set-Cookie")).hasValue("JSESSIONID=phone; Path=/; HttpOnly; Secure; SameSite=Lax");
            var oversized=client.send(HttpRequest.newBuilder(base.resolve("/demo-wallet/api/"+ID+"/status"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("X".repeat(4097))).build(),HttpResponse.BodyHandlers.ofString());
            assertThat(oversized.statusCode()).isEqualTo(413);assertThat(requests.get()).isEqualTo(1);
            var asset=client.send(HttpRequest.newBuilder(base.resolve("/js/money.js?v=20261003-money-1")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(asset.statusCode()).isEqualTo(200);assertThat(query.get()).isEqualTo("v=20261003-money-1");
            assertThat(requests.get()).isEqualTo(2);
            var invalidAsset=client.send(HttpRequest.newBuilder(base.resolve("/js/money.js?v=1&next=/admin")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(invalidAsset.statusCode()).isEqualTo(404);assertThat(requests.get()).isEqualTo(2);
        }finally{gateway.stop();backend.stop(0);}
    }
}
