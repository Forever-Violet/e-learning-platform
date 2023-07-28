package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import com.xuecheng.content.service.CourseCategoryService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description 课程分类相关接口
 * @author 12508
 */
@Slf4j
@RestController
public class CourseCategoryController {

    @Resource
    CourseCategoryService categoryService;

    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeDto> queryTreeNodes() {
        return categoryService.queryTreeNodes("1");
    }

}
