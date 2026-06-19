const api = require('../../utils/api');

Page({
  data: {
    wishList: [],
    showCreateModal: false,
    formTitle: '',
    formCategory: 'life',
    formTargetDate: '',
    loading: true,
    error: false,
    categories: [
      { value: 'travel', label: '旅行', icon: '🌍' },
      { value: 'life', label: '生活', icon: '💖' },
      { value: 'growth', label: '成长', icon: '🚀' }
    ]
  },

  onShow() {
    this.loadWishes();
  },

  onPullDownRefresh() {
    this.loadWishes().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  async loadWishes() {
    this.setData({ loading: true, error: false });
    try {
      const list = await api.getWishList();
      this.setData({ wishList: list || [], loading: false });
    } catch (err) {
      this.setData({ loading: false, error: true });
    }
  },

  showCreateModal() {
    this.setData({
      showCreateModal: true,
      formTitle: '',
      formCategory: 'life',
      formTargetDate: ''
    });
  },

  hideModal() {
    this.setData({ showCreateModal: false });
  },

  onTitleInput(e) { this.setData({ formTitle: e.detail.value }); },
  onCategoryChange(e) { this.setData({ formCategory: e.currentTarget.dataset.value }); },
  onDateInput(e) { this.setData({ formTargetDate: e.detail.value }); },

  async handleCreate() {
    if (!this.data.formTitle) {
      wx.showToast({ title: '请输入愿望标题', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '创建中...' });
    try {
      const data = {
        title: this.data.formTitle,
        category: this.data.formCategory
      };
      if (this.data.formTargetDate) data.targetDate = this.data.formTargetDate;

      await api.createWish(data);
      wx.hideLoading();
      this.hideModal();
      wx.showToast({ title: '愿望已创建', icon: 'success' });
      this.loadWishes();
    } catch (err) { wx.hideLoading(); }
  },

  async handleAchieve(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '梦想成真',
      content: '确定要标记这个愿望为已达成吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.achieveWish(id);
            wx.showToast({ title: '恭喜梦想成真！', icon: 'success' });
            this.loadWishes();
          } catch (err) {}
        }
      }
    });
  },

  async handleProgress(e) {
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
            this.loadWishes();
          } catch (err) {}
        }
      }
    });
  },

  getProgressPercent(current, target) {
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
