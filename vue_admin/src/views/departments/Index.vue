<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    <el-card shadow="always">
      <template #header>
        <div class="card-header-title">
          <span>科室信息总览与管理</span>
          <div>
            <el-button type="info" @click="handleSymptomMapping">
              症状映射管理
            </el-button>
            <el-button type="success" :icon="Plus" @click="handleCreateDepartment">
              创建新科室
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="科室名称">
          <el-input
              v-model="searchForm.name"
              placeholder="按科室名称搜索"
              clearable
              @input="debouncedSearch"
          />
        </el-form-item>
        <el-form-item label="职能描述">
          <el-input
              v-model="searchForm.description"
              placeholder="按职能描述搜索"
              clearable
              @input="debouncedSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 树形结构控制栏 -->
      <div class="tree-controls" v-if="treeData.length > 0">
        <el-button size="small" @click="expandAllSimple">展开全部</el-button>
        <el-button size="small" @click="collapseAllSimple">折叠全部</el-button>
        <span class="tree-stats">共 {{ totalElements }} 个科室</span>
      </div>

      <!-- 树形结构展示 -->
      <div class="tree-container" v-loading="loading">
        <el-tree
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            :filter-node-method="filterNode"
            node-key="id"
            :expand-on-click-node="false"
            :default-expand-all="false"
            :default-expanded-keys="[]"
            class="department-tree"
        >
          <template #default="{ node, data }">
            <div class="tree-node-content">
              <div class="node-info">
                <span class="node-name" v-html="highlightText(data.name, searchForm.name)"></span>
                <span class="node-description" v-if="data.description" v-html="highlightText(data.description, searchForm.description)"></span>
              </div>
              <div class="node-actions">
                <el-button
                    v-if="data.type === 'department'"
                    size="small"
                    :icon="View"
                    @click.stop="handleViewDetails(data)"
                >
                  查看成员
                </el-button>
                <el-button
                    v-if="data.type === 'department'"
                    size="small"
                    type="success"
                    @click.stop="handleViewLocations(data)"
                >
                  地点管理
                </el-button>
                <el-button
                    v-if="data.type === 'department'"
                    size="small"
                    type="primary"
                    :icon="Edit"
                    @click.stop="handleEdit(data)"
                >
                  编辑
                </el-button>
                <el-button
                    v-if="data.type === 'department'"
                    size="small"
                    type="danger"
                    :icon="Delete"
                    @click.stop="handleDelete(data)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </template>
        </el-tree>
        
        <el-empty v-if="!loading && treeData.length === 0" description="暂无科室数据" />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑科室信息" width="500">
      <el-form :model="currentEditDepartment" label-width="100px">
        <el-form-item label="科室名称">
          <el-input v-model="currentEditDepartment.name" />
        </el-form-item>
        <el-form-item label="科室描述">
          <el-input v-model="currentEditDepartment.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEdit">
            确认修改
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Edit, Delete, View, Search, Refresh, Plus } from '@element-plus/icons-vue';
import BackButton from '@/components/BackButton.vue';
import { useRouter } from 'vue-router';

// 导入 API 服务
import { getDepartmentPage, updateDepartmentDescription, deleteDepartmentByName, getAllParentDepartments, getDepartmentTree, deleteDepartment } from '@/api/department';

const router = useRouter();

// --- 数据状态 ---
const allDepartments = ref([]); // 存放所有科室数据
const parentDepartments = ref([]); // 父科室列表
const totalElements = ref(0); // 后端返回的总记录数
const loading = ref(false); // 数据加载状态
const treeRef = ref(null); // 树组件引用

// --- 搜索和排序状态 ---
const searchForm = reactive({
  name: '',
  description: ''
});

// --- 编辑状态 ---
const editDialogVisible = ref(false);
const currentEditDepartment = ref({});

// 树组件配置
const treeProps = {
  children: 'children',
  label: 'name'
};

// 构建树形数据
const treeData = computed(() => {
  // 使用新的树形数据接口，后端已经过滤了ID=999的节点
  return allDepartments.value || [];
});

// 核心方法：从后端获取数据
const fetchDepartments = async () => {
    loading.value = true;
    try {
        // 使用新的树形数据接口
        const response = await getDepartmentTree();

        console.log('--- 调试日志 start ---');
        console.log('1. API 原始返回的树形数据:', response);

        // 处理后端返回的树形数据
        totalElements.value = response ? response.length : 0;
        allDepartments.value = response || [];

        console.log('2. 处理后的树形数据:', allDepartments.value);
        
        // 调试：检查节点ID
        if (allDepartments.value && allDepartments.value.length > 0) {
            console.log('3. 第一个节点的详细信息:', allDepartments.value[0]);
            console.log('4. 第一个节点的ID:', allDepartments.value[0]?.id);
            console.log('5. 第一个节点的类型:', typeof allDepartments.value[0]?.id);
            
            // 检查所有节点的ID
            const allKeys = getAllNodeKeys(allDepartments.value);
            console.log('6. 所有节点键值:', allKeys);
            console.log('7. 键值类型:', allKeys.map(key => typeof key));
        }
        
        console.log('--- 调试日志 end ---');

    } catch (error) {
        console.error('获取科室数据失败:', error);
        ElMessage.error('获取科室数据失败: ' + (error.message || '未知错误'));
    } finally {
        loading.value = false;
    }
};

