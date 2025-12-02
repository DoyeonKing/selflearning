-- 查询数据库中所有引用 department_id 的外键约束
-- 这将帮助我们了解哪些表依赖 departments 表

-- 1. 查询所有外键约束，特别是引用 departments 表的
SELECT 
    TABLE_NAME as '引用表名',
    COLUMN_NAME as '引用列名',
    CONSTRAINT_NAME as '约束名称',
    REFERENCED_TABLE_NAME as '被引用表名',
    REFERENCED_COLUMN_NAME as '被引用列名'
FROM 
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE 
    REFERENCED_TABLE_NAME = 'departments'
    AND TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, COLUMN_NAME;

-- 2. 查询所有包含 department_id 列的表
SELECT 
    TABLE_NAME as '表名',
    COLUMN_NAME as '列名',
    DATA_TYPE as '数据类型',
    IS_NULLABLE as '允许空值',
    COLUMN_DEFAULT as '默认值',
    COLUMN_COMMENT as '注释'
FROM 
    INFORMATION_SCHEMA.COLUMNS 
WHERE 
    COLUMN_NAME LIKE '%department%'
    AND TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, COLUMN_NAME;

-- 3. 查询所有外键约束（包括可能的命名模式）
SELECT 
    TABLE_NAME as '表名',
    COLUMN_NAME as '列名',
    CONSTRAINT_NAME as '约束名',
    REFERENCED_TABLE_NAME as '引用表',
    REFERENCED_COLUMN_NAME as '引用列'
FROM 
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE 
    TABLE_SCHEMA = DATABASE()
    AND REFERENCED_TABLE_NAME IS NOT NULL
    AND (COLUMN_NAME LIKE '%department%' OR REFERENCED_TABLE_NAME = 'departments')
ORDER BY TABLE_NAME, COLUMN_NAME;

-- 4. 检查具体的表结构，看看哪些表有 department_id 列
SHOW TABLES;

-- 5. 检查每个可能包含 department_id 的表
DESCRIBE doctors;
DESCRIBE locations;
DESCRIBE symptom_department_mapping;
DESCRIBE schedules;
DESCRIBE appointments;
DESCRIBE waitlist;

-- 6. 查询具体的数据，看看哪些表有数据
SELECT 'doctors表' as table_name, COUNT(*) as record_count FROM doctors
UNION ALL
SELECT 'locations表', COUNT(*) FROM locations  
UNION ALL
SELECT 'symptom_department_mapping表', COUNT(*) FROM symptom_department_mapping
UNION ALL
SELECT 'schedules表', COUNT(*) FROM schedules
UNION ALL
SELECT 'appointments表', COUNT(*) FROM appointments
UNION ALL
SELECT 'waitlist表', COUNT(*) FROM waitlist;

-- 7. 检查特定科室ID=5的依赖关系
SELECT '=== 科室ID=5的依赖关系检查 ===' as info;

SELECT 'doctors表中引用科室ID=5的记录数' as dependency_type, COUNT(*) as count 
FROM doctors WHERE department_id = 5
UNION ALL
SELECT 'locations表中引用科室ID=5的记录数', COUNT(*) 
FROM locations WHERE department_id = 5
UNION ALL
SELECT 'symptom_department_mapping表中引用科室ID=5的记录数', COUNT(*) 
FROM symptom_department_mapping WHERE department_id = 5
UNION ALL
SELECT 'schedules表中通过doctor_id间接引用科室ID=5的记录数', COUNT(*) 
FROM schedules s 
JOIN doctors d ON s.doctor_id = d.doctor_id 
WHERE d.department_id = 5;
