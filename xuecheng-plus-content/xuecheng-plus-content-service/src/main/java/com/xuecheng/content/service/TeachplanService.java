package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;

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


    /**
     * @description 根据id下移课程计划的位置, 即将该课程计划的位置与其同级且下位的课程计划对调
     * @param teachplanId 课程计划id
     */
    void moveDownTeachplan(Long teachplanId);

    /**
     * @description 根据id上移课程计划的位置, 即将该课程计划的位置与其同级且上位的课程计划对调
     * @param teachplanId 课程计划id
     */
    void moveUpTeachplan(Long teachplanId);

    /**
     * @description 教学计划绑定媒资文件
     * @param bindTeachplanMediaDto dto
     * @return TeachplanMedia
     */
    TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);


    /**
     * @description 解除教学计划与媒资的绑定
     * @param teachPlanId 教学计划id
     * @param mediaId 媒资id
     */
    void unBindAssociationMedia(Long teachPlanId, String mediaId);


}
