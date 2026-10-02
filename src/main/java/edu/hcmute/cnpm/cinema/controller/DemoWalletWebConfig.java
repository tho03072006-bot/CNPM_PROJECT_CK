package edu.hcmute.cnpm.cinema.controller;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
/** Không cache ví/QR; khoá QR không đi qua Referer. */
@Configuration
public class DemoWalletWebConfig implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new HandlerInterceptor(){
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
                response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");
                response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("X-Frame-Options","DENY");
                response.setHeader("X-Robots-Tag","noindex, nofollow");
                if(request.getRequestURI().startsWith("/demo-wallet"))
                    response.setHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
                return true;}
        }).addPathPatterns("/demo-wallet","/demo-wallet/**","/thanh-toan/demo/**");}
}
