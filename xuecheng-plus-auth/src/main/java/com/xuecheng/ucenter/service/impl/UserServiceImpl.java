package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    ApplicationContext applicationContext; //注意这里导入的包是spring框架的ApplicationContext
//    @Autowired
//    AuthService authService;

    /**
     * @description 根据账号查询用户信息组成用户身份信息
     * @param s AuthParamsDto类型的json数据
     * @return UserDetails
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        AuthParamsDto authParamsDto = null;
        try {
            // 将认证参数转为AuthParamsDto类型
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            log.info("认证请求不符合项目要求:{}", s);
            throw new RuntimeException("认证请求数据格式不正确");
        }

        // 认证方法, 根据认证类型的字段的值, 去spring容器中找到对应的实现了AuthService接口的service方法 Bean
        String authType = authParamsDto.getAuthType();
        AuthService authService = applicationContext.getBean(authType + "_authservice", AuthService.class);
        // 认证
        XcUserExt user = authService.execute(authParamsDto);

        return getUserPrincipal(user);
    }

    /**
     * @description 查询用户信息
     * @param user
     * @return
     */
    private UserDetails getUserPrincipal(XcUserExt user) {
        // 用户权限, 如果不加会报Cannot pass a null GrantedAuthority collection
        String[] authorities = {"p1"};
        String password = user.getPassword();
        // 为了安全在令牌中不放密码
        user.setPassword(null);
        // 将user对象转为json
        String userString = JSON.toJSONString(user);
        // 创建UserDetails对象
        UserDetails userDetails = User.withUsername(userString).password(password).authorities(authorities).build();
        return userDetails;

    }
}
