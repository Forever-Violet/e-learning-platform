package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;

import java.util.List;

/**
 * <p>
 * 课程分类 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-07-26
 */
public interface CourseCategoryService extends IService<CourseCategory> {

    /**
     * 课程分类树形结构查询
     * @param id
     * @return
     */
    public List<CourseCategoryTreeDto> queryTreeNodes(String id);

}
