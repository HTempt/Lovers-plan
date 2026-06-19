const api = require('../../utils/api');

Page({
  data: {
    loading: true,
    error: false,
    hasCouple: false,
    stats: {}
  },

  onShow() {
    this.loadStatistics();
  },

  async loadStatistics() {
    this.setData({ loading: true, error: false });
    try {
      const stats = await api.getStatistics();
      this.setData({
        stats: stats,
        hasCouple: stats.hasCouple || false,
        loading: false
      });
    } catch (err) {
      this.setData({ loading: false, error: true });
    }
  }
});
