const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    capsuleList: [],
    activeTab: -1,    // -1=全部 0=待写入 1=已封存 2=可开启 3=已开启
    openableCount: 0,
    draftCount: 0,
    currentUserId: 0,
    page: 0,
    hasMore: true,
    loading: true,
    error: false,
    showOpenModal: false,
    openingCapsule: null,
    pageSize: 10
  },

  onLoad() {
    const userInfo = app.globalData.userInfo;
    if (userInfo && userInfo.id) {
      this.setData({ currentUserId: userInfo.id });
    }
  },

  onShow() {
    this.loadCapsules();
    this.loadStats();
  },

  onPullDownRefresh() {
    this.setData({ page: 0, capsuleList: [] });
    this.loadCapsules().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMore();
    }
  },

  async loadCapsules() {
    this.setData({ loading: true, error: false });
    try {
      const params = { page: 0, size: this.data.pageSize };
      if (this.data.activeTab !== -1) {
        params.status = this.data.activeTab;
      }
      const result = await api.getCapsuleList(params);
      this.setData({
        capsuleList: result.items || [],
        hasMore: result.hasMore,
        page: 0,
        loading: false
      });
    } catch (err) {
      this.setData({ loading: false, error: true });
    }
  },

  async loadMore() {
    if (this.data.loading) return;
    const nextPage = this.data.page + 1;
    this.setData({ loading: true });
    try {
      const params = { page: nextPage, size: this.data.pageSize };
      if (this.data.activeTab !== -1) {
        params.status = this.data.activeTab;
      }
      const result = await api.getCapsuleList(params);
      this.setData({
        capsuleList: [...this.data.capsuleList, ...(result.items || [])],
        hasMore: result.hasMore,
        page: nextPage,
        loading: false
      });
    } catch (err) {
      this.setData({ loading: false });
    }
  },

  async loadStats() {
    try {
      const stats = await api.getCapsuleStats();
      this.setData({ openableCount: stats.openableCount || 0 });
      // 同时查待写入数量
      this.loadDraftCount();
    } catch (err) {
      // ignore
    }
  },

  async loadDraftCount() {
    try {
      const result = await api.getCapsuleList({ status: 0, page: 0, size: 1 });
      this.setData({ draftCount: result.total || 0 });
    } catch (err) {}
  },

  switchTab(e) {
    const tab = parseInt(e.currentTarget.dataset.tab);
    this.setData({ activeTab: tab, page: 0, capsuleList: [] });
    this.loadCapsules();
  },

  handleCapsuleTap(e) {
    const id = e.currentTarget.dataset.id;
    const status = parseInt(e.currentTarget.dataset.status);

    if (status === 2) {
      this.showOpenModal(id);
    } else if (status === 0) {
      // 草稿：创建者等待伴侣；伴侣去写入
      if (!this.data.currentUserId) {
        // 未获取到用户信息，先跳详情页由详情页判断
        wx.navigateTo({ url: `/pages/time-capsule-detail/time-capsule-detail?id=${id}` });
        return;
      }
      // 从列表数据中找到胶囊
      const capsule = this.data.capsuleList.find(c => c.id === id);
      if (capsule && capsule.userId === this.data.currentUserId) {
        wx.showToast({ title: '⏳ 等待伴侣写入', icon: 'none' });
      } else {
        wx.navigateTo({ url: `/pages/time-capsule-write/time-capsule-write?pairId=${id}` });
      }
    } else {
      wx.navigateTo({
        url: `/pages/time-capsule-detail/time-capsule-detail?id=${id}`
      });
    }
  },

  onCapsuleLongPress(e) {
    // 预留
  },

  async showOpenModal(capsuleId) {
    try {
      const detail = await api.getCapsuleDetail(capsuleId);
      this.setData({
        showOpenModal: true,
        openingCapsule: detail
      });
    } catch (err) {
      wx.showToast({ title: '获取详情失败', icon: 'none' });
    }
  },

  hideOpenModal() {
    this.setData({ showOpenModal: false, openingCapsule: null });
  },

  async confirmOpen() {
    if (!this.data.openingCapsule) return;
    const id = this.data.openingCapsule.id;
    wx.showLoading({ title: '开启中...' });
    try {
      await api.openCapsule(id);
      wx.hideLoading();
      this.hideOpenModal();
      wx.showToast({ title: '💌 胶囊已开启', icon: 'success' });
      this.loadCapsules();
      this.loadStats();
    } catch (err) {
      wx.hideLoading();
    }
  },

  goCreate() {
    wx.navigateTo({
      url: '/pages/time-capsule-create/time-capsule-create'
    });
  },

  getTypeIcon(type) {
    const map = {
      'to_future_ta': '💌',
      'to_future_us': '💑',
      'birthday': '🎂',
      'anniversary': '💍',
      'wish': '✨'
    };
    return map[type] || '💌';
  },

  formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getFullYear()}/${(d.getMonth()+1).toString().padStart(2,'0')}/${d.getDate().toString().padStart(2,'0')}`;
  },

  /** 长按删除 */
  handleLongPress(e) {
    const id = e.currentTarget.dataset.id;
    const status = parseInt(e.currentTarget.dataset.status);
    if (status === 3) {
      wx.showToast({ title: '已开启的胶囊无法删除', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '删除胶囊',
      content: '确定要删除这颗时光胶囊吗？删除后不可恢复。',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' });
          try {
            await api.deleteCapsule(id);
            wx.hideLoading();
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadCapsules();
            this.loadStats();
          } catch (err) {
            wx.hideLoading();
          }
        }
      }
    });
  }
});
