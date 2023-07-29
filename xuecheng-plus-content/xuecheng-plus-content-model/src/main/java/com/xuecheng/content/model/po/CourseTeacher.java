package com.xuecheng.content.model.po;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 课程-教师关系表
 * </p>
 *
 * @author itcast
 */
@Data
@TableName("course_teacher")
public class CourseTeacher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO) // 插入后自动赋值
    @ApiModelProperty(value = "课程教师Id", required = true)
    private Long id;

    /**
     * 课程标识
     */
    @ApiModelProperty(value = "课程Id", required = true)
    private Long courseId;

    /**
     * 教师姓名
     */
    @NotBlank(message = "教师姓名不能为空")
    @ApiModelProperty(value = "教师姓名", required = true)
    private String teacherName;

    /**
     * 教师职位
     */
    @NotBlank(message = "教师职位不能为空")
    @ApiModelProperty(value = "教师职位", required = true)
    private String position;

    /**
     * 教师简介
     */
    @ApiModelProperty(value = "课程教师简介", required = true)
    private String introduction;

    /**
     * 照片
     */
    @ApiModelProperty(value = "课程教师照片", required = true)
    private String photograph;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty(value = "课程教师创建时间", required = true)
    private LocalDateTime createDate;


}
