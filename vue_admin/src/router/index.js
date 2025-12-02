import { createRouter, createWebHistory } from 'vue-router';

// 导入主入口
import AdminIndex from '@/views/AdminIndex.vue';
import AdminLogin from '@/views/AdminLogin.vue';
import Debug from '@/views/Debug.vue';

// 导入科室管理页面组件
import DepartmentIndex from '@/views/departments/Index.vue';
import CreateDepartment from '@/views/departments/CreateDepartment.vue';
// 【已移除】ManageDepartments 和 DepartmentView 导入，因为文件已删除
import DepartmentMembers from '@/views/departments/Members.vue'; // 保留：科室成员页面
import DepartmentLocations from '@/views/departments/Locations.vue'; // 科室地点管理页面
import SymptomMapping from '@/views/departments/SymptomMapping.vue'; // 症状映射管理页面

// 导入用户管理页面组件
import UserIndex from '@/views/users/Index.vue';
import CreateUser from '@/views/users/Create.vue';
import ImportPatient from '@/views/users/ImportPatient.vue';
import ImportDoctor from '@/views/users/ImportDoctor.vue';
import SearchUser from '@/views/users/Search.vue';
import EditUser from '@/views/users/Edit.vue';
import UserHistory from '@/views/users/History.vue';

// ===== 新增：导入排班管理页面组件 =====
import ShiftManagement from '@/views/scheduling/ShiftManagement.vue';
import ScheduleDashboard from '@/views/scheduling/ScheduleDashboard.vue';
import ScheduleFeeManagement from '@/views/scheduling/ScheduleFeeManagement.vue';
import AutoSchedule from '@/views/scheduling/AutoSchedule.vue';
import DoctorWorkHours from '@/views/scheduling/DoctorWorkHours.vue';

// ===== 新增：导入就医规范管理页面组件 =====
import RegulationIndex from '@/views/regulations/Index.vue';
import CreateRegulation from '@/views/regulations/Create.vue';
import EditRegulation from '@/views/regulations/Edit.vue';

// ===== 新增：导入费用管理页面组件 =====
import FeeManagement from '@/views/fees/FeeManagement.vue';

// ===== 新增：导入签到管理页面组件 =====
// import CheckIn from '@/views/CheckIn.vue';
const CheckIn = () => import('@/views/CheckIn.vue');
// ===== 新增：导入现场服务页面组件 =====
const OnSiteService = () => import('@/views/OnSiteService.vue');
// ===== 新增：导入数据大屏页面组件 =====
import StatsCanvas from '@/views/dashboard/StatsCanvas.vue';

// ===== 新增：导入请假审批页面组件 =====
import LeaveApproval from '@/views/LeaveApproval.vue';
import SubstituteSelection from '@/views/SubstituteSelection.vue';

// ===== 新增：导入加号审批页面组件 =====
import SlotApproval from '@/views/SlotApproval.vue';

// 通用视图组件导入
const NotFoundView = () => import('../views/404.vue');

