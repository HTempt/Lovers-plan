const api = require('../../utils/api');
const legal = require('../../utils/legal');
const app = getApp();

Page({
  data: {
    loading: false,
    // 隐私协议勾选状态：默认为 false，不读取本地记录、不做任何默认勾选，
    // 必须由用户阅读协议后主动点击选择
    agreed: false,
    shakeAgreement: false
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

  // 用户主动勾选 / 取消勾选协议
  toggleAgreement() {
    this.setData({ agreed: !this.data.agreed });
  },

  // 查看《用户服务协议》或《隐私政策》全文
  openAgreement(e) {
    const type = e.currentTarget.dataset.type === 'privacy' ? 'privacy' : 'user';
    wx.navigateTo({ url: `/pages/agreement/agreement?type=${type}` });
  },

  // 暂不登录：返回上一页；若登录页是入口页则回到可自由浏览的首页
  handleSkip() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.reLaunch({ url: '/pages/index/index' });
    }
  },

  // 点击登录按钮
  async handleStart() {
    if (this.data.loading) return;

    // 用户未主动同意前，不申请微信授权、不获取任何个人信息、不发起登录请求
    if (!this.data.agreed) {
      this.setData({ shakeAgreement: true });
      setTimeout(() => this.setData({ shakeAgreement: false }), 500);
      wx.showToast({ title: '请先阅读并勾选同意协议', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    try {
      // 用户已同意协议后，才申请昵称/头像授权
      // （授权弹窗必须在用户点击的调用栈内同步发起，因此先于 showLoading）
      const profile = await this.getWechatProfile();

      wx.showLoading({ title: '登录中...' });

      // 获取微信登录code
      const { code } = await wx.login();
      if (!code) {
        throw new Error('获取登录凭证失败');
      }

      // 记录本次主动同意的协议版本与时间，便于合规审计
      legal.saveConsent();

      // 调后端登录
      const result = await api.login(code);

      // 保存token
      app.setToken(result.token);

      // 获取用户信息
      if (result.isNewUser) {
        // 首次登录：同步微信头像到服务端
        await api.updateUserInfo(profile).catch(() => {});
        const defaultAvatar = profile.gender === 1 ? '/images/icon/user-boy.png' : '/images/icon/user-girl.png';
        app.setUserInfo({
          nickname: profile.nickname,
          avatar: profile.avatar || defaultAvatar,
          gender: profile.gender
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
  },

  // 申请微信昵称/头像（用户拒绝授权或接口不可用时使用默认资料，不阻断登录）
  getWechatProfile() {
    const fallback = { nickname: '新用户', avatar: '', gender: 0 };
    return new Promise((resolve) => {
      if (typeof wx.getUserProfile !== 'function') {
        resolve(fallback);
        return;
      }
      wx.getUserProfile({
        desc: '用于完善您的昵称与头像',
        success: (res) => {
          const info = (res && res.userInfo) || {};
          resolve({
            nickname: info.nickName || fallback.nickname,
            avatar: info.avatarUrl || '',
            gender: info.gender || 0
          });
        },
        fail: () => resolve(fallback)
      });
    });
  }
});