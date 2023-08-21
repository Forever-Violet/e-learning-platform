package com.xuecheng.checkcode.service.impl;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.checkcode.model.CheckCodeParamsDto;
import com.xuecheng.checkcode.model.CheckCodeResultDto;
import com.xuecheng.checkcode.service.AbstractCheckCodeService;
import com.xuecheng.checkcode.service.CheckCodeService;
import com.xuecheng.checkcode.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
@Service("SmsOrEmailCheckCodeService")
public class SmsOrEmailCheckCodeServiceImpl  extends AbstractCheckCodeService implements CheckCodeService {

    @Resource(name="NumberLetterCheckCodeGenerator")
    @Override
    public void setCheckCodeGenerator(CheckCodeGenerator checkCodeGenerator) {
        this.checkCodeGenerator = checkCodeGenerator;
    }

    @Resource(name="UUIDKeyGenerator")
    @Override
    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }


    @Resource(name="RedisCheckCodeStore")
    @Override
    public void setCheckCodeStore(CheckCodeStore checkCodeStore) {
        this.checkCodeStore = checkCodeStore;
    }

    @Override
    public CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto){
        // 生成验证码, GenerateResult 包括 key 和 验证码         这里生成了6位的验证码, 过期时间300秒
        GenerateResult generate = generate(checkCodeParamsDto, 6, "checkcode:", 300);
        String phoneOrEmail = checkCodeParamsDto.getParam1();

        // 判断是该参数是手机号还是邮箱
        if (CommonUtil.checkPhone(phoneOrEmail)) { //如果是手机号
            // pass
        } else if (CommonUtil.checkEmail(phoneOrEmail)) { //如果是邮箱
            final String myEmail = "1250812574@qq.com";
            final String password = "baejolqecrgtidih";

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.transport.protocol", "smtp"); // 连接协议
            props.put("mail.smtp.starttls.enable", "true"); // 设置是否使用ssl安全连接 ---一般都使用
            props.put("mail.smtp.host", "smtp.qq.com"); //主机名
            props.put("mail.smtp.port", "587"); //端口号
            props.put("mail.debug", "true");// 设置是否显示debug信息 true 会在控制台显示相关信息
            try {
                // 得到会话对象
                Session session = Session.getInstance(props);
                // 获取邮件对象
                Message message = new MimeMessage(session);
                // 设置发件人邮箱地址
                message.setFrom(new InternetAddress(myEmail));
                // 设置收件人邮箱地址
                message.setRecipients(Message.RecipientType.TO, new InternetAddress[]{new InternetAddress(phoneOrEmail)});
                // 设置邮件标题
                message.setSubject("[学成在线]找回您的密码:");
                // 设置邮件内容
                message.setText("尊敬的用户您好,您的验证码是:["+ generate.getCode() +"]。");//这是我们的邮件要发送的信息内容
                // 得到邮差对象
                Transport transport = session.getTransport();
                // 连接自己的邮箱账户
                transport.connect(myEmail, password);// 密码为QQ邮箱开通的stmp服务后得到的客户端授权码,输入自己的即可
                // 发送邮件
                transport.sendMessage(message, message.getAllRecipients());
                transport.close();
            } catch (Exception e) {
                log.error("发送邮件失败, 异常:{}", e.getMessage());
                e.printStackTrace();
            }
        } else {
            throw new XueChengPlusException("手机号或邮箱不正确!");
        }

        CheckCodeResultDto checkCodeResultDto = new CheckCodeResultDto();
        // 设置验证码在redis中的key
        checkCodeResultDto.setKey(generate.getKey());
        // 非图片验证码外都是null
        checkCodeResultDto.setAliasing(null);
        return checkCodeResultDto;
    }

    @Override
    public GenerateResult generate(CheckCodeParamsDto checkCodeParamsDto, Integer code_length, String keyPrefix, Integer expire) {
        return super.generate(checkCodeParamsDto, code_length, keyPrefix, expire);
    }
}
