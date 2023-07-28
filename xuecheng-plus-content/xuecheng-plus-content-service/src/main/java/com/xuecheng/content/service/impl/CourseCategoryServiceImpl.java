package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 课程分类 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {

    @Resource
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        // 先获取该结点的所有子节点(这里包括该节点本身)
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        // 将list转map, 以备使用, 排除根节点
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos
                .stream()
                .filter(item -> !id.equals(item.getId()))
                .collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));

        // 最终返回的list
        List<CourseCategoryTreeDto> categoryTreeDtos = new ArrayList<>();

        // 下面要给根节点的所有子节点(以及子节点的子节点...)设置对应的孩子结点列表
        // 依次遍历每个元素, 排除根节点
        courseCategoryTreeDtos
                .stream()
                .filter(item -> !id.equals(item.getId())) // 先排除根节点
                .forEach(item -> {
                    if (item.getParentid().equals(id)) {
                        categoryTreeDtos.add(item); // 先加入结果集
                    }
                    // 找到当前结点的父节点
                    CourseCategoryTreeDto courseCategoryTreeDto = mapTemp.get(item.getParentid());
                    if (courseCategoryTreeDto != null) { //如果父节点不为空
                        // 如果父节点的孩子列表为空, 先给他new一个孩子节点列表
                        if (courseCategoryTreeDto.getChildrenTreeNodes() == null) {
                            courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                        }
                        // 下边开始往ChildrenTreeNodes属性中放子节点
                        courseCategoryTreeDto.getChildrenTreeNodes().add(item);
                    }
                });

        return categoryTreeDtos;
    }
}
