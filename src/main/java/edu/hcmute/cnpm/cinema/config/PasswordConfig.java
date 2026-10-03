package edu.hcmute.cnpm.cinema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bộ mã hoá mật khẩu dùng chung cho cả dự án.
 *
 * Dùng BCrypt vì nó tự sinh muối ngẫu nhiên cho từng mật khẩu và cố tình chạy
 * chậm, nên kể cả database bị lộ thì dò ngược ra mật khẩu gốc vẫn rất tốn kém.
 * Tuyệt đối không lưu mật khẩu thô vào cột {@code password_hash}.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
