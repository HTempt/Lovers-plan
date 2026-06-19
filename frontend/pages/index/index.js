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
    myAvatar: '/images/icon/user-girl.png',
    partnerAvatar: '/images/icon/user-girl.png',
    myStatus: '',
    myMood: '',
    myStatusDuration: '',
    partnerStatus: '',
    partnerMood: '',
    partnerStatusDuration: '',

    // 岛屿动态
    activityFeed: [],
    displayFeed: [],
    feedPage: 0,
    feedHasMore: true,
    feedLoading: false,

    // 今日提醒
    reminders: [],

    // 爱情树
    loveTree: null,

    // 签到
    signedInToday: false,
    signInStreak: 0,

    // 每日一言
    dailySentence: { text: '', page: '' },

    // 那天的我们（回忆重现）
    memories: [],

    // 时光胶囊
    capsule: null,

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

      // 爱情树信息
      const loveTree = data.loveTree || null;

      // 签到状态
      const signIn = data.signIn || {};

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
        myAvatar: data.myUserInfo && data.myUserInfo.avatar ? data.myUserInfo.avatar : (myGender === 1 ? '/images/icon/user-boy.png' : '/images/icon/user-girl.png'),
        partnerAvatar: data.partner && data.partner.avatar ? data.partner.avatar : defaultPartnerAvatar,
        myStatus: myStatusStr,
        myMood: data.myStatus ? data.myStatus.mood : '',
        myStatusDuration: myStatusDur,
        partnerStatus: partnerStatusStr,
        partnerMood: data.partnerStatus ? data.partnerStatus.mood : '',
        partnerStatusDuration: partnerStatusDur,
        loveTree: loveTree,
        signedInToday: signIn.signedInToday || false,
        signInStreak: signIn.streakDays || 0,
        dailySentence: data.dailySentence || '',
        reminders: this.buildReminders(data),
        // 每日一言（后端返回 { text, page }）
        dailySentence: typeof data.dailySentence === 'object' && data.dailySentence
          ? { text: data.dailySentence.text || '', page: data.dailySentence.page || '' }
          : { text: data.dailySentence || '', page: '' },

        activityFeed: feedItems,
        feedPage: feed.page || 0,
        feedHasMore: feed.hasMore || false,

        // 首页只展示前3条动态
        displayFeed: (feedItems || []).slice(0, 3),

        memories: data.memories || [],

        // 时光胶囊
        capsule: data.capsule || null,

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

  // 构建提醒列表
  buildReminders(data) {
    const reminders = [];
    // 1. 最近纪念日
    if (data.upcomingAnniversary) {
      reminders.push({
        type: 'anniversary',
        icon: data.upcomingAnniversary.icon || '❤️',
        title: data.upcomingAnniversary.title,
        desc: `还有 ${data.upcomingAnniversary.daysLeft} 天`,
        daysLeft: data.upcomingAnniversary.daysLeft,
        page: 'anniversary',
        id: data.upcomingAnniversary.id
      });
    }
    // 2. 进行中任务
    if (data.taskList && data.taskList.length > 0) {
      data.taskList.slice(0, 1).forEach(t => {
        if (reminders.length >= 3) return;
        const target = t.targetCount || 1;
        const current = t.currentCount || 0;
        const progress = Math.min(100, Math.round(current / target * 100));
        reminders.push({
          type: 'task',
          icon: '🎯',
          title: t.title,
          desc: `还差 ${target - current} 次完成`,
          progressValue: progress,
          page: 'task',
          id: t.id
        });
      });
    }
    // 3. 进行中愿望
    if (data.wishList && data.wishList.length > 0) {
      const activeWish = data.wishList.find(w => w.status === 1) || data.wishList[0];
      if (reminders.length < 3 && activeWish) {
        reminders.push({
          type: 'wish',
          icon: '✨',
          title: activeWish.title,
          desc: activeWish.progress ? `${activeWish.progress}%` : '进行中',
          progressValue: activeWish.progress || 0,
          page: 'wish',
          id: activeWish.id
        });
      }
    }
    // 4. 可开启胶囊
    if (data.capsule && data.capsule.openableCount > 0 && reminders.length < 3) {
      reminders.push({
        type: 'capsule',
        icon: '💌',
        title: '时光胶囊',
        desc: `${data.capsule.openableCount} 封胶囊可开启`,
        page: 'capsule',
        id: null
      });
    }
    // 最多取3条提醒
    return reminders.slice(0, 3);
  },

  // 提醒点击跳转
  onReminderTap(e) {
    const page = e.currentTarget.dataset.page;
    const tabPages = { task: '/pages/task/task', wish: '/pages/wish/wish' };
    const navPages = { anniversary: '/pages/anniversary/anniversary', capsule: '/pages/time-capsule/time-capsule' };
    const tabUrl = tabPages[page];
    const navUrl = navPages[page];
    if (tabUrl) wx.switchTab({ url: tabUrl });
    else if (navUrl) wx.navigateTo({ url: navUrl });
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
      case 'capsule':
        wx.navigateTo({ url: '/pages/time-capsule/time-capsule' });
        break;
      default:
        break;
    }
  },

  // 头像加载失败 → 使用默认图片
  onMyAvatarError() {
    this.setData({ myAvatar: '/images/icon/user-girl.png' });
  },

  onPartnerAvatarError() {
    this.setData({ partnerAvatar: '/images/icon/user-girl.png' });
  },

  // 去爱情树页面
  goToLoveTree() {
    wx.navigateTo({ url: '/pages/love-tree/love-tree' });
  },

  // 去时光胶囊
  goToCapsule() {
    wx.navigateTo({ url: '/pages/time-capsule/time-capsule' });
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

  // 每日一言点击跳转
  onSentenceTap(e) {
    const page = e.currentTarget.dataset.page;
    if (!page) return;
    const navMap = {
      diary: '/pages/diary/diary',
      task: '/pages/task/task',
      wish: '/pages/wish/wish',
      'love-tree': '/pages/love-tree/love-tree'
    };
    const url = navMap[page];
    if (url) {
      if (page === 'love-tree') {
        wx.navigateTo({ url });
      } else {
        wx.switchTab({ url });
      }
    }
  },

  // 点击回忆卡片：跳转对应详情页
  onMemoryTap(e) {
    const type = e.currentTarget.dataset.type;
    const refId = e.currentTarget.dataset.refid;
    if (!type || !refId) return;

    if (type === 'diary') {
      wx.navigateTo({ url: `/pages/diary-detail/diary-detail?id=${refId}` });
    } else if (type === 'anniversary') {
      wx.switchTab({ url: '/pages/anniversary/anniversary' });
    } else if (type === 'wish') {
      wx.switchTab({ url: '/pages/wish/wish' });
    } else if (type === 'task') {
      wx.switchTab({ url: '/pages/task/task' });
    }
  },

  // 查看更多动态
  goToFeedPage() {
    wx.switchTab({ url: '/pages/diary/diary' });
  },

  onShareAppMessage() {
    return {
      title: '双人岛 - 记录我们的故事',
      path: '/pages/index/index'
    };
  }
});
