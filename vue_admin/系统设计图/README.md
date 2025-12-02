# 医院排班系统 - UML设计图文档

## 📋 文档概述

本目录包含医院排班系统的完整UML设计图，基于PlantUML格式编写。这些图表详细展示了系统的业务流程、数据结构和交互逻辑。

---

## 📁 文件列表

### 1. 活动图
- **文件名**: `1_活动图_排班系统流程.puml`
- **描述**: 完整的业务流程活动图
- **包含内容**:
  - 单个排班创建流程
  - 批量排班导入流程
  - 冲突检测流程
  - 患者查看排班流程
  - 医生请假流程
  - 排班修改与取消流程

### 2. 类图
- **文件名**: `2_类图_数据库表结构.puml`
- **描述**: 数据库表结构及关系类图
- **包含内容**:
  - 核心业务表：`schedules`（排班表）
  - 医生相关表：`doctors`、`departments`、`parent_departments`
  - 资源表：`time_slots`、`locations`
  - 业务关联表：`leave_requests`、`appointments`、`patients`、`users`
  - 表间关系及外键约束

### 3. 时序图 - 单个排班创建
- **文件名**: `3_时序图_单个排班创建.puml`
- **描述**: 场景一 - 管理员创建单个班次的完整交互流程
- **关键步骤**:
  1. 初始化页面，加载医生/时间段/诊室列表
  2. 管理员填写排班信息
  3. 前后端数据验证
  4. 检查冲突（医生同时间段重复排班）
  5. 插入数据库并返回结果
  6. 自动触发冲突检测

### 4. 时序图 - 批量排班导入
- **文件名**: `4_时序图_批量排班导入.puml`
- **描述**: 场景二 - 批量排班与Excel导入的完整流程
- **关键步骤**:
  1. 下载Excel模板
  2. 管理员填写Excel数据
  3. 上传并解析Excel文件
  4. 逐行验证数据（医生工号、时间段ID、地点ID）
  5. 检查每条记录的冲突
  6. 批量插入数据库
  7. 返回导入结果统计
  8. 自动冲突检测并高亮显示

### 5. 时序图 - 患者查看排班
- **文件名**: `5_时序图_患者查看排班.puml`
- **描述**: 场景三 - 患者查看可预约排班的流程
- **关键步骤**:
  1. 患者访问挂号系统
  2. 选择科室和日期
  3. 后端执行多表联合查询（JOIN 6张表）
  4. 返回完整排班信息（医生、科室、时间、地点、费用、剩余号源）
  5. 患者查看并选择排班
  6. 跳转到预约流程

### 6. 时序图 - 医生请假流程
- **文件名**: `6_时序图_医生请假流程.puml`
- **描述**: 场景四 - 医生请假申请与排班自动取消的流程
- **关键步骤**:
  1. 医生提交请假申请（插入`leave_requests`表）
  2. 系统通知管理员
  3. 管理员审批请假
  4. 查找与请假时间冲突的排班
  5. 检查冲突排班是否有患者预约
  6. 自动取消冲突排班，更新预约状态
  7. 通知受影响的患者并标记退款
  8. 更新请假申请状态为已批准

### 7. 时序图 - 排班修改与取消
- **文件名**: `7_时序图_排班修改与取消.puml`
- **描述**: 场景五 - 排班修改与取消的完整流程
- **关键步骤**:
  - **修改排班**:
    1. 管理员选择并编辑排班
    2. 验证修改的合理性（新号源≥已预约数）
    3. 更新数据库
    4. 通知已预约患者地点变更
  - **取消排班**:
    1. 检查是否有患者预约
    2. 如有预约，通知患者并标记退款
    3. 更新排班状态为已取消
    4. 记录操作日志
    5. 重新触发冲突检测

### 8. 时序图 - 冲突检测系统
- **文件名**: `8_时序图_冲突检测系统.puml`
- **描述**: 完整的冲突检测系统工作流程
- **检测的冲突类型**:
  1. **医生重复排班** (Critical) - 同一医生同一时间多次排班
  2. **办公室冲突** (Critical) - 同一办公室同一天被多人使用
  3. **医生跨办公室** (Critical) - 同一医生同一时段在多个办公室
  4. **工作时间过长** (Warning) - 连续工作超过7天
  5. **时间段班次不匹配** (Warning) - 上午时间段在下午班次
- **检测算法**:
  - 使用Map数据结构进行高效冲突检测
  - 按冲突类型分别执行检测逻辑
  - 汇总并分类冲突（严重/警告）
  - 前端根据冲突数据进行高亮显示

---

## 🔧 如何查看这些图表

