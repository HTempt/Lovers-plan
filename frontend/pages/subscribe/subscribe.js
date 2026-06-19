const api = require('../../utils/api');

Page({
  data: {
    templates: [],
    subscribed: {}
  },

  onLoad() {
    this.loadTemplates();
    this.loadSubscribedStatus();
  },

  async loadTemplates() {
    try {
      const templates = await api.getSubscribeTemplates();
      this.setData({ templates });
    } catch (err) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  // 从缓存读取已订阅状态
  loadSubscribedStatus() {
    const subscribed = wx.getStorageSync('subscribe_templates') || {};
    this.setData({ subscribed });
  },

  // 请求订阅
  async requestSubscribe(e) {
    const { templateId, id } = e.currentTarget.dataset;
    if (!templateId || templateId.startsWith('your_')) {
      wx.showToast({ title: '模板ID未配置', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '请求订阅...' });
    try {
      const res = await new Promise((resolve, reject) => {
        wx.requestSubscribeMessage({
          tmplIds: [templateId],
          success: resolve,
          fail: reject
        });
      });

      // 检查订阅结果
      const accepted = res[templateId] === 'accept';
      if (accepted) {
        wx.showToast({ title: '订阅成功 🎉', icon: 'success' });
        // 保存到缓存
        const subscribed = { ...this.data.subscribed, [id]: true };
        this.setData({ subscribed });
        wx.setStorageSync('subscribe_templates', subscribed);
      } else {
        wx.showToast({ title: '已取消订阅', icon: 'none' });
      }
    } catch (err) {
      if (err.errMsg && err.errMsg.includes('cancel')) {
        wx.showToast({ title: '已取消订阅', icon: 'none' });
      } else {
        wx.showToast({ title: '请求失败', icon: 'none' });
      }
    }
    wx.hideLoading();
  }
});
