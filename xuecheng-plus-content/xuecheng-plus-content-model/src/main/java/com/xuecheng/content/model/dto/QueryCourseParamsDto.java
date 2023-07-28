package com.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * DTO (Data Transfer Object) 数据传输对象
 * 这个传输通常指的前后端之间的传输
 * DTO是一个比较特殊的对象，他有两种存在形式：
 * 在后端，他的存在形式是java对象，也就是在controller里面定义的那个东东，通常在后端不需要关心怎么从json转成java对象的，这个都是由一些成熟的框架帮你完成啦，比如spring框架
 * @author 12508
 * DTO数据传输对象、PO持久化对象，DTO用于接口层向业务层之间传输数据，
 * PO(Persistent Object)用于业务层与持久层之间传输数据，有些项目还会设置VO(View Object)对象，
 * VO对象用在前端与接口层之间传输数据
 */

@Data
@ToString
public class QueryCourseParamsDto {

    // 审核状态
    private String auditStatus;

    // 课程名称
    private String courseName;

    // 发布状态
    private String publishStatus;


}
