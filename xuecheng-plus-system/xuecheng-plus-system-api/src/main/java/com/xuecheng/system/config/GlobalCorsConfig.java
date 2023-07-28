package com.xuecheng.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域过滤器
 * 判断是否跨域请求，同源策略是浏览器的一种安全机制，
 * 从一个地址请求另一个地址，如果协议、主机、端口三者全部一致则不属于跨域，否则有一个不一致就是跨域请求。
 */

@Configuration
public class GlobalCorsConfig {

    /**
     * 允许跨域调用的过滤器, 此配置类实现了跨域过虑器，在响应头添加Access-Control-Allow-Origin。
     */
    @Bean
    public CorsFilter corsFilter(){
        CorsConfiguration config = new CorsConfiguration();
        // 允许白名单域名进行跨域调用, *表示所有
        config.addAllowedOrigin("*");
        // 允许跨域发送cookie
        config.setAllowCredentials(true);
        // 放行全部原始头信息
        config.addAllowedHeader("*");
        // 允许所有请求方法跨域调用
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }




}


/**
 * 解决跨域的方法：
 * 1、JSONP
 * 通过script标签的src属性进行跨域请求，如果服务端要响应内容则首先读取请求参数callback的值，callback是一个回调函数的名称，服务端读取callback的值后将响应内容通过调用callback函数的方式告诉请求方。如下图：
 *
 *
 * 2、添加响应头
 * 服务端在响应头添加 Access-Control-Allow-Origin：*
 *
 * 3、通过nginx代理跨域
 * 由于服务端之间没有跨域，浏览器通过nginx去访问跨域地址。
 *
 * 1）浏览器先访问http://192.168.101.10:8601 nginx提供的地址，进入页面
 * 2）此页面要跨域访问http://192.168.101.11:8601 ，不能直接跨域访问http://www.baidu.com:8601  ，而是访问nginx的一个同源地址，比如：http://192.168.101.11:8601/api ，通过http://192.168.101.11:8601/api 的代理去访问http://www.baidu.com:8601。
 * 这样就实现了跨域访问。
 * 浏览器到http://192.168.101.11:8601/api 没有跨域
 * nginx到http://www.baidu.com:8601通过服务端通信，没有跨域。
 */