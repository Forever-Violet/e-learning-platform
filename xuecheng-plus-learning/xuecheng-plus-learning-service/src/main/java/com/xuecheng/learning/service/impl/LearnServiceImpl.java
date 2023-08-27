package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearnService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class LearnServiceImpl implements LearnService {

    @Resource
    ContentServiceClient contentServiceClient;

    @Resource
    MediaServiceClient mediaServiceClient;

    @Resource
    MyCourseTablesService myCourseTablesService;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {

        // 查询课程信息
        CoursePublish coursePublish = contentServiceClient.getCoursepublish(courseId);
        if (coursePublish == null) {
            XueChengPlusException.cast("课程信息不存在");
        }

        // 校验学习资格, 远程调用内容管理服务, 根据课程计划id(teachplanId)去查询课程计划信息, 如果is_preview的值为1表示支持试学
        // 也可以从coursePublish对象中解析出teachplan去判断是否支持试学
        // 如果支持试学, 直接调用媒资服务查询视频的方法返回url
        List<TeachPlanDto> teachPlanTree = JSON.parseArray(coursePublish.getTeachplan(), TeachPlanDto.class);

        for (TeachPlanDto teachPlan : teachPlanTree) { //一级节点 章节
            for (TeachPlanDto teachPlan_two : teachPlan.getTeachPlanTreeNodes()) { //二级节点 小章节

                // 遍历小章节, 如果支持试学, 直接返回url
                if (teachPlan_two.getId() == teachplanId && teachPlan_two.getIsPreview().equals("1")) {
                    return mediaServiceClient.getPlayUrlByMediaId(mediaId);
                }
            }
        }

        // 未登录或未选课 判断是否收费
        String charge = coursePublish.getCharge();
        if (charge.equals("201000")) { //可以免费学习
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }

        // 如果登录
        if (StringUtils.isNotEmpty(userId)) {

            // 判断是否选课, 根据选课情况判断学习资格
            XcCourseTablesDto xcCourseTablesDto = myCourseTablesService.getLearningStatus(userId, courseId);
            if (xcCourseTablesDto == null) {
                XueChengPlusException.cast("您没有选该门课程");
            }
            // 学习资格状态  [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
            String learnStatus = xcCourseTablesDto.getLearnStatus();
            if (learnStatus.equals("702001")) {
                // 返回url   RestResponse<String> : 状态码+url
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            } else if (learnStatus.equals("702003")) {
                return RestResponse.validfail("您的选课已过期，需要申请续期或重新支付");
            } else {
                return RestResponse.validfail("无法学习，您没有选课或选课后没有支付");
            }
        }


        return RestResponse.validfail("请购买课程后继续学习");
    }
}
