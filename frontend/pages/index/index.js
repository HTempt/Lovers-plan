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

    // 🕰 时光机（那天回忆）
    memories: [],

    // 双列卡片 — 左侧动态卡
    leftCard: { type: 'milestone', icon: '❤️', title: '恋爱里程碑', subtitle: '加载中...', daysLeft: 0 },

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

        // 双列卡片 — 左侧动态卡
        leftCard: this.computeLeftCard(data),

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

  // 计算双列卡片左侧内容（优先级：时光机 > 足迹 > 愿望 > 里程碑）
  computeLeftCard(data) {
    // 1. 时光机 — 有历史同月同日回忆
    if (data.memories && data.memories.length > 0) {
      const m = data.memories[0];
      return {
        type: 'memory',
        icon: '🕰',
        title: '时光机',
        subtitle: m.offsetLabel || '一年前的今天',
        // 记忆数据
        memory: m
      };
    }
    // 2. 情侣足迹 — 有足迹数据
    const cityCount = data.albumCityCount || 0;
    if (cityCount > 0) {
      const cities = data.albumCities || [];
      return {
        type: 'footprint',
        icon: '🌏',
        title: '情侣足迹',
        subtitle: `已一起到达 ${cityCount} 座城市`,
        cities: cities
      };
    }
    // 3. 愿望进度 — 存在愿望
    const wishes = data.wishList || [];
    if (wishes.length > 0) {
      const activeWish = wishes.find(w => w.status === 1) || wishes[0];
      return {
        type: 'wish',
        icon: '🎯',
        title: '愿望进度',
        subtitle: activeWish.title,
        progress: activeWish.progress || 0,
        wishId: activeWish.id
      };
    }
    // 4. 恋爱里程碑（兜底）
    const milestone = this.computeMilestone(data);
    return {
      type: 'milestone',
      icon: '❤️',
      title: '恋爱里程碑',
      subtitle: milestone.label,
      daysLeft: milestone.daysLeft
    };
  },

  // 计算恋爱里程碑
  computeMilestone(data) {
    const loveDays = data.loveDays || 0;
    const milestones = [100, 200, 300, 365, 500, 730, 1000, 1500, 2000];
    let next = milestones.find(m => m > loveDays);
    if (!next) next = Math.ceil(loveDays / 365) * 365 + 365;
    const daysLeft = next - loveDays;
    const label = data.isMarried
      ? `距离结婚${next}天纪念`
      : `距离${next}天纪念`;
    return { label, daysLeft };
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
    // 2. ❤️ 今日问答（每日必显）
    const quiz = data.quiz;
    {
      let desc = '今日问题待作答';
      if (quiz && quiz.answerCount >= 2) {
        desc = '✅ 双方已作答';
      } else if (quiz && quiz.answerCount === 1) {
        desc = '⏳ 等待TA作答';
      }
      reminders.push({
        type: 'quiz',
        icon: '❤️',
        title: '今日问答',
        desc: desc,
        page: 'quiz',
        id: null
      });
    }

    // 判断时光胶囊是否有特殊状态（可开启 / 即将开启 / 新创建）
    // 判断时光胶囊是否展示
    const cap = data.capsule;
    let showCapsule = false;
    let capsuleReminder = null;
    if (cap) {
      // 条件1：有待我写入的内容
      if (cap.pendingWriteCount > 0) {
        showCapsule = true;
        capsuleReminder = {
          type: 'capsule',
          icon: '💌',
          title: '时光胶囊',
          desc: '有待你写入的内容',
          page: 'capsule',
          id: null
        };
      }
      // 条件2：即将开启（5天内）
      else if (cap.aboutToOpenCount > 0 && cap.nextOpenDays != null && cap.nextOpenDays <= 5) {
        showCapsule = true;
        capsuleReminder = {
          type: 'capsule',
          icon: '💌',
          title: '时光胶囊',
          desc: `还有${cap.nextOpenDays}天开启`,
          page: 'capsule',
          id: null
        };
      }
      // 条件3：有可开启的
      else if (cap.openableCount > 0) {
        showCapsule = true;
        capsuleReminder = {
          type: 'capsule',
          icon: '📬',
          title: '时光胶囊已成熟',
          desc: '点击开启',
          page: 'capsule',
          id: null
        };
      }
    }

    // 3. 第3个位置：有胶囊特殊状态 → 胶囊卡片；否则 → 愿望卡片
    if (showCapsule) {
      reminders.push(capsuleReminder);
    } else {
      // 默认展示愿望卡片
      if (data.wishList && data.wishList.length > 0) {
        const activeWish = data.wishList.find(w => w.status === 1) || data.wishList[0];
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
    return reminders.slice(0, 3);
  },

  // 提醒点击跳转
  onReminderTap(e) {
    const page = e.currentTarget.dataset.page;
    const tabPages = { task: '/pages/task/task', wish: '/pages/wish/wish' };
    const navPages = { anniversary: '/pages/anniversary/anniversary', capsule: '/pages/time-capsule/time-capsule', quiz: '/pages/daily-question/daily-question' };
    const tabUrl = tabPages[page];
    const navUrl = navPages[page];
    if (tabUrl) wx.switchTab({ url: tabUrl });
    else if (navUrl) wx.navigateTo({ url: navUrl });
  },

  // 左侧动态卡点击跳转
  onLeftCardTap() {
    const card = this.data.leftCard;
    if (!card) return;
    switch (card.type) {
      case 'memory':
        this.onMemoryTap({ currentTarget: { dataset: card.memory } });
        break;
      case 'footprint':
        wx.navigateTo({ url: '/pages/footprint/footprint' });
        break;
      case 'wish':
        wx.switchTab({ url: '/pages/wish/wish' });
        break;
      case 'milestone':
        wx.navigateTo({ url: '/pages/anniversary/anniversary' });
        break;
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
    if (!type) return;

    if (type === 'diary') {
      wx.switchTab({ url: '/pages/diary/diary' });
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
