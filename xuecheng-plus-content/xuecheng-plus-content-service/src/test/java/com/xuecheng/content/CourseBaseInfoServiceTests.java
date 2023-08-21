package com.xuecheng.content;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class CourseBaseInfoServiceTests {

    @Resource
    CourseBaseService courseBaseService;

    @Test
    void testCourseBaseInfoService(){
        // 查询跳进
        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");
        queryCourseParamsDto.setAuditStatus("202004");
        queryCourseParamsDto.setPublishStatus("203001");

        // 分页参数
        PageParams pageParams = new PageParams();
        pageParams.setPageSize(3L);
        pageParams.setPageNo(1L);

        PageResult<CourseBase> courseBasePageResult =
                courseBaseService.queryCourseBaseList(companyId, pageParams, queryCourseParamsDto);
        System.out.println(courseBasePageResult);

    }

}
