package com.xuecheng.ucenter.model.dto;

import lombok.Data;

@Data
public class FindPwdDto {

    private String password; //密码
    private String confirmpwd; //确认密码
    private String cellphone;//手机号
    private String email;   //邮箱
    private String checkcode;//验证码
    private String checkcodekey;//验证码key

}
