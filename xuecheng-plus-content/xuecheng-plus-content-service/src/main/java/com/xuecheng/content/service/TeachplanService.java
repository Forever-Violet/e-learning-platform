package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.Teachplan;

import java.util.List;

/**
 * @description 课程基本信息管理业务接口
 * @author itcast
 * @since 2023-07-26
 */
public interface TeachplanService extends IService<Teachplan> {

    /**
     * @description 查询课程计划树形结构
     * @param courseId 课程id
     * @return
     */
    public List<TeachPlanDto> findTeachplanTree(long courseId);

    /**
     * @description 保存课程计划
     * @param teachplanDto 课程计划信息
     */
    void saveTeachplan(SaveTeachplanDto teachplanDto);

    /**
     * @description 根据id删除课程计划, 大章节下有小章节时不允许删除。删除小章节，同时将关联的信息进行删除。
     * @param teachplanId 课程计划id
     */
    void deleteTeachplan(Long teachplanId);


}
