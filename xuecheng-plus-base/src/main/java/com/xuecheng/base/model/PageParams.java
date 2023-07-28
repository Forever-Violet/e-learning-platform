package com.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.builder.ToStringExclude;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PageParams {

    // 当前页码
    @ApiModelProperty("当前页码")
    private Long pageNo = 1L;

    // 每页记录数默认值
    @ApiModelProperty("每页记录数默认值")
    private Long pageSize = 10L;


}
