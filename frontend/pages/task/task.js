const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    taskList: [],
    badges: [],
    myUserId: null,
    showCreateModal: false,
    formTitle: '',
    formTargetCount: '',
    formDeadline: '',
    loading: true,
    error: false
  },

  onShow() {
    this.loadTasks();
    this.loadBadges();
    this.getMyId();
  },

  onPullDownRefresh() {
    this.loadTasks().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  async getMyId() {
    try {
      const info = await api.getUserInfo();
      this.setData({ myUserId: info.id });
    } catch (err) {}
  },

  async loadBadges() {
    try {
      const badges = await api.getBadges();
      // 格式化日期显示
      const formatted = (badges || []).map(b => ({
        ...b,
        earnedDateDisplay: util.formatDateTime(b.earnedDate)
      }));
      this.setData({ badges: formatted });
    } catch (err) {}
  },

  async loadTasks() {
    this.setData({ loading: true, error: false });
    try {
      const list = await api.getTaskList();
      this.setData({ taskList: list || [], loading: false });
    } catch (err) {
      this.setData({ loading: false, error: true });
    }
  },

  showCreateModal() {
    this.setData({
      showCreateModal: true,
      formTitle: '',
      formTargetCount: '',
      formDeadline: ''
    });
  },

  hideModal() {
    this.setData({ showCreateModal: false });
  },

  onTitleInput(e) {
    this.setData({ formTitle: e.detail.value });
  },

  onTargetCountInput(e) {
    this.setData({ formTargetCount: e.detail.value });
  },

  onDeadlineInput(e) {
    this.setData({ formDeadline: e.detail.value });
  },

  async handleCreate() {
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
      this.hideModal();
      wx.showToast({ title: '创建成功', icon: 'success' });
      this.loadTasks();
    } catch (err) {
      wx.hideLoading();
    }
  },

  async handleCheckIn(e) {
    const taskId = e.currentTarget.dataset.id;
    wx.showLoading({ title: '打卡中...' });
    try {
      await api.checkInTask(taskId);
      wx.hideLoading();
      wx.showToast({ title: '打卡成功，等待对方确认', icon: 'success' });
      this.loadTasks();
    } catch (err) {
      wx.hideLoading();
    }
  },

  async handleConfirm(e) {
    const recordId = e.currentTarget.dataset.id;
    wx.showLoading({ title: '确认中...' });
    try {
      await api.confirmTask(recordId);
      wx.hideLoading();
      wx.showToast({ title: '已确认', icon: 'success' });
      this.loadTasks();
    } catch (err) {
      wx.hideLoading();
    }
  },

  getProgressPercent(current, target) {
    if (!target || target === 0) return 0;
    return Math.min(100, Math.round(current / target * 100));
  },

  getStatusText(status) {
    return { 1: '进行中', 2: '已完成', 3: '已过期' }[status] || '未知';
  }
});
