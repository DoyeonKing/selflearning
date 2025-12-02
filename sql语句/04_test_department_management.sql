-- 科室内医生管理功能测试脚本

-- 1. 验证未分配科室记录是否存在
SELECT '=== 验证未分配科室记录 ===' as test_step;
SELECT 'parent_departments' as table_name, parent_department_id, name FROM parent_departments WHERE parent_department_id = 999
UNION ALL
SELECT 'departments' as table_name, department_id, name FROM departments WHERE department_id = 999;

-- 2. 查看现有科室结构
SELECT '=== 现有科室结构 ===' as test_step;
SELECT 
    pd.parent_department_id as parent_id,
    pd.name as parent_name,
    d.department_id,
    d.name as department_name
FROM parent_departments pd
LEFT JOIN departments d ON pd.parent_department_id = d.parent_id
WHERE pd.parent_department_id != 999
ORDER BY pd.parent_department_id, d.department_id;

-- 3. 查看现有医生分布
SELECT '=== 现有医生分布 ===' as test_step;
SELECT 
    d.identifier,
    d.full_name,
    d.title,
    dept.name as department_name,
    pdept.name as parent_department_name
FROM doctors d
LEFT JOIN departments dept ON d.department_id = dept.department_id
LEFT JOIN parent_departments pdept ON dept.parent_id = pdept.parent_department_id
ORDER BY dept.department_id, d.identifier;

-- 4. 测试：将某个医生移动到未分配科室
SELECT '=== 测试：移动医生到未分配科室 ===' as test_step;
-- 选择一个医生进行测试（假设医生ID=1）
UPDATE doctors 
SET department_id = 999 
WHERE doctor_id = 1;

-- 验证更新结果
SELECT 
    d.identifier,
    d.full_name,
    dept.name as department_name
FROM doctors d
LEFT JOIN departments dept ON d.department_id = dept.department_id
WHERE d.doctor_id = 1;

-- 5. 测试：将医生重新分配到具体科室
SELECT '=== 测试：重新分配医生到具体科室 ===' as test_step;
-- 将医生重新分配到呼吸内科（假设department_id=1）
UPDATE doctors 
SET department_id = 1 
WHERE doctor_id = 1;

-- 验证更新结果
SELECT 
    d.identifier,
    d.full_name,
    dept.name as department_name
FROM doctors d
LEFT JOIN departments dept ON d.department_id = dept.department_id
WHERE d.doctor_id = 1;

-- 6. 查看各科室的医生数量统计
SELECT '=== 各科室医生数量统计 ===' as test_step;
SELECT 
    dept.department_id,
    dept.name as department_name,
    pdept.name as parent_department_name,
    COUNT(d.doctor_id) as doctor_count
FROM departments dept
LEFT JOIN parent_departments pdept ON dept.parent_id = pdept.parent_department_id
LEFT JOIN doctors d ON dept.department_id = d.department_id
WHERE dept.department_id != 999
GROUP BY dept.department_id, dept.name, pdept.name
ORDER BY pdept.parent_department_id, dept.department_id;
