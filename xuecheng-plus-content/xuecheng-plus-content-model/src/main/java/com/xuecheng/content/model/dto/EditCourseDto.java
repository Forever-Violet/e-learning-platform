package com.xuecheng.content.model.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * @description 修改课程dto, 继承了新增课程Dto, 多了课程id
 * @author 12508
 */
@Data
@ApiModel(value = "EditCourseDto", description = "修改课程信息")
public class EditCourseDto extends AddCourseDto{

    @ApiModelProperty(value = "课程id", required = true)
    private Long id;

}
