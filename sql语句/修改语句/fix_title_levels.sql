-- 修复医生职称等级
-- 确保所有医生都有正确的 title_level 设置

-- 1. 检查当前状态
SELECT '=== 当前医生职称状态 ===' AS info;
SELECT 
    doctor_id,
    full_name,
    title,
    title_level
FROM `doctors`
WHERE status = 'active'
ORDER BY doctor_id;

-- 2. 更新所有医生的 title_level
UPDATE `doctors`
SET `title_level` = CASE
    WHEN `title` LIKE '%主任医师%' AND `title` NOT LIKE '%副%' THEN 0  -- 主任医师
    WHEN `title` LIKE '%副主任医师%' THEN 1                              -- 副主任医师
    WHEN `title` LIKE '%主治医师%' THEN 2                                -- 主治医师
    WHEN `title` LIKE '%住院医师%' THEN 3                                -- 住院医师
    ELSE 2  -- 默认设置为主治医师级别
END
WHERE status = 'active';

-- 3. 特别处理：如果 title 为空或未知，根据医生姓名手动设置
-- 示例：假设李亚辉是副主任医师，杨红光是主任医师
UPDATE `doctors` SET `title` = '副主任医师', `title_level` = 1 WHERE full_name = '李亚辉';
UPDATE `doctors` SET `title` = '主任医师', `title_level` = 0 WHERE full_name = '杨红光';

-- 4. 查看更新后的结果
SELECT '=== 更新后的医生职称状态 ===' AS info;
SELECT 
    doctor_id,
    full_name,
    title,
    title_level,
    CASE title_level
        WHEN 0 THEN '主任医师'
        WHEN 1 THEN '副主任医师'
        WHEN 2 THEN '主治医师'
        WHEN 3 THEN '住院医师'
        ELSE '未设置'
    END AS level_description
FROM `doctors`
WHERE status = 'active'
ORDER BY title_level, doctor_id;

-- 5. 验证关键医生
SELECT '=== 关键医生验证 ===' AS info;
SELECT 
    doctor_id,
    full_name,
    title,
    title_level
FROM `doctors`
WHERE full_name IN ('李亚辉', '杨红光')
ORDER BY doctor_id;
