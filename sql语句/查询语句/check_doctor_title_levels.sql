-- 检查医生职称等级设置情况
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
WHERE status = 'active'
ORDER BY title_level, doctor_id;

-- 特别检查李亚辉和杨红光的职称
SELECT 
    doctor_id,
    full_name,
    title,
    title_level
FROM `doctors`
WHERE full_name IN ('李亚辉', '杨红光', '杨红光');
