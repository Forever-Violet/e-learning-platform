package com.xuecheng.content.api;

import com.alibaba.fastjson.JSON;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * @description 课程预览, 发布
 * @author 12508
 */
@Controller
public class CoursePublishController {

    @Autowired
    CoursePublishService coursePublishService;


    @ApiOperation("查询课程发布信息")
    @ResponseBody
    @GetMapping("/r/coursepublish/{courseId}")
    public CoursePublish getCoursepublish(@PathVariable("courseId") Long courseId) {
        return coursePublishService.getCoursePublish(courseId);
    }

    @ApiOperation("获取课程发布信息")
    @ResponseBody
    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getCoursePublish(@PathVariable("courseId") Long courseId) {
/*        // 查询课程发布信息
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        if (coursePublish == null) {
            return new CoursePreviewDto();
        }
        // 课程基本信息
        CourseBaseInfoDto courseBaseDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(coursePublish, courseBaseDto);
        // 课程计划, json数据转为List<TeachPlanDto>
        List<TeachPlanDto> teachPlans = JSON.parseArray(coursePublish.getTeachplan(), TeachPlanDto.class);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseDto); //课程基本信息
        coursePreviewDto.setTeachplans(teachPlans); // 课程计划

        return coursePreviewDto;*/
        // 查询课程发布信息
        CoursePublish coursePublish = coursePublishService.getCoursePublishFirstInCache(courseId);
        if (coursePublish == null) {
            return new CoursePreviewDto();
        }

        // 课程基本信息, 营销信息
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(coursePublish, coursePublish);
        // 课程计划
        List<TeachPlanDto> teachPlans = JSON.parseArray(coursePublish.getTeachplan(), TeachPlanDto.class);

        // 填充
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachplans(teachPlans);
        return coursePreviewDto;
    }

    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId) {
        ModelAndView modelAndView = new ModelAndView();
        // 课程预览信息共享到请求域
        modelAndView.addObject("model", coursePublishService.getCoursePreviewInfo(courseId));
        modelAndView.setViewName("course_template"); //逻辑视图, 后面会加上.ftl去找
        return modelAndView;

    }


    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId, courseId);
    }


    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping("/coursepublish/{courseId}")
    public void coursePublish(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.publish(companyId, courseId);

    }


}
