const api = require('../../utils/api');
const util = require('../../utils/util');
const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    currentDate: '',
    lunarDate: '',
    loading: true,
    error: false,
    hasCouple: false,

    // 恋爱天数
    loveDays: 0,
    loveDate: '',
    isMarried: false,
    marriedDays: 0,
    marriedDate: '',

    // 伴侣信息 + 状态（保留双人卡片）
    partner: null,
    partnerName: '',
    myAvatar: '',
    partnerAvatar: '',
    myStatus: '',
    myMood: '',
    myStatusDuration: '',
    partnerStatus: '',
    partnerMood: '',
    partnerStatusDuration: '',

    // 岛屿动态
    activityFeed: [],
    feedPage: 0,
    feedHasMore: true,
    feedLoading: false,

    // 当前活跃tab
    activeTab: 'index'
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync();
    this.setData({
      statusBarHeight: sysInfo.statusBarHeight,
      currentDate: util.formatDate(new Date())
    });
    this.loadLunarDate();
  },

  async loadLunarDate() {
    try {
      const lunar = await api.getLunarDate();
      this.setData({ lunarDate: lunar });
    } catch (err) {
      console.error('获取农历日期失败', err);
    }
  },

  onShow() {
    this.loadHomeData();
  },

  async loadHomeData() {
    this.setData({ loading: true, error: false });
    try {
      const data = await api.getHomeData();

      if (!data.hasCouple) {
        this.setData({ hasCouple: false, loading: false });
        return;
      }

      // 计算状态持续时间
      const myStatusStr = data.myStatus ? data.myStatus.statusName : '';
      const myStatusDur = data.myStatus ? this.formatDuration(data.myStatus.startTime, data.myStatus.durationMinutes) : '';
      const partnerStatusStr = data.partnerStatus ? data.partnerStatus.statusName : '';
      const partnerStatusDur = data.partnerStatus ? this.formatDuration(data.partnerStatus.startTime, data.partnerStatus.durationMinutes) : '';

      // 计算默认头像（按性别）
      const myGender = data.myUserInfo ? (data.myUserInfo.gender || 0) : 0;
      const defaultMyAvatar = myGender === 1 ? '/images/icon/user-boy.png' : '/images/icon/user-girl.png';
      const partnerGender = data.partner ? (data.partner.gender || 0) : 0;
      const finalPartnerGender = partnerGender !== 0 ? partnerGender : (myGender === 1 ? 2 : 1);
      const defaultPartnerAvatar = finalPartnerGender === 1 ? '/images/icon/user-boy.png' : '/images/icon/user-girl.png';

      // 初始化动态数据
      const feed = data.activityFeed || { items: [], page: 0, hasMore: false };
      const feedItems = (feed.items || []).map(item => ({
        ...item,
        _timeDisplay: util.timeAgo(item.createTime)
      }));

      this.setData({
        hasCouple: true,
        loveDays: data.loveDays || 0,
        loveDate: data.loveDate || '',
        loveDateLunar: data.loveDateLunar || '',
        isMarried: data.isMarried || false,
        marriedDays: data.marriedDays || 0,
        marriedDate: data.marriedDate || '',
        partner: data.partner,
        partnerName: data.partner ? data.partner.nickname || '另一半' : '对方',
        myAvatar: data.myUserInfo ? (data.myUserInfo.avatar || defaultMyAvatar) : defaultMyAvatar,
        partnerAvatar: data.partner ? (data.partner.avatar || defaultPartnerAvatar) : defaultPartnerAvatar,
        myStatus: myStatusStr,
        myMood: data.myStatus ? data.myStatus.mood : '',
        myStatusDuration: myStatusDur,
        partnerStatus: partnerStatusStr,
        partnerMood: data.partnerStatus ? data.partnerStatus.mood : '',
        partnerStatusDuration: partnerStatusDur,
        activityFeed: feedItems,
        feedPage: feed.page || 0,
        feedHasMore: feed.hasMore || false,
        loading: false
      });
    } catch (err) {
      console.error('加载首页数据失败', err);
      this.setData({ error: true, loading: false });
    }
  },

  // 格式化状态持续时间（上限24小时）
  formatDuration(startTime, durationMinutes) {
    if (!startTime) return '';
    let diff;
    if (durationMinutes && durationMinutes > 0) {
      diff = durationMinutes;
    } else {
      const start = new Date(startTime).getTime();
      const now = Date.now();
      diff = Math.floor((now - start) / 60000);
    }
    if (diff >= 1440) return '';
    if (diff < 1) return '刚刚开始';
    if (diff < 60) return `持续${diff}分钟`;
    const hours = Math.floor(diff / 60);
    const mins = diff % 60;
    if (mins === 0) return `持续${hours}小时`;
    return `持续${hours}小时${mins}分钟`;
  },

  // 下拉刷新
  async onRefresh() {
    this.setData({ feedPage: 0, feedHasMore: true });
    await this.loadHomeData();
    wx.stopPullDownRefresh();
  },

  // 加载更多动态
  async loadMoreFeed() {
    if (!this.data.feedHasMore || this.data.feedLoading) return;
    this.setData({ feedLoading: true });
    try {
      const nextPage = this.data.feedPage + 1;
      const result = await api.getActivityFeed({ page: nextPage, size: 10 });
      const items = (result.items || []).map(item => ({
        ...item,
        _timeDisplay: util.timeAgo(item.createTime)
      }));
      this.setData({
        activityFeed: [...this.data.activityFeed, ...items],
        feedPage: nextPage,
        feedHasMore: result.hasMore || false,
        feedLoading: false
      });
    } catch (err) {
      this.setData({ feedLoading: false });
    }
  },

  // 动态点击跳转
  onFeedItemTap(e) {
    const item = e.currentTarget.dataset.item;
    if (!item) return;
    switch (item.type) {
      case 'diary':
        wx.switchTab({ url: '/pages/diary/diary' });
        break;
      case 'status':
        wx.navigateTo({ url: '/pages/status/status' });
        break;
      case 'task':
        wx.switchTab({ url: '/pages/task/task' });
        break;
      case 'wish':
        wx.switchTab({ url: '/pages/wish/wish' });
        break;
      case 'anniversary':
        wx.navigateTo({ url: '/pages/anniversary/anniversary' });
        break;
      default:
        break;
    }
  },

  // 设置我的状态
  setMyStatus() {
    wx.navigateTo({ url: '/pages/status/status' });
  },

  // 去绑定页
  goToBind() {
    wx.navigateTo({ url: '/pages/bind/bind' });
  },

  // 去个人页
  goToProfile() {
    wx.navigateTo({ url: '/pages/profile/profile' });
  },

  onShareAppMessage() {
    return {
      title: '双人岛 - 记录我们的故事',
      path: '/pages/index/index'
    };
  }
});
