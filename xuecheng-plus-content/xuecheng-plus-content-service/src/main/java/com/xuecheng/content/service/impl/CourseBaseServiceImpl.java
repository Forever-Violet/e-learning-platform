package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 课程基本信息 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CourseBaseServiceImpl extends ServiceImpl<CourseBaseMapper, CourseBase> implements CourseBaseService {

    @Resource
    CourseBaseMapper courseBaseMapper;

    @Resource
    CourseMarketMapper courseMarketMapper;

    @Resource
    CourseCategoryMapper courseCategoryMapper;


    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {

        // 构建查询条件对象
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        // 构建查询条件, 根据课程名称查询
        queryWrapper
                .like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName,
                        queryCourseParamsDto.getCourseName())
                .eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus,
                        queryCourseParamsDto.getAuditStatus())  // 审核状态
                .eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus,
                        queryCourseParamsDto.getPublishStatus()); // 发布状态

        // 分页对象
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        // 查询数据内容获得结果
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        // 获取数据列表
        List<CourseBase> list = pageResult.getRecords();

        // 获取数据总数
        long total = pageResult.getTotal();

        // 构建结果集
        PageResult<CourseBase> courseBasePageResult = new
                PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());

        // 返回结果集
        return courseBasePageResult;
    }

    @Transactional //事务
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {

        // 合法性校验
        if (StringUtils.isBlank(addCourseDto.getName())) {
            throw new XueChengPlusException("课程名称为空");
        }
        if (StringUtils.isBlank(addCourseDto.getMt())) {
            throw new XueChengPlusException("课程分类为空");
        }
        if (StringUtils.isBlank(addCourseDto.getSt())) {
            throw new XueChengPlusException("课程分类为空");
        }
        if (StringUtils.isBlank(addCourseDto.getGrade())) {
            throw new XueChengPlusException("课程等级为空");
        }
        if (StringUtils.isBlank(addCourseDto.getTeachmode())) {
            throw new XueChengPlusException("教育模式为空");
        }
        if (StringUtils.isBlank(addCourseDto.getUsers())) {
            throw new XueChengPlusException("适用人群为空");
        }
        if (StringUtils.isBlank(addCourseDto.getCharge())) {
            throw new XueChengPlusException("收费规则为空");
        }

        // 新增对象
        CourseBase courseBaseNew = new CourseBase();
        // 将填写的课程信息赋值给新增的对象
        BeanUtils.copyProperties(addCourseDto, courseBaseNew);
        // 设置审核状态
        courseBaseNew.setAuditStatus("202002");
        // 设置发布状态
        courseBaseNew.setStatus("203001");
        // 机构id
        courseBaseNew.setCompanyId(companyId);
        // 设置添加时间
        courseBaseNew.setCreateDate(LocalDateTime.now());
        // 插入课程基本信息表
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new XueChengPlusException("新增课程基本信息失败");
        }

        // 向课程营销信息表保存课程营销信息
        // 课程营销信息
        CourseMarket courseMarketNew = new CourseMarket();
        Long courseId = courseBaseNew.getId(); // 两张表信息主键相同
        BeanUtils.copyProperties(addCourseDto, courseMarketNew);
        courseMarketNew.setId(courseId);
        int i = saveCourseMarket(courseMarketNew);
        if (i <= 0) {
            throw new XueChengPlusException("保存课程营销信息失败");
        }

        // 查询课程基本信息及营销信息并返回
        return getCourseBaseInfo(courseId);

    }

    // 保存课程营销信息
    private int saveCourseMarket(CourseMarket courseMarketNew) {
        // 收费规则
        String charge = courseMarketNew.getCharge();
        if (StringUtils.isBlank(charge)) {
            throw new XueChengPlusException("收费规则没有选择");
        }
        // 收费规则为收费
        if ("201001".equals(charge)) {
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice() <= 0) {
                throw new XueChengPlusException("课程收费价格不能为空且必须大于0");
            }
        }
        // 根据id从课程营销表查询
        CourseMarket courseMarketObj = courseMarketMapper.selectById(courseMarketNew.getId());
        if (courseMarketObj == null) {
            return courseMarketMapper.insert(courseMarketNew);
        } else {
            BeanUtils.copyProperties(courseMarketNew, courseMarketObj);
            courseMarketObj.setId(courseMarketNew.getId());
            return courseMarketMapper.updateById(courseMarketObj);
        }

    }

    // 根据课程id查询课程基本信息, 包括基本信息和营销信息
    public CourseBaseInfoDto getCourseBaseInfo(long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }

        // 查询分类名称
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt()); // 小分类
        courseBaseInfoDto.setStName(courseCategoryBySt.getName()); // 设置小分类名称
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt()); // 大分类
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName()); // 设置大分类名称

        return courseBaseInfoDto;
    }


}
