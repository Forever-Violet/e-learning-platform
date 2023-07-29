package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @description 课程教师编辑接口
 * @author itcast
 */
@Api(value = "课程教师编辑接口", tags = "课程教师编辑接口")
@RestController
public class CourseTeacherController {

    @Autowired
    private CourseTeacherService  courseTeacherService;


    @ApiOperation("查询课程教师列表")
    @GetMapping("/courseTeacher/list/{id}")
    @ApiImplicitParam(value = "courseId", name = "课程Id", required = true)
    public List<CourseTeacher> list(@PathVariable("id") Long courseId) {
        return courseTeacherService.getCourseTeacherList(courseId);
    }


    @ApiOperation("新增或修改课程的教师")
    @RequestMapping(value = "/courseTeacher", method = {RequestMethod.POST, RequestMethod.PUT})
    public CourseTeacher addOrUpdateCourseTeacher(@RequestBody @Validated CourseTeacher CourseTeacher) {

        return courseTeacherService.addOrUpdateCourseTeacher(CourseTeacher);
    }

    @ApiOperation("删除课程的教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{courseTeacherId}")
    public ResponseEntity<Void> deleteCourseTeacher(@PathVariable Long courseId,
                                                    @PathVariable Long courseTeacherId) {
        courseTeacherService.deleteCourseTeacher(courseId, courseTeacherId);
        return ResponseEntity.ok().build();
    }
}