// 获取父科室列表
const fetchParentDepartments = async () => {
    try {
        const response = await getAllParentDepartments();
        parentDepartments.value = response || [];
        console.log('父科室列表:', parentDepartments.value);
    } catch (error) {
        console.error('获取父科室列表失败:', error);
        ElMessage.error('获取父科室列表失败: ' + (error.message || '未知错误'));
    }
};

// 树节点过滤方法
const filterNode = (value, data) => {
    if (!value) return true;
    
    const searchValue = value.toLowerCase();
    const matchesName = data.name.toLowerCase().includes(searchValue);
    const matchesDescription = data.description && data.description.toLowerCase().includes(searchValue);
    
    // 如果是父节点，检查是否有子节点匹配
    if (data.children && data.children.length > 0) {
        const hasMatchingChildren = data.children.some(child => 
            child.name.toLowerCase().includes(searchValue) ||
            (child.description && child.description.toLowerCase().includes(searchValue))
        );
        return matchesName || matchesDescription || hasMatchingChildren;
    }
    
    return matchesName || matchesDescription;
};

// 防抖搜索
let searchTimeout = null;
const debouncedSearch = () => {
    if (searchTimeout) {
        clearTimeout(searchTimeout);
    }
    searchTimeout = setTimeout(() => {
        handleSearch();
    }, 300);
};

// 搜索处理
const handleSearch = async () => {
    await fetchDepartments();
    
    // 如果有搜索条件，应用过滤
    if (searchForm.name || searchForm.description) {
        await nextTick();
        if (treeRef.value) {
            treeRef.value.filter(searchForm.name || searchForm.description);
        }
    } else {
        // 清除过滤
        if (treeRef.value) {
            treeRef.value.filter('');
        }
    }
};

// 重置搜索
const resetSearch = async () => {
    searchForm.name = '';
    searchForm.description = '';
    await fetchDepartments();
};

// 展开全部节点
const expandAll = () => {
    console.log('开始展开全部节点...');
    console.log('树组件引用:', treeRef.value);
    console.log('树形数据:', treeData.value);
    
    if (!treeRef.value) {
        console.warn('树组件引用不存在');
        return;
    }
    
    // 方法1：使用 setExpandedKeys
    try {
        const allKeys = getAllNodeKeys(treeData.value);
        console.log('找到的节点键值:', allKeys);
        treeRef.value.setExpandedKeys(allKeys);
        console.log('使用setExpandedKeys展开成功');
    } catch (error) {
        console.error('setExpandedKeys失败:', error);
        
        // 方法2：使用 setExpanded 逐个展开
        try {
            const allKeys = getAllNodeKeys(treeData.value);
            allKeys.forEach(key => {
                treeRef.value.setExpanded(key, true);
            });
            console.log('使用setExpanded展开成功');
        } catch (error2) {
            console.error('setExpanded也失败:', error2);
        }
    }
};

// 折叠全部节点
const collapseAll = () => {
    console.log('开始折叠全部节点...');
    console.log('树组件引用:', treeRef.value);
    console.log('树形数据:', treeData.value);
    
    if (!treeRef.value) {
        console.warn('树组件引用不存在');
        return;
    }
    
    // 方法1：使用 setExpandedKeys 设置为空数组
    try {
        treeRef.value.setExpandedKeys([]);
        console.log('使用setExpandedKeys折叠成功');
    } catch (error) {
        console.error('setExpandedKeys失败:', error);
        
        // 方法2：使用 setExpanded 逐个折叠
        try {
            const allKeys = getAllNodeKeys(treeData.value);
            allKeys.forEach(key => {
                treeRef.value.setExpanded(key, false);
            });
            console.log('使用setExpanded折叠成功');
        } catch (error2) {
            console.error('setExpanded也失败:', error2);
        }
    }
};

// 获取所有节点键值
const getAllNodeKeys = (nodes) => {
    let keys = [];
    if (!nodes || !Array.isArray(nodes)) {
        return keys;
    }
    
    nodes.forEach(node => {
        // 确保节点有有效的id
        if (node.id !== null && node.id !== undefined) {
            keys.push(node.id);
        }
        if (node.children && node.children.length > 0) {
            keys = keys.concat(getAllNodeKeys(node.children));
        }
    });
    return keys;
};

