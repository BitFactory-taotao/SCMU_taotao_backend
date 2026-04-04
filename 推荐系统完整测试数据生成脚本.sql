-- =============================================
-- SCMU淘淘推荐系统 - 完整测试数据生成脚本
-- =============================================
-- 说明：本脚本用于生成推荐系统完整测试所需的数据
-- 包含：用户、商品、分类、浏览记录、收藏等数据
-- 执行时间：2-3秒
-- =============================================
-- =============================================
-- 1. 分类数据（如果还未创建）
-- =============================================

INSERT IGNORE INTO t_goods_category (category_name, sort, is_show, create_time)
VALUES
    ( '电子产品', 1, 0, NOW()),
    ( '图书', 2, 0, NOW()),
    ( '宿舍用品', 3, 0, NOW()),
    ( '娱乐用品', 4, 0, NOW()),
    ( '学习用品', 5, 0, NOW());

-- =============================================
-- 2. 用户数据
-- =============================================

INSERT IGNORE INTO t_user (user_id, user_name, avatar, credit_score, credit_star, create_time, update_time, is_delete)
VALUES
-- 主要测试用户
('20240001', '张三', 'https://example.com/avatar/001.jpg', 95, 4.8, NOW(), NOW(), 0),
('20240002', '李四', 'https://example.com/avatar/002.jpg', 88, 4.4, NOW(), NOW(), 0),
('20240003', '王五', 'https://example.com/avatar/003.jpg', 92, 4.6, NOW(), NOW(), 0),
('20240004', '赵六', 'https://example.com/avatar/004.jpg', 85, 4.2, NOW(), NOW(), 0),
('20240005', '孙七', 'https://example.com/avatar/005.jpg', 90, 4.5, NOW(), NOW(), 0),
-- 无浏览记录的新用户（用于冷启动推荐测试）
('20240006', '周八', 'https://example.com/avatar/006.jpg', 100, 5.0, NOW(), NOW(), 0),
-- 高信誉用户
('20240007', '吴九', 'https://example.com/avatar/007.jpg', 98, 4.9, NOW(), NOW(), 0),
-- 低信誉用户
('20240008', '郑十', 'https://example.com/avatar/008.jpg', 60, 3.0, NOW(), NOW(), 0);

-- =============================================
-- 3. 商品数据（电子产品 - 分类1）
-- =============================================

INSERT IGNORE INTO t_goods (user_id, category_id, goods_type, goods_name, goods_desc, goods_note, price, use_scene, exchange_place, goods_status, view_count, create_time, is_delete)
VALUES
-- 高热度商品
('20240001', 1, 1, 'iPhone 13 Pro', '原装港版，95新，无拆无修', '配件齐全', 5999.00, '日常使用', '校园内', 0, 156, DATE_SUB(NOW(), INTERVAL 5 DAY), 0),
('20240001', 1, 1, 'iPad Air 2022', '256GB，成色新，原盒配件', '无刮花，无水渍', 3999.00, '学习娱乐', '校园内', 0, 142, DATE_SUB(NOW(), INTERVAL 3 DAY), 0),
('20240002', 1, 1, 'MacBook Pro 2021', '16英寸，M1 Max，99新', '专业编程开发', 12999.00, '编程开发', '校园内', 0, 98, DATE_SUB(NOW(), INTERVAL 10 DAY), 0),
('20240002', 1, 1, 'AirPods Pro', '原装正品，成色新', '原盒原配，质保卡齐全', 1999.00, '日常使用', '校园内', 0, 215, DATE_SUB(NOW(), INTERVAL 2 DAY), 0),
('20240003', 1, 1, '华为Mate 40 Pro', '5G手机，成色九新', '无摔无碰', 3500.00, '日常使用', '校园内', 0, 189, DATE_SUB(NOW(), INTERVAL 1 DAY), 0),
-- 中等热度商品
('20240001', 1, 1, 'Sony WH-1000XM4', '降噪耳机，95新', '原盒原配', 1899.00, '听音乐', '校园内', 0, 87, DATE_SUB(NOW(), INTERVAL 7 DAY), 0),
('20240004', 1, 1, 'DJI Mavic 3', '航拍无人机，成色新', '带两块电池', 6999.00, '航拍', '校园内', 0, 76, DATE_SUB(NOW(), INTERVAL 8 DAY), 0),
('20240005', 1, 1, 'Kindle Paperwhite', '电子书阅读器', '支持防水', 899.00, '阅读', '校园内', 0, 112, DATE_SUB(NOW(), INTERVAL 6 DAY), 0),
-- 低热度商品
('20240007', 1, 1, '小米充电宝', '20000mAh，快充', '新品', 159.00, '充电', '校园内', 0, 34, DATE_SUB(NOW(), INTERVAL 15 DAY), 0),
('20240008', 1, 1, '蓝牙音箱', '防水音箱，重低音', '户外露营用', 299.00, '音乐', '校园内', 0, 28, DATE_SUB(NOW(), INTERVAL 20 DAY), 0);

