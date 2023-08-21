package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.dto.FindPwdDto;
import com.xuecheng.ucenter.service.FindPasswordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "找回密码服务接口")
@Slf4j
@RestController
public class FindPasswordController {

    @Autowired
    FindPasswordService findPasswordService;

    @ApiOperation("找回密码接口")
    @PostMapping("/findpassword")
    public void findPassword(@RequestBody FindPwdDto findPwdDto) {
        findPasswordService.findPassword(findPwdDto);
    }

}
