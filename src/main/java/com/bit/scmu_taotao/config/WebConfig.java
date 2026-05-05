package com.bit.scmu_taotao.config;

import com.bit.scmu_taotao.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * 注册并配置拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns(
                        "/login",           // 登录接口放行
                        "/admin/login",     // 管理员登录接口放行
                        "/error",           // 错误页面放行
                        "/static/**",       // 静态资源放行
                        "/favicon.ico",     // 图标放行
                        "/ws/**",           // WebSocket 握手路径放行，由握手拦截器鉴权
                        "/test-only/auth/**" // test profile 下用于联调的临时发 token 接口
                );
    }
}