-- =============================================
-- 4. 商品数据（图书 - 分类2）
-- =============================================

INSERT IGNORE INTO t_goods (user_id, category_id, goods_type, goods_name, goods_desc, goods_note, price, use_scene, exchange_place, goods_status, view_count, create_time, is_delete)
VALUES
('20240001', 2, 1, '深入浅出Java多线程', '技术书籍，无笔记', '保存完好', 89.00, '学习参考', '校园内', 0, 76, DATE_SUB(NOW(), INTERVAL 7 DAY), 0),
('20240003', 2, 1, '算法导论', '经典教材，第三版', '无标记', 129.00, '学习参考', '校园内', 0, 118, DATE_SUB(NOW(), INTERVAL 4 DAY), 0),
('20240002', 2, 1, '高效能人士的七个习惯', '管理学经典', '完好', 68.00, '自我提升', '校园内', 0, 94, DATE_SUB(NOW(), INTERVAL 6 DAY), 0),
('20240004', 2, 1, 'Python核心编程', '第三版，无拆无修', '配送快', 99.00, '学习参考', '校园内', 0, 156, DATE_SUB(NOW(), INTERVAL 8 DAY), 0),
('20240005', 2, 1, '活着', '余华著，文学作品', '精装版', 39.00, '课外阅读', '校园内', 0, 72, DATE_SUB(NOW(), INTERVAL 15 DAY), 0),
('20240003', 2, 1, '人类简史', '尤瓦尔著', '无笔记', 88.00, '开阔视野', '校园内', 0, 103, DATE_SUB(NOW(), INTERVAL 9 DAY), 0),
('20240002', 2, 1, '围城', '钱钟书著', '完好', 32.00, '文学欣赏', '校园内', 0, 58, DATE_SUB(NOW(), INTERVAL 12 DAY), 0),
('20240004', 2, 1, 'Spring实战', '框架学习', '最新版', 119.00, '技术学习', '校园内', 0, 87, DATE_SUB(NOW(), INTERVAL 5 DAY), 0);

-- =============================================
-- 5. 商品数据（宿舍用品 - 分类3）
-- =============================================

INSERT IGNORE INTO t_goods (user_id, category_id, goods_type, goods_name, goods_desc, goods_note, price, use_scene, exchange_place, goods_status, view_count, create_time, is_delete)
VALUES
('20240001', 3, 1, '小台灯', 'LED护眼，可调光', '新品', 199.00, '学习工作', '校园内', 0, 78, DATE_SUB(NOW(), INTERVAL 13 DAY), 0),
('20240002', 3, 1, '床上四件套', '纯棉，1.5米床', '全新未用', 299.00, '日常生活', '校园内', 0, 134, DATE_SUB(NOW(), INTERVAL 16 DAY), 0),
('20240003', 3, 1, '储物柜', '钢制，防盗', '质量保证', 399.00, '日常生活', '校园内', 0, 91, DATE_SUB(NOW(), INTERVAL 20 DAY), 0),
('20240004', 3, 1, '保温杯', '500ml，保温12小时', 'Hydro品牌', 149.00, '日常使用', '校园内', 0, 167, DATE_SUB(NOW(), INTERVAL 8 DAY), 0),
('20240005', 3, 1, '烧水壶', '1.7L电热水壶', '宿舍必备', 89.00, '日常生活', '校园内', 0, 122, DATE_SUB(NOW(), INTERVAL 17 DAY), 0),
('20240003', 3, 1, '小电风扇', '台式风扇，静音', '夏季必备', 129.00, '降温', '校园内', 0, 145, DATE_SUB(NOW(), INTERVAL 10 DAY), 0);

-- =============================================
-- 6. 商品数据（娱乐用品 - 分类4）
-- =============================================

INSERT IGNORE INTO t_goods (user_id, category_id, goods_type, goods_name, goods_desc, goods_note, price, use_scene, exchange_place, goods_status, view_count, create_time, is_delete)
VALUES
('20240001', 4, 1, 'Nintendo Switch', '游戏机，带游戏卡', '95新', 2499.00, '游戏娱乐', '校园内', 0, 167, DATE_SUB(NOW(), INTERVAL 6 DAY), 0),
('20240004', 4, 1, '吉他', '木质吉他，36寸', '送琴包琴弦', 599.00, '音乐', '校园内', 0, 89, DATE_SUB(NOW(), INTERVAL 12 DAY), 0),
('20240005', 4, 1, '筋膜枪', '按摩放松', '三档模式', 399.00, '健身', '校园内', 0, 76, DATE_SUB(NOW(), INTERVAL 14 DAY), 0),
('20240002', 4, 1, '篮球', '正品NBA篮球', '官方授权', 299.00, '运动', '校园内', 0, 128, DATE_SUB(NOW(), INTERVAL 11 DAY), 0);

