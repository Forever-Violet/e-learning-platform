package com.xuecheng.content.api;


import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(@RequestBody AddCourseDto addCourseDto) {
        // 机构id, 由于认证系统没有上线, 暂时硬编码
        Long companyId = 1234125125L;
        return courseBaseService.createCourseBase(companyId, addCourseDto);
    }


}
