package com.xuecheng.learning.service.impl;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Resource
    XcChooseCourseMapper xcChooseCourseMapper;

    @Resource
    XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Autowired
    MyCourseTablesService myCourseTablesService;

    @Autowired
    MyCourseTablesServiceImpl currentProxy; //代理对象

    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        // 查询课程信息
        CoursePublish coursePublish = contentServiceClient.getCoursepublish(courseId);
        // 课程收费标准
        String charge = coursePublish.getCharge();
        // 选课记录
        XcChooseCourse chooseCourse = null;
        if ("201000".equals(charge)){ //如果课程免费
            // 添加免费课程到选课记录表
            chooseCourse = addFreeCourse(userId, coursePublish);
            // 添加到我的课程表
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);

        } else { //如果是收费课程
            // 添加收费课程到选课记录表
            chooseCourse = addChargeCourse(userId, coursePublish);
        }
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse, xcChooseCourseDto);
        // 获取学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());

        return xcChooseCourseDto;
    }

    /**
     * @description 添加到我的课程表, 我的课程表的记录来源于选课记录，选课记录成功将课程信息添加到我的课程表,
     * 如果我的课程表已存在课程可能已经过期，如果有新的选课记录则需要更新我的课程表中的现有信息。
     * @param xcChooseCourse 选课记录
     * @return XcCourseTables
     */
    private XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse) {
        // 选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)) {
            XueChengPlusException.cast("选课未成功，无法添加到课程表");
        }
        // 查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) { // 如果我的课程表中已存在该课程
            return xcCourseTables;
        }
        // 如果不存在
        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId()); //选课记录id
        xcCourseTablesNew.setUserId(xcChooseCourse.getUserId());
        xcCourseTablesNew.setCourseId(xcChooseCourse.getCourseId());
        xcCourseTablesNew.setCompanyId(xcChooseCourse.getCompanyId());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setCourseName(xcChooseCourse.getCourseName());
        xcCourseTablesNew.setValidtimeStart(xcChooseCourse.getValidtimeStart());
        xcCourseTablesNew.setValidtimeEnd(xcChooseCourse.getValidtimeEnd());
        xcCourseTablesNew.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTablesMapper.insert(xcCourseTablesNew);

        return xcCourseTablesNew;
    }

    /**
     * @description 根据课程id和用户id查询我的课程表中的某一门课程
     * @param userId 用户id
     * @param courseId 课程id
     * @return XcCourseTables
     */
    private XcCourseTables getXcCourseTables(String userId, Long courseId) {
        return xcCourseTablesMapper.selectOne(
                new LambdaQueryWrapper<XcCourseTables>()
                        .eq(XcCourseTables::getUserId, userId)
                        .eq(XcCourseTables::getCourseId, courseId));

    }

    // 添加免费课程, 免费课程加入选课记录表
    private XcChooseCourse addFreeCourse(String userId, CoursePublish coursePublish) {
        // 查询选课记录表是否存在免费的且选课成功的订单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursePublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001") //免费课程
                .eq(XcChooseCourse::getStatus, "701001"); //选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0); //返回选课记录
        }
        //如果没有选成功过该免费课程 添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursePublish.getId());
        xcChooseCourse.setCourseName(coursePublish.getName());
        xcChooseCourse.setCoursePrice(0f); //免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursePublish.getCompanyId());
        xcChooseCourse.setOrderType("700001"); //免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now()); //创建时间
        xcChooseCourse.setStatus("701001"); //选课成功
        xcChooseCourse.setValidDays(365); //免费课程默认有效期365天
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));

        xcChooseCourseMapper.insert(xcChooseCourse);

        return xcChooseCourse;
    }

    // 添加收费课程
    private XcChooseCourse addChargeCourse(String userId, CoursePublish coursePublish) {
        // 如果存在待支付交易记录, 直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursePublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002") //收费订单
                .eq(XcChooseCourse::getStatus, "701002"); //待支付

        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0); //如果存在待支付交易记录, 直接返回
        }
        // 如果不存在, 添加待支付记录
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursePublish.getId());
        xcChooseCourse.setCourseName(coursePublish.getName());
        xcChooseCourse.setCoursePrice(coursePublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursePublish.getCompanyId());
        xcChooseCourse.setOrderType("700002"); //收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002"); //待支付

        xcChooseCourse.setValidDays(coursePublish.getValidDays()); //有效期
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursePublish.getValidDays()));

        xcChooseCourseMapper.insert(xcChooseCourse);
        return xcChooseCourse;

    }


    /**
     * @description 根据用户id和课程id获取学习资格
     * @param userId 用户id
     * @param courseId 课程id
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"}
     * ,{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        // 查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null) { //如果没有在我的课程表中查到记录
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            // 没有选课或选课后没有支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables, xcCourseTablesDto);
        // 是否过期, true过期, false未过期               //即ValidtimeEnd是否在当前时间之前, 是即过期, 否则反
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isExpires) {
            // 未过期 正常学习
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        } else {
            // 已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }
    }


    // 更新选课记录的状态为成功, 向我的课程表中插入选课成功的课程
    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        // 根据选课id查询选课记录
        XcChooseCourse chooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if (chooseCourse == null) {
            log.debug("接收购买课程的消息，根据选课id从数据库找不到选课记录，选课id:{}", chooseCourseId);
            return false;
        }
        // 选课状态
        String status = chooseCourse.getStatus();
        // 只有当未支付时才更新为已支付
        if ("701002".equals(status)) {
            // 更新选课记录的状态为支付成功
            chooseCourse.setStatus("701001");
            int update = xcChooseCourseMapper.updateById(chooseCourse);
            if (update <= 0) {
                log.debug("添加选课记录失败:{}", chooseCourseId);
                XueChengPlusException.cast("添加选课记录失败");
            }

            // 向我的课程表插入记录
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);
            return true;
        }

        return false;
    }

    // 我的课程表
    @Override
    public PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params) {
        // 页码
        long pageNo = params.getPage();
        // 每页记录数, 固定为4
        long pageSize = params.getSize();
        // 构造分页条件
        Page<XcCourseTables> page = new Page<>(pageNo, pageSize);
        // 根据用户id查询  (查询条件)
        LambdaQueryWrapper<XcCourseTables> queryWrapper = new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, params.getUserId())
                .orderByDesc(StringUtils.isNotEmpty(params.getCourseType()) && params.getCourseType().equals("1")
                        , XcCourseTables::getUpdateDate) //更新时间
                .orderByDesc(StringUtils.isNotEmpty(params.getCourseType()) && params.getCourseType().equals("2")
                        , XcCourseTables::getCreateDate); //加入课程时间

        // 分页查询
        Page<XcCourseTables> pageResult = xcCourseTablesMapper.selectPage(page, queryWrapper);
        // 记录存放到list集合
        List<XcCourseTables> records = pageResult.getRecords();
        // 记录总数
        long total = pageResult.getTotal();
        // 填充数据
        PageResult<XcCourseTables> courseTablesPageResult = new PageResult<>(records, total, pageNo, pageSize);

        return courseTablesPageResult;
    }
}
