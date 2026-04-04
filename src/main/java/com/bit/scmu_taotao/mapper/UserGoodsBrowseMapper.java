package com.bit.scmu_taotao.mapper;

import com.bit.scmu_taotao.entity.UserGoodsBrowse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @author 35314
 * @description 针对表【user_goods_browse(用户商品浏览记录表)】的数据库操作Mapper
 * @createDate 2026-04-02 16:56:14
 * @Entity com.bit.scmu_taotao.entity.UserGoodsBrowse
 */
public interface UserGoodsBrowseMapper extends BaseMapper<UserGoodsBrowse> {
    // 查询用户近30天浏览最多的Top2分类（实时计算偏好）
    @Select("SELECT g.category_id " +
            "FROM user_goods_browse b " +
            "LEFT JOIN t_goods g ON b.goods_id = g.goods_id " +
            "WHERE b.user_id = #{userId} " +
            "AND b.browse_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "AND g.is_delete = 0 " + // 过滤已删除商品
            "AND g.goods_status = 0 " +
            "GROUP BY g.category_id " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT 2")
    List<String> getUserTop2Categories(@Param("userId") String userId);

    /**
     * 查询用户浏览统计（分类维度）
     */
    @Select("SELECT g.category_id, gc.category_name, COUNT(*) as browse_count " +
            "FROM user_goods_browse b " +
            "LEFT JOIN t_goods g ON b.goods_id = g.goods_id " +
            "LEFT JOIN t_goods_category gc ON g.category_id = gc.category_id " +
            "WHERE b.user_id = #{userId} " +
            "  AND b.browse_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "  AND g.is_delete = 0 " +
            "GROUP BY g.category_id, gc.category_name " +
            "ORDER BY browse_count DESC")
    List<Map<String, Object>> getUserBrowseStatistics(@Param("userId") String userId);

    /**
     * 清理指定天数前的浏览记录
     */
    @Delete("DELETE FROM user_goods_browse " +
            "WHERE browse_time < DATE_SUB(DATE(NOW()), INTERVAL #{days} DAY)")
    int cleanupOldRecords(@Param("days") Integer days);
}




