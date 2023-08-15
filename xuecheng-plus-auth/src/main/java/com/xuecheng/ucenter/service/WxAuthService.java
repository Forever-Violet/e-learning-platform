package com.xuecheng.ucenter.service;


import com.xuecheng.ucenter.model.po.XcUser;

import java.util.Map;

/**
 * @description 微信认证接口
 */
public interface WxAuthService {

    public XcUser wxAuth(String code);

    XcUser addWxUser(Map<String, String> userinfo);
}
