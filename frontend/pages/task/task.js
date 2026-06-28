const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    // 任务
    taskList: [],
    myUserId: null,
    showCreateTaskModal: false,
    formTitle: '',
    formTargetCount: '',
    formDeadline: '',

    // 愿望（从愿望页合并）
    wishList: [],
    showCreateWishModal: false,
    wishFormTitle: '',
    wishFormCategory: 'life',
    wishFormTargetDate: '',
    wishCategories: [
      { value: 'travel', label: '旅行', icon: '🌍' },
      { value: 'life', label: '生活', icon: '💖' },
      { value: 'growth', label: '成长', icon: '🚀' }
    ],

    // 当前tab
    currentTab: 'task',

    loading: true,
    error: false
  },

  onShow() {
    this.loadData();
  },

  onPullDownRefresh() {
    this.loadData().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  async loadData() {
    this.setData({ loading: true, error: false });
    try {
      const [taskList, wishList, userInfo] = await Promise.all([
        api.getTaskList(),
        api.getWishList(),
        api.getUserInfo()
      ]);
      this.setData({
        taskList: taskList || [],
        wishList: wishList || [],
        myUserId: userInfo.id,
        loading: false
      });
    } catch (err) {
      this.setData({ loading: false, error: true });
    }
  },

  // ── Tab切换 ──
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
  },

  // ── 任务相关 ──

  showCreateTaskModal() {
    this.setData({
      showCreateTaskModal: true,
      formTitle: '',
      formTargetCount: '',
      formDeadline: ''
    });
  },

  hideTaskModal() {
    this.setData({ showCreateTaskModal: false });
  },

  onTaskTitleInput(e) { this.setData({ formTitle: e.detail.value }); },
  onTargetCountInput(e) { this.setData({ formTargetCount: e.detail.value }); },
  onDeadlineInput(e) { this.setData({ formDeadline: e.detail.value }); },

  async handleCreateTask() {
    if (!this.data.formTitle) {
      wx.showToast({ title: '请输入任务标题', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '创建中...' });
    try {
      const data = { title: this.data.formTitle };
      if (this.data.formTargetCount) data.targetCount = parseInt(this.data.formTargetCount);
      if (this.data.formDeadline) data.deadline = this.data.formDeadline;
      await api.createTask(data);
      wx.hideLoading();
      this.hideTaskModal();
      wx.showToast({ title: '创建成功', icon: 'success' });
      this.loadData();
    } catch (err) { wx.hideLoading(); }
  },

  async handleCheckIn(e) {
    const taskId = e.currentTarget.dataset.id;
    wx.showLoading({ title: '打卡中...' });
    try {
      await api.checkInTask(taskId);
      wx.hideLoading();
      wx.showToast({ title: '打卡成功，等待对方确认', icon: 'success' });
      this.loadData();
    } catch (err) { wx.hideLoading(); }
  },

  async handleConfirm(e) {
    const recordId = e.currentTarget.dataset.id;
    wx.showLoading({ title: '确认中...' });
    try {
      await api.confirmTask(recordId);
      wx.hideLoading();
      wx.showToast({ title: '已确认', icon: 'success' });
      this.loadData();
    } catch (err) { wx.hideLoading(); }
  },

  getProgressPercent(current, target) {
    if (!target || target === 0) return 0;
    return Math.min(100, Math.round(current / target * 100));
  },

  getStatusText(status) {
    return { 1: '进行中', 2: '已完成', 3: '已过期' }[status] || '未知';
  },

  // ── 愿望相关 ──

  showCreateWishModal() {
    this.setData({
      showCreateWishModal: true,
      wishFormTitle: '',
      wishFormCategory: 'life',
      wishFormTargetDate: ''
    });
  },

  hideWishModal() {
    this.setData({ showCreateWishModal: false });
  },

  onWishTitleInput(e) { this.setData({ wishFormTitle: e.detail.value }); },
  onWishCategoryChange(e) { this.setData({ wishFormCategory: e.currentTarget.dataset.value }); },
  onWishDateInput(e) { this.setData({ wishFormTargetDate: e.detail.value }); },

  async handleCreateWish() {
    if (!this.data.wishFormTitle) {
      wx.showToast({ title: '请输入愿望标题', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '创建中...' });
    try {
      const data = { title: this.data.wishFormTitle, category: this.data.wishFormCategory };
      if (this.data.wishFormTargetDate) data.targetDate = this.data.wishFormTargetDate;
      await api.createWish(data);
      wx.hideLoading();
      this.hideWishModal();
      wx.showToast({ title: '愿望已创建', icon: 'success' });
      this.loadData();
    } catch (err) { wx.hideLoading(); }
  },

  async handleAchieveWish(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '梦想成真',
      content: '确定要标记这个愿望为已达成吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.achieveWish(id);
            wx.showToast({ title: '恭喜梦想成真！', icon: 'success' });
            this.loadData();
          } catch (err) {}
        }
      }
    });
  },

  async handleWishProgress(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '更新进度',
      editable: true,
      placeholderText: '输入当前金额',
      success: async (res) => {
        if (res.confirm && res.content) {
          try {
            await api.updateWishProgress({ id, currentAmount: parseFloat(res.content) });
            wx.showToast({ title: '进度已更新', icon: 'success' });
            this.loadData();
          } catch (err) {}
        }
      }
    });
  },

  getWishProgressPercent(current, target) {
    const c = parseFloat(current) || 0;
    const t = parseFloat(target) || 0;
    if (t === 0) return 0;
    return Math.min(100, Math.round(c / t * 100));
  },

  getCategoryInfo(category) {
    const map = { travel: { icon: '🌍', label: '旅行' }, life: { icon: '💖', label: '生活' }, growth: { icon: '🚀', label: '成长' } };
    return map[category] || { icon: '📌', label: category };
  },

  formatAmount(amount) {
    if (!amount) return '0';
    return parseFloat(amount).toLocaleString();
  }
});
