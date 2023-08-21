package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.FindPwdDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.FindPasswordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class FindPasswordServiceImpl implements FindPasswordService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    CheckCodeClient checkCodeClient;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void findPassword(FindPwdDto findPwdDto) {
        // 校验用户输入的两次密码是否一致
        if(!findPwdDto.getPassword().equals(findPwdDto.getConfirmpwd())) {
            // 若不一致
            throw new RuntimeException("两次输入密码不一致，请重新输入!");
        }

        // 校验验证码
        // 用户输入的验证码
        String user_checkCode = findPwdDto.getCheckcode();
        // 验证码的key
        String checkCodeKey = findPwdDto.getCheckcodekey();
        // 调用验证码服务的远程接口校验验证码
        boolean verify = checkCodeClient.verify(checkCodeKey, user_checkCode);
        if (!verify) {
            XueChengPlusException.cast("验证码错误!");
        }

        // 这里使用这样的查询条件是为了防止输入的手机号码和邮箱不是属于一个用户的, 即如果查出了两个用户, 那肯定不对
        LambdaQueryWrapper<XcUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(StringUtils.isNotEmpty(findPwdDto.getCellphone()), XcUser::getCellphone, findPwdDto.getCellphone())
                .or()
                .eq(StringUtils.isNotEmpty(findPwdDto.getEmail()), XcUser::getEmail, findPwdDto.getEmail());
        List<XcUser> xcUsers = xcUserMapper.selectList(lambdaQueryWrapper);
        if (xcUsers.size() > 1) {
            XueChengPlusException.cast("手机号与邮箱所属用户不同，请重新检查，或只填写一个验证参数!");
        }
        XcUser xcUser = new XcUser();
        xcUser.setPassword(passwordEncoder.encode(findPwdDto.getPassword())); //将密码加密, 并更新密码
        // 这里可以继续使用上面构造的查询条件, 因为能到这一步已经保证了只有一条记录被查到
        xcUserMapper.update(xcUser, lambdaQueryWrapper);

    }
}