// 简化的展开全部方法
const expandAllSimple = () => {
    if (treeRef.value) {
        // 直接设置所有节点为展开状态
        const allKeys = getAllNodeKeys(treeData.value);
        console.log('简化展开方法，键值:', allKeys);
        treeRef.value.setExpandedKeys(allKeys);
    }
};

// 简化的折叠全部方法
const collapseAllSimple = () => {
    if (treeRef.value) {
        // 直接设置所有节点为折叠状态
        console.log('简化折叠方法');
        treeRef.value.setExpandedKeys([]);
    }
};

// 搜索高亮
const highlightText = (text, searchTerm) => {
    if (!searchTerm || !text) return text;
    
    const regex = new RegExp(`(${searchTerm})`, 'gi');
    return text.replace(regex, '<mark class="search-highlight">$1</mark>');
};

// 创建科室
const handleCreateDepartment = () => {
    router.push('/departments/create');
};

// 症状映射管理
const handleSymptomMapping = () => {
    router.push('/departments/symptom-mapping');
};

// 查看成员
const handleViewDetails = (row) => {
    // 只有子科室才能查看成员
    if (row.type === 'department') {
        router.push(`/departments/members/${row.id}`);
    }
};

// 地点管理
const handleViewLocations = (row) => {
    // 只有子科室才能管理地点
    if (row.type === 'department') {
        router.push(`/departments/locations/${row.id}`);
    }
};

// 编辑科室
const handleEdit = (row) => {
    currentEditDepartment.value = { ...row };
    editDialogVisible.value = true;
};

// 提交编辑
const submitEdit = async () => {
    try {
        await updateDepartmentDescription(currentEditDepartment.value);
        ElMessage.success('科室信息更新成功');
        editDialogVisible.value = false;
        await fetchDepartments(); // 刷新列表
    } catch (error) {
        console.error('更新科室信息失败:', error);
        ElMessage.error('更新科室信息失败: ' + (error.message || '未知错误'));
    }
};

// 删除科室
const handleDelete = async (row) => {
  try {
    // 构建确认消息
    let confirmMessage = `确定要删除科室 "${row.name}" 吗？`;
    
    // 如果是子科室，添加额外提示
    if (row.type === 'department') {
      confirmMessage += '\n\n注意：';
      confirmMessage += '\n• 如果该科室下有医生，将自动移入"未分配科室"';
      confirmMessage += '\n• 如果该科室下有地点，将设置为未分配状态';
      confirmMessage += '\n• 该科室有未来排班时，无法删除';
    }
    
    confirmMessage += '\n\n此操作不可恢复。';

    await ElMessageBox.confirm(
      confirmMessage,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    // 调用新的删除接口
    const result = await deleteDepartment(row.id);
    
    // 显示删除结果
    let successMsg = '科室删除成功！';
    const details = [];
    
    if (result.doctorCount > 0) {
      details.push(`已将 ${result.doctorCount} 位医生移动到未分配科室`);
    }
    if (result.locationCount > 0) {
      details.push(`已将 ${result.locationCount} 个地点设置为未分配状态`);
    }
    
    if (details.length > 0) {
      successMsg += ' ' + details.join('，');
    }
    
    ElMessage.success(successMsg);
    
    await fetchDepartments(); // 刷新列表
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除科室失败:', error);
      ElMessage.error('删除科室失败: ' + (error.message || '未知错误'));
    }
  }
};

// 组件挂载时获取数据
onMounted(() => {
    fetchParentDepartments();
    fetchDepartments();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.card-header-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.tree-controls {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.tree-stats {
  margin-left: auto;
  color: #606266;
  font-size: 14px;
}

.tree-container {
  margin-top: 20px;
  min-height: 400px;
}

.department-tree {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
}

.tree-node-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 5px 0;
}

.node-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-name {
  font-weight: 500;
  color: #303133;
}

.node-id {
  font-size: 12px;
  color: #909399;
}

.node-description {
  font-size: 12px;
  color: #606266;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-actions {
  display: flex;
  gap: 5px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node-content:hover .node-actions {
  opacity: 1;
}

.pagination-info {
  margin-top: 20px;
  text-align: center;
  color: #606266;
  font-size: 14px;
}

.dialog-footer {
  text-align: right;
}

/* 树节点样式优化 */
:deep(.el-tree-node__content) {
  height: auto;
  min-height: 40px;
  padding: 8px 0;
}

:deep(.el-tree-node__expand-icon) {
  margin-right: 8px;
}

:deep(.el-tree-node__label) {
  font-size: 14px;
}

/* 搜索高亮样式 */
.search-highlight {
  background-color: #ffeb3b;
  color: #333;
  padding: 1px 2px;
  border-radius: 2px;
  font-weight: bold;
}
</style>