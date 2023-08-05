package com.xuecheng.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {

    /**
     * @description 根据分片参数获取待处理任务
     * @param shardTotal 分片总数
     * @param shardIndex 分片序号
     * @param count 任务数
     * @return List<MediaProcess>
     */
    // 通过将id对分片总数取模得到对应的分片序号, 将查询出的任务给分片序号对应的执行器处理
    @Select("select * from media_process t where t.id % #{shardTotal} = #{shardIndex}" +
            " and (t.status = '1' or t.status = '3')" + //状态为未处理或失败的任务
            " and t.fail_count < 3 limit #{count}") // 任务失败次数小于3, 并将查询出来的记录数限制为count个
    List<MediaProcess> selectListByShardIndex(@Param("shardTotal") int shardTotal,
                                              @Param("shardIndex") int shardIndex,
                                              @Param("count") int count);


    /**
     * @description 开启一个任务
     * @param id 任务的id
     * @return 更新记录数
     */
    @Update("update media_process m set m.status = '4' where (m.status = '1' or m.status = '3')" +
            "and m.fail_count < 3 and m.id = #{id}")
    int startTask(@Param("id") long id);

}
