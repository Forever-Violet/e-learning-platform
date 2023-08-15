package com.xuecheng.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.util.Collections;

/**
 * @author Administrator
 * @version 1.0
 **/
@Configuration
public class TokenConfig {

    private final String SIGNING_KEY = "mq123";

    @Autowired
    TokenStore tokenStore;

/*    @Bean
    public TokenStore tokenStore() {
        //使用内存存储令牌（普通令牌）
        return new InMemoryTokenStore();
    }*/

    @Autowired
    private JwtAccessTokenConverter accessTokenConverter;

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(SIGNING_KEY); //设置密钥
        return converter;
    }

    //令牌管理服务
    @Bean(name="authorizationServerTokenServicesCustom")
    public AuthorizationServerTokenServices tokenService() {
        DefaultTokenServices service=new DefaultTokenServices();
        service.setSupportRefreshToken(true);//支持刷新令牌
        service.setTokenStore(tokenStore);//令牌存储策略

        // 令牌增强链
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        // 将accessTokenConverter作为增强处理器添加到链中。
        tokenEnhancerChain.setTokenEnhancers(Collections.singletonList(accessTokenConverter));
        // 可以实现对令牌的多个增强操作同时进行。这样，生成的令牌就会包含更多有用的信息
        service.setTokenEnhancer(tokenEnhancerChain);

        service.setAccessTokenValiditySeconds(7200); // 令牌默认有效期2小时
        service.setRefreshTokenValiditySeconds(259200); // 刷新令牌默认有效期3天
        return service;
    }


}