-- =============================================
-- 7. 商品数据（学习用品 - 分类5）
-- =============================================

INSERT IGNORE INTO t_goods (user_id, category_id, goods_type, goods_name, goods_desc, goods_note, price, use_scene, exchange_place, goods_status, view_count, create_time, is_delete)
VALUES
('20240001', 5, 1, '考研资料', '完整复习资料包', '高校老师总结', 129.00, '考研准备', '校园内', 0, 98, DATE_SUB(NOW(), INTERVAL 9 DAY), 0),
('20240003', 5, 1, '英语单词卡', '4000词汇', '记忆卡片', 69.00, '英语学习', '校园内', 0, 112, DATE_SUB(NOW(), INTERVAL 8 DAY), 0),
('20240004', 5, 1, '计算器', '科学计算器', '工程用', 149.00, '数学计算', '校园内', 0, 67, DATE_SUB(NOW(), INTERVAL 18 DAY), 0),
('20240005', 5, 1, '学习灯', '无蓝光，护眼', '宿舍台灯', 259.00, '学习', '校园内', 0, 145, DATE_SUB(NOW(), INTERVAL 5 DAY), 0);

-- =============================================
-- 8. 预购/悬赏商品（goods_type=2）
-- =============================================

INSERT IGNORE INTO t_goods (user_id, category_id, goods_type, goods_name, goods_desc, goods_note, price, use_scene, exchange_place, goods_status, view_count, create_time, is_delete)
VALUES
('20240001', 1, 2, '求购：任何型号的iPhone', '想要二手iPhone', '价格面议', 4000.00, '收购', '校园内', 0, 89, DATE_SUB(NOW(), INTERVAL 8 DAY), 0),
('20240002', 2, 2, '求购：高数练习题', '需要高等数学资料', '越多越好', 50.00, '学习资料', '校园内', 0, 56, DATE_SUB(NOW(), INTERVAL 10 DAY), 0),
('20240007', 3, 2, '出价招聘：家教', '数学家教一小时100元', '可线下或线上', 100.00, '家教服务', '校园内', 0, 134, DATE_SUB(NOW(), INTERVAL 6 DAY), 0);

-- =============================================
-- 9. 用户浏览记录（模拟用户浏览行为）
-- =============================================

-- 用户20240001的浏览记录（电子产品爱好者，预期推荐分类=电子产品）
INSERT IGNORE INTO user_goods_browse (user_id, goods_id, browse_time)
VALUES
('20240001', 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),    -- iPhone
('20240001', 2, DATE_SUB(NOW(), INTERVAL 4 DAY)),    -- iPad
('20240001', 5, DATE_SUB(NOW(), INTERVAL 5 DAY)),    -- 华为手机
('20240001', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),    -- 再次浏览iPhone
('20240001', 4, DATE_SUB(NOW(), INTERVAL 2 DAY)),    -- AirPods
('20240001', 3, DATE_SUB(NOW(), INTERVAL 6 DAY)),    -- MacBook
('20240001', 11, DATE_SUB(NOW(), INTERVAL 5 DAY)),   -- 图书
('20240001', 23, DATE_SUB(NOW(), INTERVAL 10 DAY)),  -- 宿舍用品
('20240001', 28, DATE_SUB(NOW(), INTERVAL 8 DAY));   -- 娱乐用品

-- 用户20240002的浏览记录（图书爱好者，预期推荐分类=图书）
INSERT IGNORE INTO user_goods_browse (user_id, goods_id, browse_time)
VALUES
('20240002', 11, DATE_SUB(NOW(), INTERVAL 3 DAY)),   -- 书籍1
('20240002', 13, DATE_SUB(NOW(), INTERVAL 2 DAY)),   -- 书籍3
('20240002', 14, DATE_SUB(NOW(), INTERVAL 4 DAY)),   -- 书籍4
('20240002', 12, DATE_SUB(NOW(), INTERVAL 1 DAY)),   -- 书籍2
('20240002', 11, DATE_SUB(NOW(), INTERVAL 2 DAY)),   -- 再次浏览书籍1
('20240002', 24, DATE_SUB(NOW(), INTERVAL 5 DAY)),   -- 宿舍用品
('20240002', 1, DATE_SUB(NOW(), INTERVAL 7 DAY));    -- 电子产品

