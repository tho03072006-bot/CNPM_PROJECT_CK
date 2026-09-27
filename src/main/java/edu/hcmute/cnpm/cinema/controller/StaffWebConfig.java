package edu.hcmute.cnpm.cinema.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Gắn bộ chặn quyền cho khu vực nhân viên, tách riêng khỏi AdminWebConfig. */
@Configuration
public class StaffWebConfig implements WebMvcConfigurer {
    private final StaffAccessInterceptor staffAccessInterceptor;

    public StaffWebConfig(StaffAccessInterceptor staffAccessInterceptor) {
        this.staffAccessInterceptor = staffAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(staffAccessInterceptor).addPathPatterns("/nhan-vien/**");
    }
}