const routes = [
    // 登录页面
    { path: '/login', name: 'AdminLogin', component: AdminLogin, meta: { title: '管理员登录' } },

    // 调试页面
    { path: '/debug', name: 'Debug', component: Debug, meta: { title: '调试页面' } },

    // 根路径
    {
        path: '/',
        name: 'AdminIndex',
        component: AdminIndex,
        meta: { title: '首页 - 医院后台管理', requiresAuth: true }
    },

    // =======================================================
    // 科室管理路由
    // =======================================================
    {
        path: '/departments',
        name: 'DepartmentIndex',
        meta: { title: '科室信息总览', requiresAuth: true },
        component: DepartmentIndex
    },
    {
        path: '/departments/create',
        name: 'CreateDepartment',
        meta: { title: '创建新科室', requiresAuth: true },
        component: CreateDepartment
    },
    {
        path: '/departments/members/:id', // 必须使用 :id 接收科室 ID 参数
        name: 'DepartmentMembers',
        meta: { title: '科室成员管理', requiresAuth: true },
        component: DepartmentMembers
    },
    {
        path: '/departments/locations/:id', // 必须使用 :id 接收科室 ID 参数
        name: 'DepartmentLocations',
        meta: { title: '科室地点管理', requiresAuth: true },
        component: DepartmentLocations
    },
    {
        path: '/departments/symptom-mapping',
        name: 'SymptomMapping',
        meta: { title: '症状映射管理', requiresAuth: true },
        component: SymptomMapping
    },

    // =======================================================
    // 用户管理路由
    // =======================================================
    {
        path: '/users',
        name: 'UserIndex',
        meta: { title: '用户管理', requiresAuth: true },
        component: UserIndex
    },
    {
        path: '/users/create',
        name: 'CreateUser',
        meta: { title: '创建新用户', requiresAuth: true },
        component: CreateUser
    },
    {
        path: '/users/import-patient',
        name: 'ImportPatient',
        meta: { title: '批量导入患者', requiresAuth: true },
        component: ImportPatient
    },
    {
        path: '/users/import-doctor',
        name: 'ImportDoctor',
        meta: { title: '批量导入医生', requiresAuth: true },
        component: ImportDoctor
    },
    {
        path: '/users/search',
        name: 'SearchUser',
        meta: { title: '搜索用户信息', requiresAuth: true },
        component: SearchUser
    },
    {
        path: '/users/edit',
        name: 'EditUser',
        meta: { title: '编辑用户信息', requiresAuth: true },
        component: EditUser
    },
    {
        path: '/users/history',
        name: 'UserHistory',
        meta: { title: '修改用户病史', requiresAuth: true },
        component: UserHistory
    },
    // =======================================================

    // ===== 排班管理相关的所有路由 =====
    {
        path: '/scheduling/shifts',
        name: 'ShiftManagement',
        meta: { title: '班次定义与管理', requiresAuth: true },
        component: ShiftManagement
    },
    {
        path: '/scheduling/dashboard',
        name: 'ScheduleDashboard',
        meta: { title: '排班看板', requiresAuth: true },
        component: ScheduleDashboard
    },
    {
        path: '/scheduling/fee-management',
        name: 'ScheduleFeeManagement',
        meta: { title: '号别管理', requiresAuth: true },
        component: ScheduleFeeManagement
    },
    {
        path: '/scheduling/auto-schedule',
        name: 'AutoSchedule',
        meta: { title: '自动排班', requiresAuth: true },
        component: AutoSchedule
    },
        
    {
        path: '/scheduling/doctor-hours',
        name: 'DoctorWorkHours',
        meta: { title: '医生工时统计', requiresAuth: true },
        component: DoctorWorkHours
    },
    // =======================================================

    // ===== 就医规范管理相关路由 =====
    {
        path: '/regulations',
        name: 'RegulationIndex',
        meta: { title: '就医规范管理', requiresAuth: true },
        component: RegulationIndex
    },
    {
        path: '/regulations/create',
        name: 'CreateRegulation',
        meta: { title: '新增就医规范', requiresAuth: true },
        component: CreateRegulation
    },
    {
        path: '/regulations/edit/:id',
        name: 'EditRegulation',
        meta: { title: '编辑就医规范', requiresAuth: true },
        component: EditRegulation
    },
    // =======================================================

    // ===== 费用规则管理路由 =====
    {
        path: '/fees',
        name: 'FeeManagement',
        meta: { title: '费用规则管理', requiresAuth: true },
        component: FeeManagement
    },
    // =======================================================

    // ===== 数据大屏路由 =====
    {
        path: '/dashboard/stats',
        name: 'DashboardStats',
        meta: { title: '运营数据大屏', requiresAuth: true },
        component: StatsCanvas
    },
    // =======================================================

    // ===== 患者签到路由 =====
    {
        path: '/check-in',
        name: 'CheckIn',
        meta: { title: '患者签到', requiresAuth: true },
        component: CheckIn
    },

    // ===== 请假审批路由 =====
    {
        path: '/leave-approval',
        name: 'LeaveApproval',
        meta: { title: '请假审批', requiresAuth: true },
        component: LeaveApproval
    },
    {
        path: '/leave-approval/substitute/:id',
        name: 'SubstituteSelection',
        meta: { title: '替班医生选择', requiresAuth: true },
        component: SubstituteSelection
    },
    // =======================================================

    // ===== 加号审批路由 =====
    {
        path: '/slot-approval',
        name: 'SlotApproval',
        meta: { title: '加号审批', requiresAuth: true },
        component: SlotApproval
    },
    // =======================================================

    // ===== 现场服务路由 =====
    {
        path: '/on-site-service',
        name: 'OnSiteService',
        meta: { title: '现场服务', requiresAuth: true },
        component: OnSiteService
    },
    // =======================================================


    // 404 未找到页面路由
    { path: '/404', name: 'NotFound', meta: { title: '404找不到页面' }, component: NotFoundView },

    // 所有其他未匹配的路径重定向到 404 页面 (这条规则必须在最后)
    { path: '/:pathMatch(.*)*', redirect: '/404' }
];

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
});

// 导入adminStore用于权限验证 (假设这是您的状态管理)
import { useAdminStore } from '@/stores/adminStore';

router.beforeEach((to, from, next) => {
    try {
        console.log('🔒 路由守卫触发:', { from: from.path, to: to.path, name: to.name });
        document.title = to.meta.title || '医院后台管理'; // 默认标题

        // 检查是否需要登录验证
        if (to.meta.requiresAuth) {
            try {
                // 确保 useAdminStore() 在这里可以被调用
                const adminStore = useAdminStore();
                console.log('🔒 检查认证状态:', adminStore.isAuthenticated);
                if (adminStore.isAuthenticated) {
                    next();
                } else {
                    // 如果未登录，重定向到登录页（避免循环重定向）
                    if (to.name !== 'AdminLogin') {
                        console.log('🔒 未登录，重定向到登录页');
                        next({ name: 'AdminLogin' });
                    } else {
                        next();
                    }
                }
            } catch (storeError) {
                console.error('🔒 Store访问错误:', storeError);
                // Store 访问失败，允许访问登录页
                if (to.name !== 'AdminLogin') {
                    next({ name: 'AdminLogin' });
                } else {
                    next();
                }
            }
        } else {
            // 不需要认证的页面直接通过
            console.log('🔒 页面不需要认证，直接通过');
            next();
        }
    } catch (error) {
        console.error('❌ 路由守卫错误:', error);
        console.error('错误堆栈:', error.stack);
        // 如果出错，至少让登录页可以访问
        if (to.name !== 'AdminLogin') {
            next({ name: 'AdminLogin' });
        } else {
            next();
        }
    }
});

export default router;

