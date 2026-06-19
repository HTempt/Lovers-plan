const api = require('../../utils/api');
const util = require('../../utils/util');
const app = getApp();

Page({
  data: {
    loading: true,
    treeInfo: null,
    growthHistory: [],
    historyPage: 0,
    historyHasMore: true,
    historyLoading: false,
    levels: [],
    rewards: {},
    activeTab: 'info' // info / history / levels
  },

  onLoad() {
    this.loadAllData();
  },

  onShow() {
    // 每次显示刷新信息
    if (!this.data.loading) {
      this.loadTreeInfo();
    }
  },

  async loadAllData() {
    this.setData({ loading: true });
    try {
      const [treeInfo, levels, rewards] = await Promise.all([
        api.getLoveTreeInfo(),
        api.getLoveTreeLevels(),
        api.getLoveTreeRewards()
      ]);
      this.setData({
        treeInfo,
        levels,
        rewards,
        loading: false
      });
      this.loadHistory();
    } catch (err) {
      console.error('加载爱情树数据失败', err);
      this.setData({ loading: false });
    }
  },

  async loadTreeInfo() {
    try {
      const treeInfo = await api.getLoveTreeInfo();
      this.setData({ treeInfo });
    } catch (err) {
      console.error('刷新爱情树信息失败', err);
    }
  },

  async loadHistory() {
    if (!this.data.historyHasMore || this.data.historyLoading) return;
    this.setData({ historyLoading: true });
    try {
      const result = await api.getLoveTreeHistory({ page: this.data.historyPage, size: 20 });
      const items = (result.items || []).map(item => ({
        ...item,
        _timeDisplay: util.timeAgo(item.createTime),
        _icon: this.getActionIcon(item.actionType),
        _growthValue: item.growthValue > 0 ? '+' + item.growthValue : String(item.growthValue)
      }));
      this.setData({
        growthHistory: this.data.historyPage === 0 ? items : [...this.data.growthHistory, ...items],
        historyPage: result.page + 1,
        historyHasMore: result.hasMore || false,
        historyLoading: false
      });
    } catch (err) {
      this.setData({ historyLoading: false });
    }
  },

  getActionIcon(type) {
    const icons = {
      'diary': '📝',
      'task': '🎯',
      'wish': '✨',
      'sign_in': '✅',
      'status': '🟢'
    };
    return icons[type] || '🌱';
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ activeTab: tab });
    if (tab === 'info') {
      this.loadTreeInfo();
    }
  },

  loadMoreHistory() {
    if (this.data.activeTab === 'history') {
      this.loadHistory();
    }
  },

  // 计算当前等级在全部等级中的索引（用于展示等级进度）
  getLevelIndex(level) {
    return (level || 1) - 1;
  }
});
