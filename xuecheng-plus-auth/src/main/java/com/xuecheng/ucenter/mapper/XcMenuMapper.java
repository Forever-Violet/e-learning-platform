package com.xuecheng.ucenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.ucenter.model.po.XcMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface XcMenuMapper extends BaseMapper<XcMenu> {
    @Select("SELECT	* FROM xc_menu WHERE id IN (" + // 根据(多个)menu_id找到在菜单表对应的菜单(权限)记录
            "SELECT menu_id FROM xc_permission WHERE role_id IN ( " + // 根据角色id在权限表(角色与菜单的对应关系表)中与查出角色id相匹配的记录的(多个)menu_id
            "SELECT role_id FROM xc_user_role WHERE user_id = #{userId} ))" // 根据用户id查出用户角色 id
            )
    List<XcMenu> selectPermissionByUserId(@Param("userId") String userId);
}
