package com.example.library.config;

import com.example.library.util.PasswordUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 密码加密器配置。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordUtil passwordUtil() {
        return new PasswordUtil();
    }
}
