const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    loading: false
  },

  onLoad() {
    // 如果已有token，检查绑定状态
    const token = wx.getStorageSync('token');
    if (token) {
      this.checkLoginStatus();
    }
  },

  // 检查登录状态
  async checkLoginStatus() {
    wx.showLoading({ title: '检查登录状态...' });
    try {
      const userInfo = await api.getUserInfo();
      app.setUserInfo(userInfo);
      if (userInfo.coupleId) {
        wx.switchTab({ url: '/pages/index/index' });
      } else {
        wx.reLaunch({ url: '/pages/bind/bind' });
      }
    } catch (err) {
      // token无效，清除
      wx.removeStorageSync('token');
      wx.hideLoading();
    }
  },

  // 微信登录（通过 getUserInfo 按钮触发）
  async handleLogin(e) {
    if (this.data.loading) return;
    this.setData({ loading: true });
    wx.showLoading({ title: '登录中...' });

    try {
      // 获取微信用户信息（含性别）
      let gender = 0;
      let nickname = '新用户';
      let avatar = '';
      if (e && e.detail && e.detail.userInfo) {
        gender = e.detail.userInfo.gender || 0; // 0=未知, 1=男, 2=女
        nickname = e.detail.userInfo.nickName || '新用户';
        avatar = e.detail.userInfo.avatarUrl || '';
      }

      // 获取微信登录code
      const { code } = await wx.login();
      if (!code) {
        throw new Error('获取登录凭证失败');
      }

      // 调后端登录
      const result = await api.login(code);

      // 保存token
      app.setToken(result.token);

      // 更新用户信息（含头像和性别）
      await api.updateUserInfo({ nickname, avatar, gender }).catch(() => {});

      // 获取用户信息
      if (result.isNewUser) {
        const defaultAvatar = gender === 1 ? '/images/icon/user-boy.png' : '/images/icon/user-girl.png';
        app.setUserInfo({
          nickname,
          avatar: avatar || defaultAvatar,
          gender
        });
      }

      wx.hideLoading();

      // 判断跳转
      if (result.hasCouple) {
        wx.switchTab({ url: '/pages/index/index' });
      } else {
        wx.reLaunch({ url: '/pages/bind/bind' });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: err.message || '登录失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  }
});
