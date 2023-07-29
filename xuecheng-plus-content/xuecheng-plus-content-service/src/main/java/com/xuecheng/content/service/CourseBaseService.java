package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * <p>
 * 课程基本信息管理业务接口
 * </p>
 *
 * @author itcast
 * @since 2023-07-26
 */
public interface CourseBaseService extends IService<CourseBase> {

    /**
     * 课程查询接口
     * @param pageParams 分页参数
     * @param queryCourseParamsDto 查询条件
     * @return PageResult<CourseBase>
     */
    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams,
                                               QueryCourseParamsDto queryCourseParamsDto);

    /**
     * @description 添加课程基本信息
     * @param companyId 教学机构Id
     * @param addCourseDto 课程基本信息
     * @return com.xuecheng.content.model.dto.CourseBaseInfoDto
     */
    CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);


    /**
     * @description 根据课程id查询课程信息(包括基本信息和营销信息)
     * @param courseId 课程id
     * @return com.xuecheng.content.model.dto
     */
    CourseBaseInfoDto getCourseBaseInfoById(long courseId);

    /**
     * @description 修改课程信息
     * @param companyId 机构id
     * @param dto 课程信息
     * @return
     */
    CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto);


    /**
     * @description 删除课程信息, 连带删除课程基本信息、课程营销信息、课程计划、课程计划关联信息、课程师资
     * @param courseId
     */
    void deleteCourseBase(Long courseId);

}
