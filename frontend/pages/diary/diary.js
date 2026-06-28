const api = require('../../utils/api');
const util = require('../../utils/util');

Page({
  data: {
    currentView: 'timeline',
    timeline: [],
    album: {},
    page: 0,
    hasMore: true,
    loading: false
  },

  onShow() {
    this.loadTimeline(true);
  },

  async loadTimeline(reset) {
    if (reset) {
      this.setData({ page: 0, timeline: [], hasMore: true });
    }
    if (!this.data.hasMore || this.data.loading) return;

    this.setData({ loading: true });
    try {
      const result = await api.getTimeline({ page: this.data.page, size: 20 });
      const items = (result.items || []).map(item => ({
        ...item,
        createTime: util.formatDateTime(item.createTime)
      }));
      this.setData({
        timeline: [...this.data.timeline, ...items],
        page: this.data.page + 1,
        hasMore: result.hasMore || false,
        loading: false
      });
    } catch (err) {
      this.setData({ loading: false });
    }
  },

  async loadAlbum() {
    wx.showLoading({ title: '加载中...' });
    try {
      const album = await api.getAlbum();
      this.setData({ album: album || {} });
    } catch (err) {}
    wx.hideLoading();
  },

  switchView(e) {
    const view = e.currentTarget.dataset.view;
    this.setData({ currentView: view });
    if (view === 'album') this.loadAlbum();
  },

  onReachBottom() {
    if (this.data.currentView === 'timeline') {
      this.loadTimeline(false);
    }
  },

  goToCreate() {
    wx.navigateTo({ url: '/pages/diary-create/diary-create' });
  },

  handleDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '删除后进入回收站，30天后自动清除',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.deleteDiary(id);
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadTimeline(true);
          } catch (err) {}
        }
      }
    });
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url;
    wx.previewImage({ urls: [url] });
  }
});
