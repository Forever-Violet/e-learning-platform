package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 课程-教师关系表 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CourseTeacherServiceImpl extends ServiceImpl<CourseTeacherMapper, CourseTeacher> implements CourseTeacherService {

    @Resource
    CourseTeacherMapper courseTeacherMapper;

    @Transactional //事务
    @Override
    public List<CourseTeacher> getCourseTeacherList(Long courseId) {
        // 构造查询条件
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(CourseTeacher::getCourseId, courseId); //根据课程id
        return courseTeacherMapper.selectList(queryWrapper);
    }


    @Transactional //事务
    @Override
    public CourseTeacher addOrUpdateCourseTeacher(CourseTeacher courseTeacher) {

        if (courseTeacher.getId() == null) { //如果传来的参数教师id为空, 即为添加
            // 设置创建时间
            courseTeacher.setCreateDate(LocalDateTime.now());
            // 插入
            Integer i = courseTeacherMapper.insert(courseTeacher);
            if (i <= 0) {
                throw new XueChengPlusException("新增失败");
            }
        } else {
            // 更新
            courseTeacherMapper.updateById(courseTeacher);
        }
        return courseTeacher; // 这里可以直接返回, 因为在插入成功之后, 主键id会自动赋值给这个对象
    }

    @Transactional
    @Override
    public void deleteCourseTeacher(Long courseId, Long courseTeacherId) {
        int i = courseTeacherMapper.deleteById(courseTeacherId);
        if (i <= 0) {
            throw new XueChengPlusException("删除失败");
        }
    }
}
