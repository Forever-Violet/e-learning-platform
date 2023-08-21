package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

/**
 * <p>
 * 课程发布 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-07-26
 */
public interface CoursePublishService extends IService<CoursePublish> {

    /**
     * @description 获取课程预览信息
     * @param courseId 练车od
     * @return CoursePreviewDto
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);


    /**
     * @description 提交审核
     * @param companyId 机构id
     * @param courseId 课程id
     */
    void commitAudit(Long companyId, Long courseId);


    /**
     * @description 课程发布接口
     * @param companyId 机构id
     * @param courseId 课程id
     */
    void publish(Long companyId, Long courseId);


    /**
     * @description 课程页面静态化
     * @param courseId 课程id
     * @return File 静态化页面文件
     */
    File generateCourseHtml(Long courseId);

    /**
     * @description 调用媒资管理服务的远程接口(通过FeignClient)来上传静态页面到minIO
     * @param courseId 课程id
     * @param file 静态化页面
     */
    void uploadCourseHtml(Long courseId, File file);


    /**
     * @description 根据id从课程发布表中查询课程信息
     * @param courseId 课程id
     * @return CoursePublish
     */
    CoursePublish getCoursePublish(Long courseId);
}
