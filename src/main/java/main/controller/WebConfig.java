package main.controller;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${configs.webInterfacePath}")
    private String urlPath;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController(urlPath).setViewName("/index.html");
        registry.addRedirectViewController("/", urlPath);
    }
}
