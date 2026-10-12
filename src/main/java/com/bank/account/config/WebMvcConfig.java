package com.bank.account.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ① 登录态校验:全站生效
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 前端页面 & 静态资源
                        "/",
                        "/login.html",
                        "/index.html",
                        "/admin.html",
                        "/admin",
                        "/assets/**",
                        "/favicon.ico",
                        // 鉴权接口
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh",
                        // Swagger / Knife4J
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**"
                );

        // ② 管理员校验:只挂在后台接口上,顺序在登录态校验之后
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addViewControllers(org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry) {
        // 根路径直接返回 SPA 主页,由前端 JS 判断是否已登录再决定跳转
        registry.addViewController("/").setViewName("forward:/index.html");
        // 后台管理控制台
        registry.addViewController("/admin").setViewName("forward:/admin.html");
        registry.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }
}