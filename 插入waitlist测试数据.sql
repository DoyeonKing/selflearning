-- =============================================
-- waitlist (候补表) 测试数据插入脚本
-- 基于现有数据库内容生成
-- =============================================

-- 查看当前排班情况（参考）
-- schedule_id 1: 李明医生 (呼吸内科) - 10个号源，已约3个，剩余7个
-- schedule_id 6: 张建国医生 (心血管内科) - 8个号源，已约6个，剩余2个 (接近满号)
-- schedule_id 12: 周伟医生 (普外科) - 10个号源，已约7个，剩余3个 (接近满号)
-- schedule_id 14: 杨光医生 (眼科) - 15个号源，已约8个，剩余7个

-- 患者信息参考：
-- patient_id 1: 张三 (学生)
-- patient_id 2: 李四 (学生)
-- patient_id 3: 王五 (学生)
-- patient_id 4: 赵教授 (教师)
-- patient_id 5: 钱老师 (教师)
-- patient_id 6: 孙师傅 (职工)
-- patient_id 7: 周小华 (学生)
-- patient_id 8: 吴小明 (学生)
-- patient_id 9: 郑教授 (教师)
-- patient_id 10: 王阿姨 (职工)

-- 清空现有候补数据（如果需要）
-- DELETE FROM `waitlist`;

-- 插入候补测试数据
INSERT INTO `waitlist` (`waitlist_id`, `patient_id`, `schedule_id`, `status`, `notification_sent_at`, `created_at`) VALUES

-- 1. 张建国医生（心血管内科专家）的候补 - schedule_id=6（接近满号，热门）
(1, 3, 6, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 王五候补张建国医生，2天前加入，还在等待中

(2, 7, 6, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 周小华候补张建国医生，1天前加入，还在等待中

(3, 9, 6, 'notified', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- 郑教授候补张建国医生，3天前加入，3小时前已通知有号源

-- 2. 周伟医生（普外科专家）的候补 - schedule_id=12（接近满号）
(4, 5, 12, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
-- 钱老师候补周伟医生，5小时前加入，等待中

(5, 8, 12, 'expired', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- 吴小明候补周伟医生，5天前加入，已过期（未及时预约）

(6, 10, 12, 'booked', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 王阿姨候补周伟医生，2天前加入，1天前通知并已成功预约（候补转正）

-- 3. 杨光医生（眼科）的候补 - schedule_id=14（较热门）
(7, 2, 14, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
-- 李四候补杨光医生，6小时前加入，等待中

(8, 4, 14, 'notified', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 赵教授候补杨光医生，1天前加入，30分钟前已通知

-- 4. 李明医生（呼吸内科专家）的候补 - schedule_id=1
(9, 6, 1, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
-- 孙师傅候补李明医生，4小时前加入，等待中

-- 5. 刘强医生（消化内科）的候补 - schedule_id=10
(10, 7, 10, 'expired', NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),
-- 周小华候补刘强医生，7天前加入，已过期

(11, 3, 10, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
-- 王五候补刘强医生，12小时前加入，等待中

-- 6. 陈静医生（心血管内科）的候补 - schedule_id=8
(12, 5, 8, 'booked', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- 钱老师候补陈静医生，3天前加入，2天前通知并已预约（候补成功）

(13, 9, 8, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
-- 郑教授候补陈静医生，8小时前加入，等待中

-- 7. 更多不同状态的候补记录
(14, 1, 7, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
-- 张三候补张建国医生（另一个排班），1小时前加入

(15, 2, 5, 'notified', DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 10 HOUR)),
-- 李四候补王小红医生，10小时前加入，15分钟前已通知

(16, 4, 3, 'expired', NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
-- 赵教授候补李明医生（另一个排班），10天前加入，因条件不符被拒绝（用expired表示）

(17, 6, 13, 'waiting', NULL, NOW()),
-- 孙师傅候补孙志强医生（骨科），刚刚加入

(18, 8, 9, 'waiting', NULL, DATE_SUB(NOW(), INTERVAL 3 HOUR));
-- 吴小明候补陈静医生（另一个排班），3小时前加入

-- 验证插入结果
SELECT 
    w.waitlist_id,
    w.patient_id,
    p.full_name AS patient_name,
    w.schedule_id,
    d.full_name AS doctor_name,
    dept.name AS department_name,
    w.status,
    w.notification_sent_at,
    w.created_at
FROM waitlist w
JOIN patients p ON w.patient_id = p.patient_id
JOIN schedules s ON w.schedule_id = s.schedule_id
JOIN doctors d ON s.doctor_id = d.doctor_id
JOIN departments dept ON d.department_id = dept.department_id
ORDER BY w.created_at DESC;

-- 按状态统计候补记录
SELECT 
    status AS '候补状态',
    COUNT(*) AS '记录数'
FROM waitlist
GROUP BY status
ORDER BY 
    CASE status
        WHEN 'waiting' THEN 1
        WHEN 'notified' THEN 2
        WHEN 'booked' THEN 3
        WHEN 'expired' THEN 4
    END;

-- 查看最热门的排班（候补人数最多）
SELECT 
    s.schedule_id,
    d.full_name AS doctor_name,
    dept.name AS department,
    s.schedule_date,
    COUNT(w.waitlist_id) AS waitlist_count
FROM schedules s
LEFT JOIN waitlist w ON s.schedule_id = w.schedule_id
JOIN doctors d ON s.doctor_id = d.doctor_id
JOIN departments dept ON d.department_id = dept.department_id
GROUP BY s.schedule_id, d.full_name, dept.name, s.schedule_date
HAVING waitlist_count > 0
ORDER BY waitlist_count DESC;

