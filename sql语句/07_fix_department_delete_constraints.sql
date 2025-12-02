-- 修复删除科室时的外键约束问题
-- 问题：symptom_department_mapping 表中有 department_id 外键约束

-- 1. 检查哪些表引用了 departments 表
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM 
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE 
    REFERENCED_TABLE_NAME = 'departments'
    AND TABLE_SCHEMA = DATABASE();

-- 2. 检查 symptom_department_mapping 表中的数据
SELECT '=== symptom_department_mapping 表数据 ===' as info;
SELECT 
    mapping_id,
    symptom_keywords,
    department_id,
    priority
FROM symptom_department_mapping
ORDER BY department_id;

-- 3. 检查 locations 表中的数据（也可能有外键约束）
SELECT '=== locations 表数据 ===' as info;
SELECT 
    location_id,
    location_name,
    department_id,
    floor_level,
    building,
    room_number
FROM locations
ORDER BY department_id;

-- 4. 修复删除科室逻辑的SQL脚本
-- 在删除科室前，需要先处理相关的外键约束

-- 方案1：将 symptom_department_mapping 中的记录移动到未分配科室
UPDATE symptom_department_mapping 
SET department_id = 999 
WHERE department_id = ?; -- 替换为要删除的科室ID

-- 方案2：删除 symptom_department_mapping 中的记录
-- DELETE FROM symptom_department_mapping WHERE department_id = ?;

-- 方案3：将 locations 中的记录移动到未分配科室（如果有外键约束）
-- UPDATE locations 
-- SET department_id = 999 
-- WHERE department_id = ?;

-- 5. 检查特定科室的依赖关系
SELECT '=== 检查科室ID=5的依赖关系 ===' as info;

-- 检查医生
SELECT '医生依赖:' as type, COUNT(*) as count 
FROM doctors 
WHERE department_id = 5;

-- 检查症状映射
SELECT '症状映射依赖:' as type, COUNT(*) as count 
FROM symptom_department_mapping 
WHERE department_id = 5;

-- 检查诊室
SELECT '诊室依赖:' as type, COUNT(*) as count 
FROM locations 
WHERE department_id = 5;

-- 6. 完整的删除科室流程（示例：删除科室ID=5）
-- 步骤1：移动医生到未分配科室
UPDATE doctors 
SET department_id = 999 
WHERE department_id = 5;

-- 步骤2：移动症状映射到未分配科室
UPDATE symptom_department_mapping 
SET department_id = 999 
WHERE department_id = 5;

-- 步骤3：移动诊室到未分配科室（如果需要）
-- UPDATE locations 
-- SET department_id = 999 
-- WHERE department_id = 5;

-- 步骤4：删除科室
-- DELETE FROM departments WHERE department_id = 5;
