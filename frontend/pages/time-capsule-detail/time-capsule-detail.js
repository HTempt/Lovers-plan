const api = require('../../utils/api');

Page({
  data: {
    capsule: null,
    mediaList: [],
    loading: true,
    capsuleId: null
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ capsuleId: parseInt(options.id) });
      this.loadDetail();
    }
  },

  async loadDetail() {
    this.setData({ loading: true });
    try {
      const detail = await api.getCapsuleDetail(this.data.capsuleId);
      this.setData({
        capsule: detail,
        mediaList: detail.mediaList || [],
        loading: false
      });
    } catch (err) {
      this.setData({ loading: false });
    }
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url;
    wx.previewImage({ urls: [url] });
  },

  playAudio(e) {
    const url = e.currentTarget.dataset.url;
    const innerAudioContext = wx.createInnerAudioContext();
    innerAudioContext.src = url;
    innerAudioContext.play();
  },

  playVideo(e) {
    const url = e.currentTarget.dataset.url;
    wx.previewMedia({
      sources: [{ url: url, type: 'video' }]
    });
  },

  formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getFullYear()}/${(d.getMonth()+1).toString().padStart(2,'0')}/${d.getDate().toString().padStart(2,'0')}`;
  },

  handleDelete() {
    wx.showModal({
      title: '删除胶囊',
      content: '确定要删除这颗时光胶囊吗？删除后不可恢复。',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' });
          try {
            await api.deleteCapsule(this.data.capsuleId);
            wx.hideLoading();
            wx.showToast({ title: '已删除', icon: 'success' });
            wx.navigateBack();
          } catch (err) {
            wx.hideLoading();
          }
        }
      }
    });
  }
});