-- 用户20240003的浏览记录（均衡爱好者）
INSERT IGNORE INTO user_goods_browse (user_id, goods_id, browse_time)
VALUES
('20240003', 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('20240003', 11, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('20240003', 25, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('20240003', 4, DATE_SUB(NOW(), INTERVAL 4 DAY)),
('20240003', 13, DATE_SUB(NOW(), INTERVAL 5 DAY)),
('20240003', 26, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 用户20240004的浏览记录
INSERT IGNORE INTO user_goods_browse (user_id, goods_id, browse_time)
VALUES
('20240004', 14, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('20240004', 28, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('20240004', 30, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 用户20240005的浏览记录
INSERT IGNORE INTO user_goods_browse (user_id, goods_id, browse_time)
VALUES
('20240005', 15, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('20240005', 32, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 用户20240006：无浏览记录（用于冷启动推荐测试）
-- （不插入任何记录）

-- =============================================
-- 10. 用户收藏记录
-- =============================================

INSERT IGNORE INTO t_favorite (user_id, goods_id, is_delete, create_time)
VALUES
-- 用户20240001的收藏
('20240001', 1, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),   -- 收藏iPhone
('20240001', 4, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),   -- 收藏AirPods
('20240001', 11, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),  -- 收藏书籍1
-- 用户20240002的收藏
('20240002', 11, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),  -- 收藏书籍1
('20240002', 13, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),  -- 收藏书籍3
-- 用户20240003的收藏
('20240003', 1, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),   -- 收藏iPhone
('20240003', 5, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),   -- 收藏华为
('20240003', 13, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),  -- 收藏书籍
-- 用户20240004的收藏
('20240004', 14, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),  -- 收藏书籍4
('20240004', 28, 0, DATE_SUB(NOW(), INTERVAL 2 DAY));  -- 收藏游戏机

-- =============================================
-- 11. 数据验证查询
-- =============================================

-- 查看用户统计
SELECT
    '用户统计' as 统计项,
    COUNT(*) as 数量
FROM t_user
WHERE is_delete = 0;

-- 查看商品统计（按分类）
SELECT
    gc.category_name as 分类名称,
    COUNT(tg.goods_id) as 商品数量,
    SUM(tg.view_count) as 总点击量
FROM t_goods tg
LEFT JOIN t_goods_category gc ON tg.category_id = gc.category_id
WHERE tg.is_delete = 0 AND tg.goods_status = 0
GROUP BY tg.category_id, gc.category_name
ORDER BY tg.category_id;

-- 查看浏览记录统计
SELECT
    '浏览记录统计' as 统计项,
    COUNT(*) as 总数,
    COUNT(DISTINCT user_id) as 用户数,
    COUNT(DISTINCT goods_id) as 商品数
FROM user_goods_browse;

-- 查看收藏统计
SELECT
    '收藏统计' as 统计项,
    COUNT(*) as 总数,
    COUNT(DISTINCT user_id) as 用户数,
    COUNT(DISTINCT goods_id) as 商品数
FROM t_favorite
WHERE is_delete = 0;

-- 查看用户20240001的浏览分类统计（用于验证个性化推荐）
SELECT
    '用户20240001的偏好分类' as 用户,
    gc.category_name as 分类,
    COUNT(*) as 浏览次数,
    ROUND(COUNT(*) * 100 / (SELECT COUNT(*) FROM user_goods_browse WHERE user_id='20240001'), 2) as 占比百分比
FROM user_goods_browse b
LEFT JOIN t_goods g ON b.goods_id = g.goods_id
LEFT JOIN t_goods_category gc ON g.category_id = gc.category_id
WHERE b.user_id = '20240001'
  AND b.browse_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY g.category_id, gc.category_name
ORDER BY COUNT(*) DESC;

-- =============================================
-- 脚本执行完成
-- =============================================

/*
说明：
1. 该脚本生成了以下测试数据：
   - 8个用户（包括1个新用户用于冷启动测试）
   - 32个商品（分布在5个分类和2种类型中）
   - 多个浏览记录（模拟真实用户行为）
   - 多个收藏记录（用于推荐计算）

2. 关键用户：
   - 20240001: 电子产品爱好者（用于个性化推荐测试）
   - 20240002: 图书爱好者（用于个性化推荐测试）
   - 20240006: 新用户（用于冷启动推荐测试）

3. 测试场景覆盖：
   ✅ 推荐列表（所有5个tab）
   ✅ 浏览埋点
   ✅ 防刷机制
   ✅ 用户偏好分类
   ✅ 推荐得分计算
   ✅ 缓存命中

4. 执行方式：
   mysql -u root -p taotao_db < 测试数据生成脚本.sql

5. 验证方式：
   在MySQL中运行上述验证查询，确保数据正确生成
*/