### 方法1：使用在线工具
1. 访问 [PlantUML Online Editor](http://www.plantuml.com/plantuml/uml/)
2. 复制 `.puml` 文件内容
3. 粘贴到编辑器中
4. 自动生成图表

### 方法2：使用VS Code插件
1. 安装 `PlantUML` 插件
2. 打开 `.puml` 文件
3. 按 `Alt + D` 预览图表

### 方法3：使用IDEA插件
1. 安装 `PlantUML integration` 插件
2. 打开 `.puml` 文件
3. 右键选择 "Show PlantUML Diagram"

### 方法4：命令行生成
```bash
# 安装 PlantUML (需要Java环境)
# 下载 plantuml.jar

# 生成PNG图片
java -jar plantuml.jar *.puml

# 生成SVG图片
java -jar plantuml.jar -tsvg *.puml
```

---

## 📊 核心业务流程说明

### 排班创建的核心逻辑

排班系统的核心是 `schedules` 表，它将以下要素组合在一起：
- **谁** (doctor_id) - 哪个医生
- **何时** (schedule_date + slot_id) - 什么时间
- **何地** (location_id) - 在哪个诊室
- **多少** (total_slots, booked_slots) - 号源数量
- **费用** (fee) - 挂号费

### 数据流转路径

```
管理员操作
    ↓
前端表单验证
    ↓
后端API接收
    ↓
业务逻辑验证
    ↓
数据库操作 (INSERT/UPDATE)
    ↓
冲突检测引擎
    ↓
返回结果 + 冲突信息
    ↓
前端高亮显示
```

### 多表联合查询示例

当患者查看排班时，系统执行以下JOIN查询：

```sql
SELECT
    s.schedule_id,
    d.full_name AS doctor_name,          -- doctors表
    d.title AS doctor_title,              -- doctors表
    p.name AS parent_department,          -- parent_departments表
    dep.name AS department_name,          -- departments表
    t.slot_name AS time_period,           -- time_slots表
    l.name AS location_name,              -- locations表
    s.fee,                                -- schedules表
    (s.total_slots - s.booked_slots) AS remaining_slots
FROM schedules s
JOIN doctors d ON s.doctor_id = d.doctor_id
JOIN departments dep ON d.department_id = dep.department_id
JOIN parent_departments p ON dep.parent_id = p.parent_department_id
JOIN time_slots t ON s.slot_id = t.slot_id
JOIN locations l ON s.location_id = l.location_id
WHERE s.schedule_date = '2025-11-10'
  AND s.status = 'available'
  AND (s.total_slots - s.booked_slots) > 0;
```

---

## 🎯 关键设计模式

### 1. 状态机模式
排班状态流转：
```
available (可用) 
    ↓
full (已满) - 当 booked_slots = total_slots
    ↓
cancelled (已取消) - 管理员取消或医生请假
```

### 2. 观察者模式
- 医生请假被批准 → 自动取消相关排班 → 通知患者
- 排班被修改 → 自动检测冲突 → 更新界面高亮

### 3. 策略模式
冲突检测使用多种检测策略：
- `detectDoctorDoubleBooking()` - 医生重复排班策略
- `detectOfficeConflicts()` - 办公室冲突策略
- `detectWorkDurationConflicts()` - 工作时间策略
- 等等...

### 4. 命令模式
批量导入操作：
- 每行Excel数据 → 一个命令对象
- 支持验证、执行、回滚
- 记录成功/失败结果

---

## 📈 性能优化考虑

### 1. 数据库索引
建议为以下字段创建索引：
```sql
-- schedules表
CREATE INDEX idx_schedules_doctor_date ON schedules(doctor_id, schedule_date);
CREATE INDEX idx_schedules_date_status ON schedules(schedule_date, status);
CREATE INDEX idx_schedules_location_date ON schedules(location_id, schedule_date);

-- appointments表
CREATE INDEX idx_appointments_schedule ON appointments(schedule_id, status);
```

### 2. 缓存策略
- 时间段列表（`time_slots`）- 很少变化，可以缓存
- 诊室列表（`locations`）- 较少变化，可以缓存
- 科室树形结构 - 可以缓存
- 冲突检测结果 - 短时间缓存（5分钟）

### 3. 批量操作优化
- Excel导入使用批量INSERT而非逐条插入
- 冲突检测一次性加载所有数据，避免N+1查询

---

## 🔐 安全考虑

### 1. 权限控制
- 管理员：完整的排班CRUD权限
- 医生：只能提交请假申请，查看自己的排班
- 患者：只能查看可用排班，不能修改

### 2. 数据验证
- 前端验证：非空、格式检查
- 后端验证：业务逻辑、数据一致性
- 数据库约束：外键、唯一性

### 3. 操作日志
所有关键操作都应记录：
- 谁（操作用户）
- 何时（操作时间）
- 做了什么（操作类型）
- 影响范围（修改的数据）

---

## 🚀 扩展性设计

### 未来可扩展的功能

1. **智能排班算法**
   - 根据医生工作量自动分配
   - 考虑医生偏好和特长
   - 优化号源利用率

2. **排班模板**
   - 保存常用的排班模式
   - 一键应用模板
   - 批量生成多周排班

3. **数据分析**
   - 医生工作量统计
   - 号源利用率分析
   - 患者就诊高峰时段分析

4. **移动端支持**
   - 医生移动端查看排班
   - 患者移动端预约
   - 推送通知

---

## 📞 联系与反馈

如果您对这些设计图有任何问题或建议，请通过以下方式联系：

- 📧 Email: [您的邮箱]
- 💬 Issue: [项目Issue地址]
- 📱 微信: [您的微信号]

---

## 📝 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2025-10-20 | 初始版本，包含完整的UML设计图 |

---

**最后更新**: 2025年10月20日  
**作者**: 医院排班系统开发团队  
**文档格式**: PlantUML (Markdown)



