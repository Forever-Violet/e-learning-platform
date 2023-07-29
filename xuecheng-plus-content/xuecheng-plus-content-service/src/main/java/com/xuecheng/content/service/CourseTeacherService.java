package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * <p>
 * 课程-教师关系表 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-07-26
 */
public interface CourseTeacherService extends IService<CourseTeacher> {

    /**
     * 根据课程id查询对应的课程教师列表
     * @param courseId 课程id
     * @return
     */
    List<CourseTeacher> getCourseTeacherList(Long courseId);


    /**
     * @description 添加或修改课程教师, 根据教师id的有无判断修改还是删除
     * @param courseTeacher 课程教师信息
     * @return
     */
    CourseTeacher addOrUpdateCourseTeacher(CourseTeacher courseTeacher);

    /**
     * @description 删除课程教师
     * @param courseId 课程id
     * @param courseTeacherId 课程教师id
     */
    void deleteCourseTeacher(Long courseId, Long courseTeacherId);

}
