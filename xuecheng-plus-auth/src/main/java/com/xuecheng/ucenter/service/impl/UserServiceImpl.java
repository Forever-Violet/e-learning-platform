package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    ApplicationContext applicationContext; //注意这里导入的包是spring框架的ApplicationContext

    @Autowired
    XcMenuMapper xcMenuMapper; //用来查询权限
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
        // 根据UserDetails对象生成令牌
        return getUserPrincipal(user);
    }

    /**
     * @description 查询用户信息
     * @param user
     * @return
     */
    private UserDetails getUserPrincipal(XcUserExt user) {
        // 查询用户权限, 如果不加会报Cannot pass a null GrantedAuthority collection
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(user.getId());
        List<String> permissions = new ArrayList<>();
        if (xcMenus.size() == 0) {
            // 用户权限, 如果不加会报Cannot pass a null GrantedAuthority collection, 没有也得加
            permissions.add("p1");
        } else { // 查询到了权限
            xcMenus.forEach(menu -> { //遍历权限(菜单)列表
                permissions.add(menu.getCode()); //将权限添加到列表
            });
        }
        // 将用户权限放在XcUserExt对象中
        user.setPermissions(permissions);

        String password = user.getPassword();
        // 为了安全在令牌中不放密码
        user.setPassword(null);
        // 将user对象转为json
        String userString = JSON.toJSONString(user);
        // 创建UserDetails对象
        UserDetails userDetails = User.withUsername(userString)
                .password(password)
                .authorities(permissions.toArray(new String[0])) // 将 permissions 转换为一个 String 数组, 由于 toArray 方法会根据 List 的大小自动创建一个足够容纳全部元素的新数组，因此可以传入一个长度为 0 的空数组，这样就能够确保创建一个恰好能够容纳所有元素的新数组。
                .build();
        return userDetails;

    }
}
