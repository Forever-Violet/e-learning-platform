package com.xuecheng.content.api;


import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * @description 课程信息编辑接口
 * @author 12508
 */

@Api(value = "课程信息编辑接口", tags = "课程信息编辑接口")
@RestController // @Controller和@ResponseBody的整合
public class CourseBaseInfoController {

    @Autowired
    CourseBaseService courseBaseService;

    /**
     * pageParams分页参数通过url的key/value传入，queryCourseParams通过json数据传入，
     * 使用@RequestBody注解将json转成QueryCourseParamsDto对象。
     * @param pageParams
     * @param queryCourseParamsDto
     * @return
     */
    @ApiOperation("课程查询接口")
    @PostMapping ("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams
            , @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) { // required = false非必需

        return courseBaseService.queryCourseBaseList(pageParams, queryCourseParamsDto);
    }

    @ApiOperation("新增课程")
    @PostMapping("/course")                             //开启校验
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated AddCourseDto addCourseDto) {
        // 机构id, 由于认证系统没有上线, 暂时硬编码
        Long companyId = 1234125125L;
        return courseBaseService.createCourseBase(companyId, addCourseDto);
    }

    @ApiOperation("根据课程id查询课程信息")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId) {
        return courseBaseService.getCourseBaseInfoById(courseId);
    }


    @ApiOperation("修改课程信息")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated EditCourseDto editCourseDto) {
        // 机构id, 还没完善认证系统, 先硬编码
        Long companyId = 124124124L;
        return courseBaseService.updateCourseBase(companyId, editCourseDto);
    }

}
