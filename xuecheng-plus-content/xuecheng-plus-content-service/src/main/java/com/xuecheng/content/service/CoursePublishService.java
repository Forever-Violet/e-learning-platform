package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

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


}
