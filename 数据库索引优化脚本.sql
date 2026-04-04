-- =============================================
-- SCMU淘淘推荐系统 - 数据库索引优化脚本
-- =============================================

-- 说明: 本脚本用于优化推荐系统的数据库查询性能
-- 执行前请备份数据库，生产环境请在低峰期执行

-- =============================================
-- 1. 浏览记录表索引（user_goods_browse）
-- =============================================

-- 用于查询用户近30天浏览的Top2分类
CREATE INDEX IF NOT EXISTS idx_browse_user_time
  ON user_goods_browse(user_id, browse_time);

-- 用于删除旧浏览记录和统计
CREATE INDEX IF NOT EXISTS idx_browse_time
  ON user_goods_browse(browse_time);

-- 用于关联商品信息时的查询
CREATE INDEX IF NOT EXISTS idx_browse_goods_id
  ON user_goods_browse(goods_id);

-- =============================================
-- 2. 商品表索引（t_goods）
-- =============================================

-- 推荐查询最常用的复合索引
-- 用于过滤有效商品 + 排序
CREATE INDEX IF NOT EXISTS idx_goods_composite
  ON t_goods(goods_status, is_delete, create_time DESC);

-- 分类查询索引
CREATE INDEX IF NOT EXISTS idx_goods_category
  ON t_goods(category_id);

-- 发布者查询索引
CREATE INDEX IF NOT EXISTS idx_goods_user_status
  ON t_goods(user_id, goods_status, is_delete);

-- 点击量排序查询索引
CREATE INDEX IF NOT EXISTS idx_goods_viewcount
  ON t_goods(view_count DESC, is_delete, goods_status);

-- =============================================
-- 3. 收藏表索引（t_favorite）
-- =============================================

-- 用于检查用户是否收藏商品
CREATE INDEX IF NOT EXISTS idx_favorite_user_goods
  ON t_favorite(user_id, goods_id, is_delete);

-- 用于统计商品被收藏数
CREATE INDEX IF NOT EXISTS idx_favorite_goods
  ON t_favorite(goods_id, is_delete);

-- =============================================
-- 4. 验证和查看索引
-- =============================================

-- 查看user_goods_browse表的所有索引
-- SHOW INDEX FROM user_goods_browse;

-- 查看t_goods表的所有索引
-- SHOW INDEX FROM t_goods;

-- 查看t_favorite表的所有索引
-- SHOW INDEX FROM t_favorite;

-- =============================================
-- 说明
-- =============================================

/*
索引优化说明:

1. user_goods_browse 表:
   - idx_browse_user_time: 用于快速查询用户的浏览历史（最常用）
   - idx_browse_time: 用于清理旧数据时的查询
   - idx_browse_goods_id: 用于JOIN商品表时的关联

2. t_goods 表:
   - idx_goods_composite: 最关键的复合索引，覆盖推荐查询中的三个条件:
     * 商品状态过滤 (goods_status = 0)
     * 逻辑删除过滤 (is_delete = 0)
     * 发布时间排序 (create_time DESC)
   - idx_goods_category: 用于分类浏览和偏好分类查询
   - idx_goods_user_status: 用于用户商品发布者信息查询
   - idx_goods_viewcount: 用于点击量排序查询

3. t_favorite 表:
   - idx_favorite_user_goods: 用于检查用户是否收藏特定商品（推荐查询常用）
   - idx_favorite_goods: 用于统计商品被收藏次数（冷启动推荐常用）

预期性能提升:
- 用户偏好分类查询: 从O(n) 提升到 O(log n)
- 个性化推荐查询: 避免全表扫描，性能提升 50-80%
- 冷启动推荐查询: 收藏数统计性能提升 30-50%
*/

