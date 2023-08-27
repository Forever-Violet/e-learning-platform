package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description 接收支付结果
 */
@Slf4j
@Service
public class ReceivePayNotifyService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    MyCourseTablesService myCourseTablesService;

    // 监听消息队列接收支付结果通知
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 获取消息, json数据转成mqMessage对象
        MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
        log.debug("学习中心服务接收支付结果:{}", mqMessage);

        // 消息类型
        String messageType = mqMessage.getMessageType();
        // 订单类型, 60201 表示购买课程
        String businessKey2 = mqMessage.getBusinessKey2();
        // 这里只处理支付结果通知, 消息类型匹配 以及 业务类型匹配
        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(businessKey2)) {
            // 选课记录id
            String chooseCourseId = mqMessage.getBusinessKey1();
            // 添加选课 (更改选课记录状态)
            boolean b = myCourseTablesService.saveChooseCourseSuccess(chooseCourseId);
            if (!b) {
                // 添加选课失败，抛出异常，消息重回丢列
                XueChengPlusException.cast("收到支付结果，添加选课失败");
            }
        }


    }

}
