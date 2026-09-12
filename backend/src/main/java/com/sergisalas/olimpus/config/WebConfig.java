package com.sergisalas.olimpus.config;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccountResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentAccountResolver currentAccountResolver;

    public WebConfig(CurrentAccountResolver currentAccountResolver) {
        this.currentAccountResolver = currentAccountResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAccountResolver);
    }
}
