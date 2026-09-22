App({
  globalData: {
    token: '',
    userInfo: null,
    hasCouple: false
  },

  onLaunch() {
    // 获取存储的token
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
    }
  },

  /**
   * 是否已登录
   * 未登录即为「游客模式」：可以正常浏览首页与各功能入口，不会强制授权登录
   */
  isLoggedIn() {
    return !!this.globalData.token;
  },

  /**
   * 统一的登录引导
   *
   * 只有在用户主动点击需要账号的功能时才调用，弹出可取消的询问框，
   * 由用户自行选择「去登录」或「再逛逛」，不做任何强制跳转。
   *
   * @returns {boolean} 已登录返回 true；未登录返回 false（并弹出引导）
   */
  requireLogin(options) {
    if (this.isLoggedIn()) return true;

    const opts = options || {};

    // 已经停留在登录页时不再重复弹窗引导
    const pages = getCurrentPages();
    const currentRoute = pages.length ? pages[pages.length - 1].route : '';
    if (currentRoute === 'pages/login/login') return false;

    // 避免并发请求同时弹出多个询问框
    if (this._loginPrompting) return false;
    this._loginPrompting = true;

    wx.showModal({
      title: opts.title || '登录后即可使用',
      content: opts.content || '登录后就能记录和同步你们的故事啦，是否现在登录？',
      confirmText: '去登录',
      cancelText: '再逛逛',
      success: (res) => {
        this._loginPrompting = false;
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/login/login' });
        }
      },
      fail: () => {
        this._loginPrompting = false;
      }
    });
    return false;
  },

  setToken(token) {
    this.globalData.token = token;
    wx.setStorageSync('token', token);
  },

  setUserInfo(userInfo) {
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('userInfo', userInfo);
  },

  logout() {
    this.globalData.token = '';
    this.globalData.userInfo = null;
    this.globalData.hasCouple = false;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    // 退出登录后回到可自由浏览的首页（游客模式），而不是强制停留在登录页
    wx.reLaunch({ url: '/pages/index/index' });
  }
});