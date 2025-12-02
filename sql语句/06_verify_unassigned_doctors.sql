-- 验证未分配医生功能
-- 检查数据库中department_id=999的医生

-- 1. 检查未分配科室是否存在
SELECT '=== 检查未分配科室 ===' as test_step;
SELECT department_id, name, description 
FROM departments 
WHERE department_id = 999;

-- 2. 检查未分配科室下的医生
SELECT '=== 检查未分配医生 ===' as test_step;
SELECT 
    d.doctor_id,
    d.identifier,
    d.full_name,
    d.title,
    d.phone_number,
    d.specialty,
    d.status,
    dept.name as department_name
FROM doctors d
LEFT JOIN departments dept ON d.department_id = dept.department_id
WHERE d.department_id = 999;

-- 3. 如果查询结果为空，插入测试数据
SELECT '=== 插入测试未分配医生 ===' as test_step;

-- 确保未分配科室存在
INSERT IGNORE INTO `departments` (`department_id`, `parent_id`, `name`, `description`) VALUES
(999, 999, '未分配科室', '用于存放未分配科室的医生');

-- 插入测试医生到未分配科室
INSERT IGNORE INTO `doctors` (`doctor_id`, `department_id`, `identifier`, `password_hash`, `full_name`, `phone_number`, `title`, `specialty`, `status`) VALUES
(9991, 999, 'UN001', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '测试医生1', '+8613810099001', '主治医师', '全科医学', 'active'),
(9992, 999, 'UN002', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '测试医生2', '+8613810099002', '副主任医师', '内科', 'active');

-- 4. 再次验证
SELECT '=== 验证插入结果 ===' as test_step;
SELECT 
    d.doctor_id,
    d.identifier,
    d.full_name,
    d.title,
    d.phone_number,
    d.specialty,
    d.status,
    dept.name as department_name
FROM doctors d
LEFT JOIN departments dept ON d.department_id = dept.department_id
WHERE d.department_id = 999;

-- 5. 统计所有科室的医生数量
SELECT '=== 各科室医生统计 ===' as test_step;
SELECT 
    dept.department_id,
    dept.name as department_name,
    COUNT(d.doctor_id) as doctor_count
FROM departments dept
LEFT JOIN doctors d ON dept.department_id = d.department_id
GROUP BY dept.department_id, dept.name
ORDER BY dept.department_id;
