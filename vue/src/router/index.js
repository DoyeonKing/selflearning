import { createRouter, createWebHistory } from 'vue-router';
// 【重要】导入您自己的 doctorStore
import { useDoctorStore } from '@/stores/doctorStore';

// 导入页面组件
import DoctorLogin from '@/views/DoctorLogin.vue';
import DoctorDashboard from '@/views/DoctorDashboard.vue';
import MySchedule from '@/views/MySchedule.vue';
import PatientInfo from '@/views/PatientInfo.vue';
import LeaveRequest from '@/views/LeaveRequest.vue'; // 导入休假页面
import SlotApplication from '@/views/SlotApplication.vue'; // 导入加号申请页面
const NotFoundView = () => import('../views/404.vue');

const routes = [
    // 登录页面
    {
        path: '/login',
        name: 'DoctorLogin',
        component: DoctorLogin,
        meta: { title: '医生登录' }
    },

    // 医生工作台（需要登录）
    {
        path: '/doctor-dashboard',
        name: 'DoctorDashboard',
        component: DoctorDashboard,
        meta: { title: '医生工作台', requiresAuth: true }
    },

    // 我的排班页面
    {
        path: '/my-schedule',
        name: 'MySchedule',
        component: MySchedule,
        meta: { title: '我的排班', requiresAuth: true }
    },

    // 患者管理页面
    {
        path: '/patient-info',
        name: 'PatientInfo',
        component: PatientInfo,
        meta: { title: '患者管理', requiresAuth: true }
    },

    // 休假申请页面
    {
        path: '/leave-request',
        name: 'LeaveRequest',
        component: LeaveRequest,
        meta: { title: '休假申请', requiresAuth: true }
    },

    // 加号申请页面
    {
        path: '/slot-application',
        name: 'SlotApplication',
        component: SlotApplication,
        meta: { title: '申请加号', requiresAuth: true }
    },

    // 根路径
    {
        path: '/',
        redirect: '/login'
    },

    // 404
    {
        path: '/404',
        name: 'NotFound',
        meta: { title: '404找不到页面' },
        component: NotFoundView
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/404'
    }
];

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
});


// 【重要】路由守卫
router.beforeEach((to, from, next) => {
    document.title = to.meta.title || '医生工作台';

    // 【已修改】使用您自己的 store
    const doctorStore = useDoctorStore();

    // 检查是否需要登录
    if (to.meta.requiresAuth) {
        // 使用您 store 中的 isAuthenticated getter
        if (!doctorStore.isAuthenticated) {
            // 未登录，重定向到登录页面
            next('/login');
            return;
        }
    }

    // 如果已登录且访问登录页面，重定向到医生工作台
    if (to.path === '/login' || to.path === '/') {
        if (doctorStore.isAuthenticated) {
            next('/doctor-dashboard');
            return;
        }
    }

    // 确保其他所有情况都能正常跳转
    next();
});

export default router;