# 室内导航子系统使用说明

## 功能概述

室内导航子系统实现了从"挂号预约"到"诊室导航"的完整业务闭环，包括：
- 地图网格数据管理
- A*路径规划算法
- 实时导航动画
- Canvas地图绘制

## 安装依赖

### 方式一：使用内置的简化A*算法（推荐）

代码中已经内置了简化的A*路径规划算法实现（`SimpleAStar`类），无需安装额外依赖，可以直接使用。

### 方式二：使用pathfinding库（可选）

如果需要使用完整的pathfinding库，可以执行以下命令：

```bash
cd uni_app
npm install pathfinding
```

然后在 `index.vue` 中替换导入：

```javascript
// 替换内置的SimpleAStar类
import PF from 'pathfinding'

// 在calculatePath方法中使用：
const grid = new PF.Grid(this.gridWidth, this.gridHeight)
// 设置障碍物
for (let y = 0; y < this.gridHeight; y++) {
  for (let x = 0; x < this.gridWidth; x++) {
    if (this.gridMatrix[y][x] === 1) {
      grid.setWalkableAt(x, y, false)
    }
  }
}

const finder = new PF.AStarFinder()
const path = finder.findPath(
  this.startNode.x,
  this.startNode.y,
  this.targetNode.x,
  this.targetNode.y,
  grid
)
```

## 使用方式

### 1. 从预约详情页跳转

在预约详情页点击"导航到诊室"按钮，系统会自动：
- 获取预约对应的locationId
- 跳转到导航页面
- 自动计算并显示路径

### 2. 直接传递locationId

```javascript
uni.navigateTo({
  url: `/pages/navigation/index?locationId=1`
})
```

### 3. 传递appointmentId（自动获取locationId）

```javascript
uni.navigateTo({
  url: `/pages/navigation/index?appointmentId=123`
})
```

## 功能特性

### 1. 地图配置
- 40x30网格地图
- 预设障碍物（墙壁）
- 关键节点（大门、分诊台、电梯、诊室）

### 2. 路径规划
- A*算法自动寻路
- 避障功能
- 支持对角线移动

### 3. 可视化
- Canvas绘制地图和路径
- 起点（蓝色圆点）
- 终点（红色圆点）
- 当前位置（橙色圆点，动态移动）
- 路径线（绿色）

### 4. 调试模式
- 点击"开启/关闭调试网格"按钮
- 显示障碍物位置（半透明红色）
- 方便查看避障效果

### 5. 实时信息
- 显示目标诊室名称
- 显示剩余距离（米）
- 动态更新

## 地图节点说明

当前预设的节点：
- Node 1: "医院大门" (20, 29) - 固定起点
- Node 2: "分诊台" (20, 20)
- Node 3: "电梯口" (35, 20)
- Node 4: "内科诊室" (5, 10) - locationId=1
- Node 5: "外科诊室" (5, 5) - locationId=2

## 注意事项

1. **背景图片**：需要准备医院平面图，放置在 `/static/images/hospital_floor_1.jpg`
   - 如果图片不存在，Canvas仍会正常显示路径
   - 建议图片尺寸与Canvas尺寸匹配（750x600px）

2. **网格坐标**：
   - X轴：0-39（40列）
   - Y轴：0-29（30行）
   - 坐标原点在左上角

3. **路径计算**：
   - 如果起点和终点之间没有通路，会提示"无法找到路径"
   - 确保诊室节点不在障碍物包围中

4. **性能优化**：
   - 地图数据在服务启动时初始化
   - 路径计算在客户端进行，减少服务器压力

## API接口

### 获取地图配置
```
GET /api/map/config
```

响应：
```json
{
  "code": "200",
  "data": {
    "grid": {
      "width": 40,
      "height": 30,
      "gridMatrix": [[0,1,0,...], ...]
    },
    "nodes": [
      {
        "nodeId": 1,
        "name": "医院大门",
        "x": 20,
        "y": 29,
        "locationId": null
      },
      ...
    ]
  }
}
```

### 获取目标节点
```
GET /api/map/target/{locationId}
```

响应：
```json
{
  "code": "200",
  "data": {
    "nodeId": 4,
    "name": "内科诊室",
    "x": 5,
    "y": 10,
    "locationId": 1
  }
}
```

## 扩展开发

### 添加新节点

在 `MapService.java` 的 `initializeMapData()` 方法中添加：

```java
nodes.add(new MapNodeDTO(6, "新诊室", 10, 15, 3));
```

### 修改地图布局

在 `MapService.java` 的 `initializeMapData()` 方法中修改 `gridMatrix` 的设置。

### 自定义起点

在导航页面的 `loadTargetNode()` 方法中修改：

```javascript
// 可以改为其他节点，比如分诊台
this.startNode = this.nodes.find(n => n.nodeId === 2) || { x: 20, y: 20 }
```


















