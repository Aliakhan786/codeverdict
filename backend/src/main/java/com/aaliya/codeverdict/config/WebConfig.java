package com.aaliya.codeverdict.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final DemoReadOnlyInterceptor demoReadOnly;

    public WebConfig(DemoReadOnlyInterceptor demoReadOnly) {
        this.demoReadOnly = demoReadOnly;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoReadOnly).addPathPatterns("/api/v1/**");
    }
}
