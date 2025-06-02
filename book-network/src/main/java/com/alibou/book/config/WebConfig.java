package com.alibou.book.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource resolveResourceInternal(
                            HttpServletRequest request,
                            String requestPath,
                            List<? extends Resource> locations,
                            ResourceResolverChain chain) {

                        try {
                            // Try to find the resource normally
                            Resource resource = super.resolveResourceInternal(request, requestPath, locations, chain);

                            // If resource not found and path doesn't start with api,
                            // return the Angular index.html for client-side routing
                            if (resource == null && !requestPath.startsWith("api/")) {
                                return new ClassPathResource("/static/index.html");
                            }

                            return resource;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                });
    }
}