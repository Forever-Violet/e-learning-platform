package com.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @description 课程预览数据模型 dto
 * @author 12508
 */
@Data
@ToString
public class CoursePreviewDto {

    // 包括课程基本信息, 课程营销信息
    CourseBaseInfoDto courseBase;

    // 课程计划信息
    List<TeachPlanDto> teachplans;

    // 师资信息暂时不加..

}
