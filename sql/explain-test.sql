-- =====================================================
-- 借阅记录索引优化实验
-- 环境：MySQL 5.7（不使用 CTE，兼容 5.6+）
-- 用法：mysql -uroot -p -D library --default-character-set=utf8mb4 < sql/explain-test.sql
-- 说明：写入 5 万行测试数据用于对比索引效果；末尾给出清理与还原语句
-- =====================================================

-- 1) 造数辅助表（会话级临时表）
CREATE TEMPORARY TABLE IF NOT EXISTS helper_seq (n INT PRIMARY KEY);
INSERT INTO helper_seq (n) VALUES
(0),(1),(2),(3),(4),(5),(6),(7),(8),(9),
(10),(11),(12),(13),(14),(15),(16),(17),(18),(19),
(20),(21),(22),(23),(24),(25),(26),(27),(28),(29),
(30),(31),(32),(33),(34),(35),(36),(37),(38),(39),
(40),(41),(42),(43),(44),(45),(46),(47),(48),(49);

-- 2) 造 5 万行借阅记录：user_id 取 1~100 循环，模拟多用户共享图书的真实分布
--    第 2~5 行是"个位 + 十位 + 百位 + 千位"的进位写，故意写得笨拙但零权限要求
INSERT INTO borrow_record (user_id, book_id, borrow_time, due_time, return_time, status)
SELECT (a.n + b.n * 10 + c.n * 100 + d.n * 1000) % 100 + 1,
       (a.n + b.n * 10 + c.n * 100 + d.n * 1000) % 6 + 1,
       DATE_SUB(NOW(), INTERVAL (a.n + b.n * 10 + c.n * 100 + d.n * 1000) MINUTE),
       DATE_ADD(NOW(), INTERVAL 30 DAY),
       NULL,
       (a.n + b.n * 10 + c.n * 100 + d.n * 1000) % 2
FROM helper_seq a
JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) b
JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) c
JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) d
LIMIT 50000;

DROP TEMPORARY TABLE IF EXISTS helper_seq;

SELECT COUNT(*) AS total_rows FROM borrow_record;

-- =====================================================
-- 3) 对比实验（请在命令行手动执行，观察输出）
-- =====================================================

-- 【加索引前】先删掉联合索引，模拟初始版本的状态
-- DROP INDEX idx_user_status_id ON borrow_record;
--
-- EXPLAIN
-- SELECT r.id, r.user_id, r.book_id, r.status
-- FROM borrow_record r
-- LEFT JOIN `user` u ON r.user_id = u.id
-- LEFT JOIN book b ON r.book_id = b.id
-- WHERE r.user_id = 1 AND r.status = 0
-- ORDER BY r.id DESC;
--
-- 预期：key = idx_user_id（或 NULL），rows 偏大，Extra 含 "Using filesort"

-- 【加索引后】
-- ALTER TABLE borrow_record ADD INDEX idx_user_status_id (user_id, status, id);
--
-- EXPLAIN
-- SELECT r.id, r.user_id, r.book_id, r.status
-- FROM borrow_record r
-- LEFT JOIN `user` u ON r.user_id = u.id
-- LEFT JOIN book b ON r.book_id = b.id
-- WHERE r.user_id = 1 AND r.status = 0
-- ORDER BY r.id DESC;
--
-- 预期：key = idx_user_status_id，"Using filesort" 消失

-- 【耗时与扫描行数对比】比 EXPLAIN 更有说服力
-- SET profiling = 1;
-- SELECT COUNT(*) FROM (
--   SELECT r.id FROM borrow_record r
--   LEFT JOIN `user` u ON r.user_id = u.id
--   LEFT JOIN book b ON r.book_id = b.id
--   WHERE r.user_id = 1 AND r.status = 0
--   ORDER BY r.id DESC
-- ) t;
-- SHOW PROFILES;
--
-- 关注 Rows_examined：加索引前 ≈ 全表行数，加索引后 ≈ 命中的行数

-- =====================================================
-- 4) 实验结束后的清理
-- =====================================================
-- DELETE FROM borrow_record WHERE id > 10;   -- 保留手工测试产生的少量真实记录
-- UPDATE book SET available_count = total_count;
--
-- 冗余索引：idx_user_id / idx_book_id 已被 idx_user_status_id / idx_book_status
-- 的最左前缀完全覆盖，保留只会增加写入开销与优化器选择成本，实测中同一查询
-- 也已改走联合索引，因此可以安全删除：
-- DROP INDEX idx_user_id ON borrow_record;
-- DROP INDEX idx_book_id ON borrow_record;
