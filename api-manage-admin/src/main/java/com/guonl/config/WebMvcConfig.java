package com.guonl.config;

import com.guonl.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：拦截器与跨域
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/logout"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 开发期 Vite devServer 代理，一般无需跨域；此处放开便于联调
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * SPA history 模式深链接兜底：非 /api/** 的前端路由刷新时 forward 到 index.html
     * 视图控制器优先级低于业务 @RequestMapping，不会与 REST 接口冲突
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/project").setViewName("forward:/index.html");
        registry.addViewController("/apis/{projectId:\\d+}").setViewName("forward:/index.html");
        registry.addViewController("/flow").setViewName("forward:/index.html");
        registry.addViewController("/flow/editor/{id:\\d+}").setViewName("forward:/index.html");
        registry.addViewController("/flow/run/{runId:\\d+}").setViewName("forward:/index.html");
    }
}
