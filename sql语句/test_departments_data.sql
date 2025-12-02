-- 测试科室数据插入脚本
-- 用于验证科室下拉选择功能

-- 1. 插入父科室数据
INSERT IGNORE INTO `parent_departments` (`parent_department_id`, `name`, `description`) VALUES
(1, '内科', '内科系统科室'),
(2, '外科', '外科系统科室'),
(3, '妇产科', '妇产科系统科室'),
(4, '儿科', '儿科系统科室'),
(5, '急诊科', '急诊科系统科室');

-- 2. 插入子科室数据
INSERT IGNORE INTO `departments` (`department_id`, `parent_id`, `name`, `description`) VALUES
(1, 1, '呼吸内科', '呼吸系统疾病诊疗'),
(2, 1, '心血管内科', '心血管疾病诊疗'),
(3, 1, '消化内科', '消化系统疾病诊疗'),
(4, 1, '神经内科', '神经系统疾病诊疗'),
(5, 2, '普外科', '普通外科手术'),
(6, 2, '骨科', '骨科疾病诊疗'),
(7, 2, '泌尿外科', '泌尿系统疾病诊疗'),
(8, 3, '妇科', '妇科疾病诊疗'),
(9, 3, '产科', '产科诊疗'),
(10, 4, '小儿内科', '小儿内科疾病诊疗'),
(11, 4, '小儿外科', '小儿外科疾病诊疗'),
(12, 5, '急诊内科', '急诊内科诊疗'),
(13, 5, '急诊外科', '急诊外科诊疗');

-- 3. 验证插入结果
SELECT '=== 父科室列表 ===' as info;
SELECT parent_department_id, name, description FROM parent_departments ORDER BY parent_department_id;

SELECT '=== 子科室列表 ===' as info;
SELECT 
    d.department_id,
    d.name as department_name,
    pd.name as parent_name
FROM departments d
LEFT JOIN parent_departments pd ON d.parent_id = pd.parent_department_id
ORDER BY d.department_id;
