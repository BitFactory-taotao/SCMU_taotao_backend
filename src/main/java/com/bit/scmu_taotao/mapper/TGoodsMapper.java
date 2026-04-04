package com.bit.scmu_taotao.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bit.scmu_taotao.entity.TGoods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * @author 35314
 * @description 针对表【t_goods(商品信息表)】的数据库操作Mapper
 * @createDate 2026-03-14 18:49:38
 * @Entity com.bit.scmu_taotao.entity.TGoods
 */
public interface TGoodsMapper extends BaseMapper<TGoods> {
    /**
     * 获取个性化推荐商品（基于用户偏好分类）
     * 公式：偏好分类(30) + 收藏(20*是否) + 点击量(25) + 发布时效(25)
     */
    @Select("<script>" +
            "SELECT " +
            "  g.goods_id, g.goods_name, g.price, g.category_id, gc.category_name, " +
            "  g.view_count, (f.favorite_id IS NOT NULL) AS is_favorited, " +
            "  g.user_id, u.user_name, u.credit_star, u.credit_score, " +
            "  gi.image_url, DATE_FORMAT(g.create_time, '%Y-%m-%d %H:%i:%S') AS create_time, " +
            "  (CASE WHEN g.category_id IN " +
            "    <foreach collection='topCategories' item='catId' open='(' close=')' separator=','>" +
            "      #{catId} " +
            "    </foreach>" +
            "    THEN #{categoryWeight} ELSE 5 END " +
            "   + (CASE WHEN f.favorite_id IS NOT NULL THEN #{favoriteWeight} ELSE 0 END) " +
            "   + (g.view_count * #{viewCountWeight} / 100) " +
            "   + (CASE WHEN DATEDIFF(NOW(), g.create_time) &lt;= 30 THEN (30 - DATEDIFF(NOW(), g.create_time)) * #{recentPublishBonus} / 100 ELSE 0 END) " +
            "  ) AS recommend_score " +
            "FROM t_goods g " +
            "LEFT JOIN t_goods_category gc ON g.category_id = gc.category_id " +
            "LEFT JOIN t_favorite f ON g.goods_id = f.goods_id AND f.user_id = #{userId} AND f.is_delete = 0 " +
            "LEFT JOIN t_user u ON g.user_id = u.user_id " +
            "LEFT JOIN (SELECT goods_id, image_url FROM t_goods_image WHERE sort = 1) gi ON g.goods_id = gi.goods_id " +
            "WHERE g.goods_status = 0 AND g.is_delete = 0 " +
            "ORDER BY recommend_score DESC " +
            "LIMIT #{pageSize} OFFSET #{offset} " +
            "</script>")
    List<Map<String, Object>> getPersonalizedRecommendations(
            @Param("userId") String userId,
            @Param("topCategories") List<Integer> topCategories,
            @Param("categoryWeight") Double categoryWeight,
            @Param("favoriteWeight") Double favoriteWeight,
            @Param("viewCountWeight") Double viewCountWeight,
            @Param("recentPublishBonus") Double recentPublishBonus,
            @Param("pageSize") Integer pageSize,
            @Param("offset") Long offset
    );

    /**
     * 获取冷启动推荐商品（全校热门Top50）
     * 公式：点击量(15%) + 收藏数(20*数量) + 发布时效
     */
    @Select("SELECT " +
            "  g.goods_id, g.goods_name, g.price, g.category_id, gc.category_name, " +
            "  g.view_count, 0 AS is_favorited, " +
            "  g.user_id, u.user_name, u.credit_star, u.credit_score, " +
            "  gi.image_url, DATE_FORMAT(g.create_time, '%Y-%m-%d %H:%i:%S') AS create_time, " +
            "  (g.view_count * 0.15 " +
            "   + COALESCE(favorite_count.cnt, 0) * 20 " +
            "   + (CASE WHEN DATEDIFF(NOW(), g.create_time) <= 30 THEN (30 - DATEDIFF(NOW(), g.create_time)) * 0.8 ELSE 0 END) " +
            "  ) AS hot_score " +
            "FROM t_goods g " +
            "LEFT JOIN t_goods_category gc ON g.category_id = gc.category_id " +
            "LEFT JOIN t_user u ON g.user_id = u.user_id " +
            "LEFT JOIN (SELECT goods_id, image_url FROM t_goods_image WHERE sort = 1) gi ON g.goods_id = gi.goods_id " +
            "LEFT JOIN (SELECT goods_id, COUNT(*) as cnt FROM t_favorite WHERE is_delete = 0 GROUP BY goods_id) favorite_count " +
            "  ON g.goods_id = favorite_count.goods_id " +
            "WHERE g.goods_status = 0 AND g.is_delete = 0 " +
            "ORDER BY hot_score DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getColdStartRecommendations(@Param("limit") Integer limit);

    /**
     * 增加商品点击量
     */
    @Update("UPDATE t_goods SET view_count = view_count + 1 WHERE goods_id = #{goodsId}")
    int incrementViewCount(@Param("goodsId") Long goodsId);

    /**
     * 查询商品当前点击量
     */
    @Select("SELECT view_count FROM t_goods WHERE goods_id = #{goodsId}")
    Integer getViewCount(@Param("goodsId") Long goodsId);
}




