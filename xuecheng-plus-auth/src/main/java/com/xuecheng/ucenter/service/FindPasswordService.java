package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.dto.FindPwdDto;

/**
 * 找回密码service
 */
public interface FindPasswordService {

    /**
     * @description 找回密码
     * @param findPwdDto 找回密码参数
     */
    void findPassword(FindPwdDto findPwdDto);
}
