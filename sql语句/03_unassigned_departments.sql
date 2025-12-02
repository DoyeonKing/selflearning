-- 科室内医生管理功能 - 数据库前提条件设置
-- 约定使用 ID=999 来表示"未分配"状态

-- 操作 A: 在父科室表中新增"未分配父科室"记录
INSERT IGNORE INTO `parent_departments` (`parent_department_id`, `name`, `description`) VALUES
(999, '未分配父科室', '用于存放未分配到具体父科室的医生');

-- 操作 B: 在子科室表中新增"未分配科室"记录
INSERT IGNORE INTO `departments` (`department_id`, `parent_id`, `name`, `description`) VALUES
(999, 999, '未分配科室', '用于存放未分配到具体科室的医生');

-- 验证插入结果
SELECT 'parent_departments' as table_name, parent_department_id, name FROM parent_departments WHERE parent_department_id = 999
UNION ALL
SELECT 'departments' as table_name, department_id, name FROM departments WHERE department_id = 999;
