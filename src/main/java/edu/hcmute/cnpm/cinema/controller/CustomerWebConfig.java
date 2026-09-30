package edu.hcmute.cnpm.cinema.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Gắn bộ chặn quyền khách hàng cho toàn bộ khu vực hỗ trợ phía khách. */
@Configuration
public class CustomerWebConfig implements WebMvcConfigurer {

    private final CustomerAccessInterceptor customerAccessInterceptor;

    public CustomerWebConfig(CustomerAccessInterceptor customerAccessInterceptor) {
        this.customerAccessInterceptor = customerAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(customerAccessInterceptor).addPathPatterns("/ho-tro/**");
    }
}
