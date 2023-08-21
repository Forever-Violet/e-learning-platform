package com.xuecheng.content.api;


import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


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
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')") //@PreAuthorize表示执行此方法需要授权, 如果当前用户请求没有权限则抛出异常
    public PageResult<CourseBase> list(PageParams pageParams
            , @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) { // required = false非必需
        // 取出用户身份
        SecurityUtil.XcUser xcUser = SecurityUtil.getUser();
        // 机构id
        Long companyId = Long.parseLong(xcUser.getCompanyId());
        return courseBaseService.queryCourseBaseList(companyId, pageParams, queryCourseParamsDto);
    }

    @ApiOperation("新增课程")
    @PostMapping("/course")                             //开启校验
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated AddCourseDto addCourseDto) {
        // 取出用户身份
        SecurityUtil.XcUser xcUser = SecurityUtil.getUser();
        // 机构id
        Long companyId = Long.parseLong(xcUser.getCompanyId());
        return courseBaseService.createCourseBase(companyId, addCourseDto);
    }

    @ApiOperation("根据课程id查询课程信息")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId) {
        // 取出当前用户身份
        // Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        System.out.println(user);
        return courseBaseService.getCourseBaseInfoById(courseId);
    }


    @ApiOperation("修改课程信息")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated EditCourseDto editCourseDto) {
        // 取出用户身份
        SecurityUtil.XcUser xcUser = SecurityUtil.getUser();
        // 机构id
        Long companyId = Long.parseLong(xcUser.getCompanyId());
        return courseBaseService.updateCourseBase(companyId, editCourseDto);
    }

    @ApiOperation("删除课程信息")
    @DeleteMapping("/course/{id}")
    public void deleteCourseBase(@PathVariable("id") Long courseId) {
        courseBaseService.deleteCourseBase(courseId);
    }

}
