package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachPlanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 课程计划 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {

    @Resource
    TeachplanMapper teachplanMapper;

    @Resource
    TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachPlanDto> findTeachplanTree(long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        // 课程计划id
        Long id = teachplanDto.getId();
        // 修改课程计划, 根据id的有无判断是修改还是新增
        if (id != null) { // 如果有即更新
            Teachplan teachplan = teachplanMapper.selectById(id);
            // 复制数据
            BeanUtils.copyProperties(teachplanDto, teachplan);
            // 更新
            teachplanMapper.updateById(teachplan);
        } else { //如果无即新增
            // 取出同父同级别的课程计划数量, 之后要计算当前新增的节的排序,
            // 这样是不合理的, 因为如果删除了非末节点, 那么下次新增的节点的排序就会等于末节点的排序, 造成排序混乱
            // int count = getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid());


            Teachplan teachplanNew = new Teachplan();
            // 设置排序号 orderby 最末的位置
            teachplanNew.setOrderby(getTeachplanLastOrder(teachplanDto.getCourseId(), teachplanDto.getParentid()));
            // 复制数据, 源->目标
            BeanUtils.copyProperties(teachplanDto, teachplanNew);
            teachplanMapper.insert(teachplanNew);
        }
    }

    private int getTeachplanLastOrder(long courseId, long parentId) {
/*        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(Teachplan::getCourseId, courseId)  // 课程相同
                .eq(Teachplan::getParentid, parentId); // 父亲相同
        Integer count = teachplanMapper.selectCount(queryWrapper); // 查询数量*/
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(Teachplan::getCourseId, courseId)  // 课程相同
                .eq(Teachplan::getParentid, parentId)  // 父亲相同
                .orderByDesc(Teachplan::getOrderby) // 根据排序字段 降序排序, ASC-升序, DESC-降序
                .last("limit 1");  //只获取第一个记录, 即排序最大的记录,  last()方法用于拼接SQL语句的最后部分。

        Teachplan teachplan = teachplanMapper.selectOne(queryWrapper);
//        Integer maxOrder = 0; //最大排序
/*        for (Teachplan teachplan : teachplans) { // 遍历同父同级的节点, 找到最大的排序
            if (teachplan.getOrderby() > maxOrder) {
                maxOrder = teachplan.getOrderby();
            }
        }*/

        return teachplan.getOrderby() + 1; //最大排序加1, 变成最末排序
    }

    @Transactional //事务
    @Override
    public void deleteTeachplan(Long teachplanId) {
        // 先获取章节
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);

        if (0 == teachplan.getParentid()) { //如果是大章节
            // 构造查询条件, 查询该大章节是否有节点
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            // 子节点的父ID等于 该大章节的ID
            queryWrapper.eq(Teachplan::getParentid, teachplanId);
            Integer count = teachplanMapper.selectCount(queryWrapper);
            if (count > 0) { //如果还存在子节点, 则抛异常
                throw new XueChengPlusException("120409", "课程计划信息还有子级信息，无法删除");
            } else {
                // 否则进行删除
                teachplanMapper.deleteById(teachplanId);
            }
        } else { //如果是小章节
            // 构造删除条件, teachplanId等于小章节的Id
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
            // 先删除小章节关联的媒体信息
            teachplanMediaMapper.delete(queryWrapper);
            // 然后删除小章节的信息
            teachplanMapper.deleteById(teachplanId);
        }
    }
}
