package com.prashant.api.ecom.ducart.global_config;

import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class PasswordResetConfig {
    @Bean
    public Clock passwordResetClock() { return Clock.systemUTC(); }
}
