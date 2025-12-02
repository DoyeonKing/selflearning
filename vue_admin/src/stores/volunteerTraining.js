import { defineStore } from 'pinia';
import request from '@/utils/request.js'; // 您的请求工具
import { ElMessage } from 'element-plus'; // 导入 ElMessage 用于提示用户

export const useVolunteerTrainingStore = defineStore('volunteerTraining', {
  state: () => ({
    trainings: [], // 用于存储培训列表
    currentTrainingDetail: null, // 用于存储当前查看的培训详情
    isLoadingList: false,
    isLoadingDetail: false,
    isSubmittingApplication: false, // 提交培训申请的状态
    error: null, // 通用错误状态
    pagination: {
      currentPage: 1,
      pageSize: 10,
      totalItems: 0,
    },
  }),

  getters: {
    // 示例：获取即将开始并通过审核的培训
    // upcomingApprovedTrainings: (state) => state.trainings.filter(trn =>
    //   new Date(trn.startTime) > new Date() && trn.trainingStatus === '审核通过'
    // ),
  },

  actions: {
    /**
     * 组织申请新的志愿培训
     * @param {object} trainingData - 包含培训详细信息的对象，
     * 例如: { orgId: 'ORG123', trainingName: '急救培训', startTime: 'YYYY-MM-DD HH:mm:ss', ... }
     * 其中 orgId 是必需的。
     */
    async applyForTraining(trainingData) {
      if (!trainingData || !trainingData.orgId) {
        this.error = '申请培训失败：组织ID (OrgID) 缺失。';
        console.error('Store (VolunteerTraining): OrgID is required to apply for a training.');
        ElMessage.error(this.error); // 添加前端提示
        return { code: '400', msg: '组织ID (OrgID) 不能为空以提交申请' };
      }

      this.isSubmittingApplication = true;
      this.error = null;
      try {
        console.log('Store (VolunteerTraining): Submitting training application with data:', trainingData);
        // 后端API端点
        const response = await request.post('/volunteerTraining/apply', trainingData);

        if (response.code === '200' || response.code === 200) {
          console.log('Store (VolunteerTraining): Application submitted successfully.', response.data);
          ElMessage.success(response.msg || '培训申请成功提交！'); // 添加前端提示
          // 申请成功后可以进行的操作，例如：
          // 1. 如果当前在培训列表页，可以刷新培训列表
          // if (router.currentRoute.value.name === 'OrganizationTrainingList') { // 假设的路由名称
          //   this.fetchTrainingsByOrg(trainingData.orgId);
          // }
          // 2. 或者，如果后端在成功响应中返回了创建的培训对象，可以考虑将其添加到本地 state.trainings (如果适用)
        } else {
          this.error = response.msg || '提交培训申请失败';
          console.error('Store (VolunteerTraining): Failed to submit application API error:', response.msg);
          ElMessage.error(this.error); // 添加前端提示
        }
        return response; // 返回完整的后端响应给调用方
      } catch (err) {
        console.error('Store (VolunteerTraining): Network or system error submitting application:', err);
        // 包含 AxiosError 的 message，提供更详细的网络错误信息
        this.error = '提交培训申请时发生网络或系统错误。' + (err.message || '');
        ElMessage.error(this.error); // 添加前端提示
        // 尝试从错误对象中提取后端可能返回的错误信息
        if (err.response && err.response.data) {
          return err.response.data; // 返回后端错误信息结构
        }
        // 如果无法提取，则抛出原始错误或返回通用错误结构
        return { code: '500', msg: this.error }; // 或者 throw err;
      } finally {
        this.isSubmittingApplication = false;
      }
    },

    // TODO: 实现获取培训列表、培训详情等 Actions
    // async fetchTrainingsByOrg(orgId, pageNum = 1, pageSize = 10) { ... }
    // async fetchTrainingDetails(trainingId) { ... }
  }
});
