package com.bit.scmu_taotao.mapper;

import com.bit.scmu_taotao.entity.UserGoodsBrowse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author 35314
 * @description 针对表【user_goods_browse(用户商品浏览记录表)】的数据库操作Mapper
 * @createDate 2026-04-02 16:56:14
 * @Entity com.bit.scmu_taotao.entity.UserGoodsBrowse
 */
public interface UserGoodsBrowseMapper extends BaseMapper<UserGoodsBrowse> {
    /*// 查询用户近30天浏览最多的Top2分类（实时计算偏好）
    @Select("SELECT g.category_id " +
            "FROM user_goods_browse b " +
            "LEFT JOIN t_goods g ON b.goods_id = g.goods_id " +
            "WHERE b.user_id = #{userId} " +
            "AND b.browse_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "AND g.is_delete = 0 " + // 补充：过滤已删除商品
            "GROUP BY g.category_id " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT 2")
    List<String> getUserTop2Categories(@Param("userId") String userId);*/
}




