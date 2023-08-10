package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.model.po.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @description 处理课程发布后产生的消息任务, 即将发布后的内容存储到各个存储系统
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    SearchServiceClient searchServiceClient;

    // 任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex(); //分片序号
        int shardTotal = XxlJobHelper.getShardTotal(); //分片总数

        log.debug("shardIndex="+shardIndex+", shardTotal="+shardTotal);
        // 参数: 分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex, shardTotal, "course_publish", 30, 60);
    }

    // 课程发布任务处理
    @Override
    public boolean execute(MqMessage mqMessage) {
        // 获取消息相关的业务信息
        String businessKey1 = mqMessage.getBusinessKey1();
        long courseId = Integer.parseInt(businessKey1);
        // 课程页面静态化
        generateCourseHtml(mqMessage, courseId);
        // 课程索引
        saveCourseIndex(mqMessage, courseId);
        // 课程缓存
        saveCourseCache(mqMessage, courseId);
        return true;
    }


    // 生成课程静态化页面并删除按到minIO文件系统
    private void generateCourseHtml(MqMessage mqMessage, long courseId) {
        log.debug("开始进行课程静态化, 课程id:{}", courseId);
        // 消息id
        Long id = mqMessage.getId();
        // 消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        // 消息幂等性处理
        int stageOne = mqMessageService.getStageOne(id);
        if (stageOne > 0) { // 如果任务1(页面静态化任务)的状态为已经完成
            log.debug("课程静态化已处理直接返回, 课程id:{}", courseId);
            return;
        }
        // 开始进行课程页面静态化
        // 生成静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);
        // 上传静态化页面
        if (file != null) {
            coursePublishService.uploadCourseHtml(courseId, file);
        }

        // 保存第一阶段任务状态为 已完成
        mqMessageService.completedStageOne(id);
    }

    // 保存课程索引信息到Elasticsearch
    private void saveCourseIndex(MqMessage mqMessage, long courseId) {
        log.debug("保存课程索引信息到Elasticsearch, 课程id:{}", courseId);

        // 消息id
        Long id = mqMessage.getId();
        // 消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        // 消息幂等性处理, 取出第二个阶段的状态
        int stageTwo = mqMessageService.getStageTwo(id);
        if (stageTwo > 0) {
            log.debug("课程索引信息已写入, 无需执行");
            return;
        }

        Boolean result = saveCourseIndex(courseId);
        if (result) {
            // 保存第二阶段任务状态为 已完成
            mqMessageService.completedStageTwo(id);
        }

    }

    private Boolean saveCourseIndex(long courseId) {
        // 取出课程发布信息
        CoursePublish coursePublish = coursePublishService.getById(courseId);
        // 拷贝至课程索引对象
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish, courseIndex);
        // 远程调用搜索服务api, 添加课程信息(文档)到索引
        Boolean add = searchServiceClient.add(courseIndex);
        if (!add) {
            XueChengPlusException.cast("添加文档到索引失败");
        }
        return true;
    }

    // 将课程信息缓存至redis
    private void saveCourseCache(MqMessage mqMessage, long courseId) {
        log.debug("将课程信息缓存至redis, 课程id:{}", courseId);

        // 消息id
        Long id = mqMessage.getId();
        // 消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        // 消息幂等性处理, 取出第三个阶段的状态
        int stageThree = mqMessageService.getStageThree(id);
        if (stageThree > 0) {
            log.debug("课程缓存数据已写入redis, 无需执行");
            return;
        }
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 保存第三阶段任务状态为 已完成
        mqMessageService.completedStageThree(id);
    }



}
