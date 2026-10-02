package edu.hcmute.cnpm.cinema.booking;
import edu.hcmute.cnpm.cinema.config.DemoWalletSettings;
import edu.hcmute.cnpm.cinema.exception.BusinessException;
import edu.hcmute.cnpm.cinema.service.DemoWalletSecurity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;
class DemoWalletSecurityTest {
    @ParameterizedTest @ValueSource(strings={"https://group8.test","https://group8.test:8443/","http://localhost:8082","http://127.0.0.1:8082","http://[::1]:8082"})
    void acceptsHttpsOrLoopback(String url){assertThat(new DemoWalletSettings(true,url).getPublicBaseUrl()).isNotBlank();}
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"http://group8.test","https://group8.test/path","https://group8.test/demo-wallet/pay/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","https://group8.test/%64emo-wallet","https://group8.test?x=y","https://group8.test#token=x","https://user:pass@group8.test","https://group8.test:0","https://group8.test:99999","javascript:alert(1)","//group8.test"})
    void rejectsUnsafeOrigins(String url){assertThatThrownBy(()->new DemoWalletSettings(true,url).getPublicBaseUrl()).isInstanceOf(BusinessException.class);}
    @ParameterizedTest @ValueSource(strings={"http://localhost:8082","https://localhost","https://127.0.0.2","https://wallet.localhost","https://0.0.0.0"})
    void publicModeRejectsLinksThatCannotOpenOnAnotherDevice(String url){
        assertThatThrownBy(()->new DemoWalletSettings(true,url,true).getPublicBaseUrl())
                .isInstanceOf(BusinessException.class).hasMessageContaining("HTTPS công khai");
    }
    @Test void publicModeAcceptsRenderHttpsOrigin(){
        assertThat(new DemoWalletSettings(true,"https://momo-g8.onrender.com/",true).getPublicBaseUrl())
                .isEqualTo("https://momo-g8.onrender.com");
    }
    @ParameterizedTest @ValueSource(strings={"https://momo-g8.onrender.com/demo-wallet","https://momo-g8.onrender.com/demo-wallet/","https://momo-g8.onrender.com/demo-wallet///"," https://momo-g8.onrender.com/demo-wallet/ "})
    void normalizesCopiedWalletPageUrlBeforeBuildingQr(String url){
        assertThat(new DemoWalletSettings(true,url,true).getPublicBaseUrl())
                .isEqualTo("https://momo-g8.onrender.com");
    }
    @Test void localTestingAlsoAcceptsWalletPagePath(){
        assertThat(new DemoWalletSettings(true,"http://localhost:8082/demo-wallet/").getPublicBaseUrl())
                .isEqualTo("http://localhost:8082");
    }
    @Test void disabledWalletCannotBeUsed(){assertThatThrownBy(()->new DemoWalletSettings(false,"https://group8.test").requireEnabled()).isInstanceOf(BusinessException.class);}
    @Test void tokensAreRandomAndOnlyTheirHashIsStored(){
        var security=new DemoWalletSecurity();String first=security.newToken(),second=security.newToken();
        assertThat(first).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(second);
        assertThat(security.hash(first)).matches("[a-f0-9]{64}").doesNotContain(first);
        security.requireToken(first,security.hash(first));
        assertThatThrownBy(()->security.requireToken(second,security.hash(first))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->security.requireCsrf(first,second)).isInstanceOf(BusinessException.class);
    }
}
