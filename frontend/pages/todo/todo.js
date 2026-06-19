const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    todoList: [],
    filter: 'all',
    showModal: false,
    formTitle: '',
    formPriority: 'mid',
    formDeadline: '',
    formExecutorId: '',
    formRepeatType: '',
    partnerId: '',
    editingId: null,
    loading: true,
    error: false
  },

  onShow() {
    this.loadTodoList();
    this.loadPartnerInfo();
  },

  onPullDownRefresh() {
    this.loadTodoList().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  async loadTodoList() {
    this.setData({ loading: true, error: false });
    try {
      const list = await api.getTodoList();
      this.setData({ todoList: list || [], loading: false });
    } catch (err) {
      console.error('加载待办失败', err);
      this.setData({ loading: false, error: true });
    }
  },

  async loadPartnerInfo() {
    try {
      const info = await api.getCoupleInfo();
      if (info && info.partnerId) {
        this.setData({ partnerId: info.partnerId });
      }
    } catch (err) {}
  },

  getFilteredList() {
    if (this.data.filter === 'all') return this.data.todoList;
    const status = this.data.filter === 'pending' ? 0 : 1;
    return this.data.todoList.filter(item => item.status === status);
  },

  filterChange(e) {
    this.setData({ filter: e.currentTarget.dataset.filter });
  },

  showAddModal() {
    this.setData({
      showModal: true,
      editingId: null,
      formTitle: '',
      formPriority: 'mid',
      formDeadline: '',
      formExecutorId: '',
      formRepeatType: ''
    });
  },

  hideModal() {
    this.setData({ showModal: false });
  },

  onTitleInput(e) {
    this.setData({ formTitle: e.detail.value });
  },

  onPriorityChange(e) {
    this.setData({ formPriority: e.detail.value });
  },

  onDeadlineInput(e) {
    this.setData({ formDeadline: e.detail.value });
  },

  onRepeatChange(e) {
    this.setData({ formRepeatType: e.detail.value });
  },

  async handleCreate() {
    if (!this.data.formTitle) {
      wx.showToast({ title: '请输入待办标题', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '创建中...' });
    try {
      const data = {
        title: this.data.formTitle,
        priority: this.data.formPriority
      };
      if (this.data.formDeadline) data.deadline = this.data.formDeadline;
      if (this.data.formExecutorId) data.executorId = this.data.formExecutorId;
      if (this.data.formRepeatType) data.repeatType = this.data.formRepeatType;

      await api.createTodo(data);
      wx.hideLoading();
      this.hideModal();
      wx.showToast({ title: '创建成功', icon: 'success' });
      this.loadTodoList();
    } catch (err) {
      wx.hideLoading();
    }
  },

  async toggleComplete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showLoading({ title: '更新中...' });
    try {
      await api.completeTodo(id);
      wx.hideLoading();
      this.loadTodoList();
    } catch (err) {
      wx.hideLoading();
    }
  },

  async handleDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个待办吗？',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' });
          try {
            await api.deleteTodo(id);
            wx.hideLoading();
            this.loadTodoList();
          } catch (err) {
            wx.hideLoading();
          }
        }
      }
    });
  },

  getPriorityText(priority) {
    const map = { high: '高', mid: '中', low: '低' };
    return map[priority] || priority;
  },

  getPriorityClass(priority) {
    return 'priority-' + (priority || 'mid');
  }
});
