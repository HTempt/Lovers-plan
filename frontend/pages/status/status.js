const api = require('../../utils/api');

Page({
  data: {
    statusTemplates: [],
    moodTags: [],
    selectedStatus: '',
    selectedMood: '',
    currentStatus: null
  },

  onLoad() {
    this.loadTemplates();
    this.loadCurrentStatus();
  },

  async loadTemplates() {
    try {
      const result = await api.getStatusTemplates();
      this.setData({
        statusTemplates: result.statusTemplates || [],
        moodTags: result.moodTags || []
      });
    } catch (err) {
      console.error('加载状态模板失败', err);
    }
  },

  async loadCurrentStatus() {
    try {
      const status = await api.getMyStatus();
      if (status) {
        this.setData({
          currentStatus: status,
          selectedStatus: status.statusName || '',
          selectedMood: status.mood || ''
        });
      }
    } catch (err) {
      console.error('加载当前状态失败', err);
    }
  },

  selectStatus(e) {
    this.setData({ selectedStatus: e.currentTarget.dataset.status });
  },

  selectMood(e) {
    this.setData({ selectedMood: e.currentTarget.dataset.mood });
  },

  async handleSave() {
    if (!this.data.selectedStatus) {
      wx.showToast({ title: '请选择一个状态', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '保存中...' });
    try {
      await api.setStatus({
        statusName: this.data.selectedStatus,
        mood: this.data.selectedMood
      });
      wx.hideLoading();
      wx.showToast({ title: '状态已更新', icon: 'success' });
      wx.navigateBack();
    } catch (err) {
      wx.hideLoading();
    }
  },

  async handleClear() {
    wx.showLoading({ title: '清除中...' });
    try {
      await api.clearStatus();
      wx.hideLoading();
      wx.showToast({ title: '状态已清除', icon: 'success' });
      wx.navigateBack();
    } catch (err) {
      wx.hideLoading();
    }
  }
});
