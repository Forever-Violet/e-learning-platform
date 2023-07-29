package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author 12508
 * @description 课程计划(大纲)树形结构 dto  , 多了课程计划关联的媒资信息和子节点列表
 */
@Data
@ToString
public class TeachPlanDto extends Teachplan {

    //课程计划关联的媒资信息
    TeachplanMedia teachplanMedia;

    //子节点列表
    List<TeachPlanDto> teachPlanTreeNodes;

}
