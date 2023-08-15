package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CheckCodeClient checkCodeClient;


    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

        // 校验验证码
        // 用户输入的验证码
        String checkcode = authParamsDto.getCheckcode();
        // 根据key去redis缓存中找对正确的验证码checkCode
        String checkcodeKey = authParamsDto.getCheckcodekey();
        if (StringUtils.isBlank(checkcodeKey) || StringUtils.isBlank(checkcode)) {
            throw new RuntimeException("验证码为空");
        }
        // 远程调用验证码服务的验证方法
        Boolean verify = checkCodeClient.verify(checkcodeKey, checkcode);
        if (!verify) {
            throw new RuntimeException("验证码输入错误");
        }

        // 账号
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            // 返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        // 复制数据
        BeanUtils.copyProperties(user, xcUserExt);
        // 校验密码
        // 取出数据库存储的正确密码
        String passwordDb = user.getPassword();
        // 用户在表单中输入的密码
        String passwordForm = authParamsDto.getPassword();
        boolean matches = passwordEncoder.matches(passwordForm, passwordDb);
        if (!matches) {
            throw new RuntimeException("账号或密码错误");
        }
        return xcUserExt;
    }
}
