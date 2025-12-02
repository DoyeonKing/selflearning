-- 为 doctors 表添加 title_level 字段
-- 用于记录医生职称等级：主任医师(0)、副主任医师(1)、主治医师(2)

-- 1. 添加 title_level 字段
ALTER TABLE `doctors`
ADD COLUMN `title_level` TINYINT UNSIGNED COMMENT '职称等级：0-主任医师，1-副主任医师，2-主治医师' AFTER `title`;

-- 2. 根据现有 title 字段更新 title_level 值
UPDATE `doctors`
SET `title_level` = CASE
    WHEN `title` LIKE '%主任医师%' AND `title` NOT LIKE '%副%' THEN 0  -- 主任医师
    WHEN `title` LIKE '%副主任医师%' THEN 1                              -- 副主任医师
    WHEN `title` LIKE '%主治医师%' THEN 2                                -- 主治医师
    ELSE NULL  -- 其他职称暂不设置等级
END;

-- 3. 查看更新结果
SELECT 
    doctor_id,
    full_name,
    title,
    title_level,
    CASE title_level
        WHEN 0 THEN '主任医师'
        WHEN 1 THEN '副主任医师'
        WHEN 2 THEN '主治医师'
        ELSE '未设置'
    END AS level_description
FROM `doctors`
ORDER BY title_level, doctor_id;
