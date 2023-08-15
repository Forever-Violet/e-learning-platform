package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @description 微信扫码认证
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    XcUserRoleMapper xcUserRoleMapper;

    @Autowired
    WxAuthService currentProxy;

    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;

    @Override
    public XcUser wxAuth(String code) {
        // 收到code 调用微信接口申请access_token
        Map<String, String> access_token_map = getAccess_token(code);
        if (access_token_map == null) {
            return null;
        }
        System.out.println(access_token_map);
        String openid = access_token_map.get("openid");
        String access_token = access_token_map.get("access_token");
        // 拿到access_token去查询用户信息
        Map<String, String> userinfo = getUserinfo(access_token, openid);
        if (userinfo == null) {
            return null;
        }
        // 添加用户到数据库, 使用代理对象调用本类的事务方法, 防止事务失效
        XcUser xcUser = currentProxy.addWxUser(userinfo);
        return xcUser;

    }

    @Transactional
    public XcUser addWxUser(Map<String, String> userInfo_map) {
        String unionid = userInfo_map.get("unionid").toString();
        // 根据unionid查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if (xcUser != null) { //如果该微信用户曾经登录过
            return xcUser;//直接返回
        }
        String userId = UUID.randomUUID().toString();
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        // 记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001"); //学生类型
        xcUser.setStatus("1"); //用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        // 插入数据库
        xcUserMapper.insert(xcUser);

        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17"); //学生角色
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;

    }


    /**
     * @description 申请访问令牌, 响应示例:
     *      {
     *      "access_token":"ACCESS_TOKEN",
     *      "expires_in":7200,
     *      "refresh_token":"REFRESH_TOKEN",
     *      "openid":"OPENID",
     *      "scope":"SCOPE",
     *      "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     *      }
     * @param code 授权码
     * @return access_token_map 存有access_token和openid等信息的map
     */
    private Map<String, String> getAccess_token(String code) {

        String wxUrl_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        // 请求微信地址 , 装填数据
        String wxUrl = String.format(wxUrl_template, appid, secret, code);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);

        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);

        String result = exchange.getBody();
        log.info("调用微信接口申请access_token: 返回值:{}", result);
        // json体转成map
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);
        return resultMap;

    }

    /**
     * @description 使用accessToken和openid向微信接口获取用户信息, 下面是一个响应示例:
     *      {
         *      "openid":"OPENID",
         *      "nickname":"NICKNAME",
         *      "sex":1,
         *      "province":"PROVINCE",
         *      "city":"CITY",
         *      "country":"COUNTRY",
         *      "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
         *      "privilege":[
         *      "PRIVILEGE1",
         *      "PRIVILEGE2"
         *      ],
         *      "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     *      }
     * @param accessToken
     * @param openid
     * @return 存有用户信息的map
     */
    private Map<String, String> getUserinfo(String accessToken, String openid) {
        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        // 请求微信地址
        String wxUrl = String.format(wxUrl_template, accessToken, openid);
        log.info("调用微信接口获取用户信息, url:{}", wxUrl);

        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);

        // 防止乱码进行转码
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        log.info("调用微信接口获取用户信息, 返回值:{}", result);
        // 转为map类型
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);
        return resultMap;

    }


    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        // 账号
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            // 返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }

}
