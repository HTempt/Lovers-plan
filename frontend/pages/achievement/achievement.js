const api = require('../../utils/api');

Page({
  data: {
    loading: true,
    overview: {
      total: 0, unlocked: 0, percent: 0,
      level: 1, title: '恋爱新手',
      nextTitle: '热恋新人', needForNext: 6,
      nextTarget: null,
      growthValue: 0, growthTarget: 200
    },
    categories: [
      { key: 'anniversary', name: '❤️纪念', active: true },
      { key: 'diary', name: '📷回忆', active: false },
      { key: 'footprint', name: '🌏探索', active: false },
      { key: 'task', name: '🎯挑战', active: false },
      { key: 'capsule', name: '💌胶囊', active: false },
      { key: 'quiz', name: '💬互动', active: false },
      { key: 'growth', name: '🌱成长', active: false }
    ],
    currentCategory: 'anniversary',
    achievements: [],
    filteredAchievements: [],
    // 解锁弹窗
    showUnlock: false,
    unlockAchievements: []
  },

  onLoad() {
    this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [overview, achievements] = await Promise.all([
        api.getAchievementOverview(),
        api.getAchievementList()
      ]);
      this.setData({
        overview,
        achievements,
        filteredAchievements: achievements.filter(a => a.category === 'anniversary'),
        loading: false
      });
    } catch (err) {
      console.error('加载成就数据失败', err);
      this.setData({ loading: false });
    }
  },

  switchCategory(e) {
    const key = e.currentTarget.dataset.key;
    const categories = this.data.categories.map(c => ({
      ...c,
      active: c.key === key
    }));
    this.setData({
      currentCategory: key,
      categories,
      filteredAchievements: this.data.achievements.filter(a => a.category === key)
    });
  },

  // 成就详情
  showAchievementDetail(e) {
    const idx = e.currentTarget.dataset.index;
    const list = this.data.filteredAchievements;
    const ach = list[idx];
    if (!ach) return;
    if (!ach.unlocked && ach.hidden) return; // 隐藏成就不可见

    const rarityMap = { 1: '普通', 2: '稀有', 3: '史诗', 4: '传说' };
    const rarity = rarityMap[ach.rarity] || '普通';

    let content = `${ach.icon} ${ach.name}\n${ach.description}`;
    if (ach.unlocked) {
      content += `\n\n✅ 已解锁`;
    } else {
      content += `\n\n🔒 未解锁`;
      if (ach.progress !== undefined && ach.target !== undefined) {
        content += `\n进度：${Math.min(ach.progress, ach.target)}/${ach.target}`;
      }
    }
    content += `\n稀有度：${rarity}`;
    content += `\n奖励成长值：+${ach.growthReward}`;

    wx.showModal({
      title: ach.name,
      content: content,
      showCancel: false
    });
  },

  // 获取稀有度样式
  getRarityClass(rarity) {
    const map = {
      1: 'rarity-common',
      2: 'rarity-rare',
      3: 'rarity-epic',
      4: 'rarity-legendary'
    };
    return map[rarity] || 'rarity-common';
  },

  // 获取稀有度标签文字
  getRarityLabel(rarity) {
    const map = { 1: '普通', 2: '稀有', 3: '史诗', 4: '传说' };
    return map[rarity] || '';
  },

  // 空状态 - 去完成任务
  goToTask() {
    wx.switchTab({
      url: '/pages/task/task'
    });
  }
});
