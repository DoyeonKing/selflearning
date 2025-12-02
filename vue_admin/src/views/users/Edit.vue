<template>
  <div class="app-container">
    <div class="back-area" style="margin-bottom: 12px;">
      <BackButton />
    </div>
    
    <h2>编辑用户信息</h2>
    
    <el-card shadow="always">
      <!-- 用户类型选择 -->
      <div style="margin-bottom: 20px;">
        <el-radio-group v-model="searchType" @change="handleTypeChange">
          <el-radio-button label="patient">患者</el-radio-button>
          <el-radio-button label="doctor">医生</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item :label="searchType === 'patient' ? '学号' : '工号'">
          <el-input v-model="searchForm.id" clearable placeholder="请输入学号/工号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" clearable placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item v-if="searchType === 'doctor'" label="科室">
          <el-select v-model="searchForm.departmentId" clearable placeholder="请选择科室">
            <el-option
              v-for="dept in departments"
              :key="dept.departmentId"
              :label="dept.name"
              :value="dept.departmentId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 用户列表表格 -->
      <el-table :data="userList" border stripe v-loading="loading">
        <!-- 患者列表列 -->
        <template v-if="searchType === 'patient'">
          <el-table-column prop="patientId" label="患者ID" width="100" />
          <el-table-column prop="identifier" label="学号" width="120" />
          <el-table-column prop="fullName" label="姓名" width="120" />
          <el-table-column prop="phoneNumber" label="手机号" width="130">
            <template #default="{ row }">
              {{ formatPhoneNumber(row.phoneNumber) }}
            </template>
          </el-table-column>
          <el-table-column prop="patientType" label="患者类型" width="100">
            <template #default="{ row }">
              {{ row.patientType === 'STUDENT' || row.patientType === 'student' ? '学生' : 
                 row.patientType === 'TEACHER' || row.patientType === 'teacher' ? '教师' : 
                 row.patientType === 'STAFF' || row.patientType === 'staff' ? '员工' : 
                 row.patientType }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="(row.status === 'ACTIVE' || row.status === 'active') ? 'success' : 'danger'">
                {{ (row.status === 'ACTIVE' || row.status === 'active') ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
        </template>

        <!-- 医生列表列 -->
        <template v-else>
          <el-table-column prop="doctorId" label="医生ID" width="100" />
          <el-table-column prop="identifier" label="工号" width="120" />
          <el-table-column prop="fullName" label="姓名" width="120" />
          <el-table-column prop="title" label="职称" width="100" />
          <el-table-column prop="phoneNumber" label="手机号" width="130">
            <template #default="{ row }">
              {{ formatPhoneNumber(row.phoneNumber) }}
            </template>
          </el-table-column>
          <el-table-column prop="departmentName" label="科室" width="120" />
          <el-table-column prop="specialty" label="专长" min-width="150" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="(row.status === 'ACTIVE' || row.status === 'active') ? 'success' : 'danger'">
                {{ (row.status === 'ACTIVE' || row.status === 'active') ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
        </template>

        <!-- 操作列 -->
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm
              title="确定要删除该账户吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :layout="searchType === 'patient' ? 'total, prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
        :total="pagination.total"
        @size-change="handleSearch"
        @current-change="handleSearch"
        style="margin-top: 20px; justify-content: center;"
      />
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="`编辑${currentUser.role === 'PATIENT' ? '患者' : '医生'}信息`"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef"
        :model="editForm" 
        :rules="formRules"
        label-width="100px"
        v-loading="saveLoading"
      >
        <!-- 患者表单 -->
        <template v-if="currentUser.role === 'PATIENT'">
          <el-form-item label="学号" prop="identifier">
            <el-input v-model="editForm.identifier" />
          </el-form-item>
          <el-form-item label="姓名" prop="fullName">
            <el-input v-model="editForm.fullName" />
          </el-form-item>
          <el-form-item label="手机号" prop="phoneNumber">
            <el-input v-model="editForm.phoneNumber" />
          </el-form-item>
          <el-form-item label="患者类型" prop="patientType">
            <el-select v-model="editForm.patientType" placeholder="请选择患者类型">
              <el-option label="学生" value="student" />
              <el-option label="教师" value="teacher" />
              <el-option label="员工" value="staff" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="editForm.status" placeholder="请选择状态">
              <el-option label="正常" value="active" />
              <el-option label="禁用" value="inactive" />
              <el-option label="已删除" value="deleted" />
            </el-select>
          </el-form-item>
          <el-form-item label="重置密码">
            <el-input 
              v-model="editForm.password" 
              type="password" 
              placeholder="不修改请留空"
              clearable
            />
          </el-form-item>
        </template>

        <!-- 医生表单 -->
        <template v-else>
          <el-form-item label="工号" prop="identifier">
            <el-input v-model="editForm.identifier" />
          </el-form-item>
          <el-form-item label="姓名" prop="fullName">
            <el-input v-model="editForm.fullName" />
          </el-form-item>
          <el-form-item label="手机号" prop="phoneNumber">
            <el-input v-model="editForm.phoneNumber" />
          </el-form-item>
          <el-form-item label="职称" prop="title">
            <el-input v-model="editForm.title" />
          </el-form-item>
          <el-form-item label="科室" prop="departmentId">
            <el-select v-model="editForm.departmentId" placeholder="请选择科室">
              <el-option
                v-for="dept in departments"
                :key="dept.departmentId"
                :label="dept.name"
                :value="dept.departmentId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="专长" prop="specialty">
            <el-input v-model="editForm.specialty" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="简介" prop="bio">
            <el-input v-model="editForm.bio" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="照片URL" prop="photoUrl">
            <el-input v-model="editForm.photoUrl" placeholder="请输入照片URL" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="editForm.status" placeholder="请选择状态">
              <el-option label="正常" value="active" />
              <el-option label="禁用" value="inactive" />
              <el-option label="锁定" value="locked" />
              <el-option label="已删除" value="deleted" />
            </el-select>
          </el-form-item>
          <el-form-item label="重置密码">
            <el-input 
              v-model="editForm.password" 
              type="password" 
              placeholder="不修改请留空"
              clearable
            />
          </el-form-item>
        </template>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import BackButton from '@/components/BackButton.vue';
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { searchPatients, searchDoctors, updateUser, deleteUser } from '@/api/user';
import { getAllDepartments } from '@/api/department';

// 搜索类型
const searchType = ref('patient');

// 搜索表单
const searchForm = reactive({
  id: '',
  name: '',
  departmentId: null
});

// 加载状态
const loading = ref(false);
const saveLoading = ref(false);

// 用户列表
const userList = ref([]);

// 分页信息
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

// 科室列表
const departments = ref([]);

// 对话框
const dialogVisible = ref(false);
const formRef = ref(null);

// 当前编辑的用户
const currentUser = ref({});

// 编辑表单
const editForm = reactive({
  identifier: '',
  fullName: '',
  phoneNumber: '',
  patientType: '',
  status: '',
  password: '',
  title: '',
  departmentId: null,
  specialty: '',
  bio: '',
  photoUrl: ''
});

// 表单验证规则
const formRules = {
  identifier: [{ required: true, message: '请输入学号/工号', trigger: 'blur' }],
  fullName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phoneNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
};

// 切换用户类型
const handleTypeChange = () => {
  // 重置搜索表单
  searchForm.id = '';
  searchForm.name = '';
  searchForm.departmentId = null;
  
  // 重置分页
  pagination.page = 1;
  if (searchType.value === 'patient') {
    pagination.pageSize = 10; // 患者搜索固定每页10条
  } else {
    pagination.pageSize = 10;
  }
  
  // 重新搜索
  handleSearch();
};

// 搜索用户
const handleSearch = async () => {
  loading.value = true;
  
  try {
    let response;
    if (searchType.value === 'patient') {
      // 患者搜索：后端固定每页10条，不传pageSize
      response = await searchPatients({
        id: searchForm.id,
        name: searchForm.name,
        page: pagination.page
      });
    } else {
      // 医生搜索：支持自定义pageSize
      response = await searchDoctors({
        id: searchForm.id,
        name: searchForm.name,
        departmentId: searchForm.departmentId,
        page: pagination.page,
        pageSize: pagination.pageSize
      });
    }

    if (response && response.content) {
      // 处理数据
      if (searchType.value === 'patient') {
        userList.value = response.content.map(item => {
          const details = item.userDetails;
          return {
            ...details,
            role: item.role,
            userId: details.patientId
          };
        });
      } else {
        userList.value = response.content.map(item => {
          const details = item.userDetails;
          return {
            ...details,
            role: item.role,
            userId: details.doctorId,
            // 添加科室名称
            departmentName: details.department?.name || '未分配'
          };
        });
      }
      
      pagination.total = response.totalElements || 0;
    } else {
      userList.value = [];
      pagination.total = 0;
    }
  } catch (error) {
    console.error('搜索失败:', error);
    ElMessage.error('搜索失败: ' + (error.message || '未知错误'));
    userList.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

// 重置搜索
const handleReset = () => {
  searchForm.id = '';
  searchForm.name = '';
  searchForm.departmentId = null;
  pagination.page = 1;
  handleSearch();
};

// 加载科室列表
const loadDepartments = async () => {
  try {
    const response = await getAllDepartments();
    // 后端返回的是分页数据，需要从 content 字段中获取列表
    departments.value = response?.content || [];
  } catch (error) {
    console.error('加载科室列表失败:', error);
    departments.value = [];
  }
};

// 打开编辑对话框
const openEditDialog = (row) => {
  currentUser.value = { ...row };
  
  // 填充表单
  if (row.role === 'PATIENT') {
    editForm.identifier = row.identifier || '';
    editForm.fullName = row.fullName || '';
    editForm.phoneNumber = formatPhoneNumber(row.phoneNumber) || '';
    editForm.patientType = row.patientType || '';
    editForm.status = row.status || '';
    editForm.password = '';
  } else {
    editForm.identifier = row.identifier || '';
    editForm.fullName = row.fullName || '';
    editForm.phoneNumber = formatPhoneNumber(row.phoneNumber) || '';
    editForm.title = row.title || '';
    // 修复：正确获取科室ID
    editForm.departmentId = row.department?.departmentId || row.departmentId || null;
    editForm.specialty = row.specialty || '';
    editForm.bio = row.bio || '';
    editForm.photoUrl = row.photoUrl || '';
    editForm.status = row.status || '';
    editForm.password = '';
  }
  
  dialogVisible.value = true;
};

// 保存用户信息
const handleSave = async () => {
  // 表单验证
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
  } catch (error) {
    return;
  }

  saveLoading.value = true;
  try {
    const userId = currentUser.value.userId;
    const role = currentUser.value.role;

    // 构建更新请求数据
    const updateData = {
      role: role,
      patientUpdateRequest: null,
      doctorUpdateRequest: null
    };

    if (role === 'DOCTOR') {
      updateData.doctorUpdateRequest = {
        departmentId: editForm.departmentId,
        identifier: editForm.identifier,
        fullName: editForm.fullName,
        phoneNumber: editForm.phoneNumber,
        title: editForm.title,
        specialty: editForm.specialty,
        bio: editForm.bio,
        photoUrl: editForm.photoUrl,
        // 后端枚举是小写：active, inactive, locked, deleted
        status: (() => {
          const status = editForm.status ? editForm.status.toLowerCase().trim() : 'active';
          return isValidStatus(status) ? status : 'active';
        })()
      };
      // 如果密码不为空，才添加密码字段
      if (editForm.password) {
        updateData.doctorUpdateRequest.password = editForm.password;
      }
    } else if (role === 'PATIENT') {
      updateData.patientUpdateRequest = {
        identifier: editForm.identifier,
        fullName: editForm.fullName,
        phoneNumber: editForm.phoneNumber,
        // 后端枚举是小写：student, teacher, staff
        patientType: editForm.patientType ? editForm.patientType.toLowerCase() : 'student',
        // 后端枚举是小写：active, inactive, locked, deleted
        status: (() => {
          const status = editForm.status ? editForm.status.toLowerCase().trim() : 'active';
          return isValidStatus(status) ? status : 'active';
        })()
      };
      // 如果密码不为空，才添加密码字段
      if (editForm.password) {
        updateData.patientUpdateRequest.password = editForm.password;
      }
    }

    // 调试：打印实际传递的数据
    console.log('更新用户数据:', updateData);
    console.log('患者状态值:', updateData.patientUpdateRequest?.status);
    console.log('医生状态值:', updateData.doctorUpdateRequest?.status);
    
    await updateUser(userId, updateData);
    
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    
    // 刷新列表
    await handleSearch();
  } catch (error) {
    console.error('保存失败:', error);
    ElMessage.error('保存失败: ' + (error.message || '未知错误'));
  } finally {
    saveLoading.value = false;
  }
};

// 删除用户处理函数
const handleDelete = async (row) => {
  try {
    loading.value = true;
    await deleteUser(row.userId, row.role);
    ElMessage.success('删除成功');
    await handleSearch(); // 刷新列表
  } catch (error) {
    ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message));
  } finally {
    loading.value = false;
  }
};

// 验证状态值是否有效
const isValidStatus = (status) => {
  const validStatuses = ['active', 'inactive', 'locked', 'deleted'];
  return validStatuses.includes(status);
};

// 格式化手机号，去掉+86前缀
const formatPhoneNumber = (phone) => {
  if (!phone) return '-';
  // 去掉+86前缀
  return phone.replace(/^\+86/, '');
};

// 初始化
onMounted(async () => {
  await loadDepartments();
  await handleSearch();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.back-area {
  margin-bottom: 12px;
}
</style>
