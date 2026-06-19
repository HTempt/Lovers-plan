const api = require('../../utils/api');
const util = require('../../utils/util');
const app = getApp();

Page({
  data: {
    mode: '',        // '' | 'create' | 'join'
    inviteCode: '',
    inputCode: '',
    generating: false,
    binding: false
  },

  onLoad() {
    // 检查是否已绑定
    this.checkBindStatus();
  },

  async checkBindStatus() {
    try {
      const info = await api.getCoupleInfo();
      if (info && info.coupleId) {
        wx.switchTab({ url: '/pages/index/index' });
      }
    } catch (e) {
      // 未绑定，停留在本页
    }
  },

  // 选择模式
  chooseMode(e) {
    const mode = e.currentTarget.dataset.mode;
    this.setData({ mode });
  },

  // 返回
  goBack() {
    this.setData({
      mode: '',
      inviteCode: '',
      inputCode: ''
    });
  },

  // 生成邀请码
  async generateCode() {
    this.setData({ generating: true });
    try {
      const result = await api.createInvite();
      this.setData({ inviteCode: result.code });
    } catch (err) {
      console.error('生成邀请码失败', err);
    } finally {
      this.setData({ generating: false });
    }
  },

  // 复制邀请码
  copyCode() {
    util.copyText(this.data.inviteCode);
  },

  // 分享邀请码
  shareCode() {
    // 调用微信分享给好友
    wx.shareMessage({
      title: '加入我的「双人岛」',
      desc: `这是我的邀请码: ${this.data.inviteCode}，快来和我绑定吧！`
    });
  },

  // 输入邀请码
  onCodeInput(e) {
    this.setData({ inputCode: e.detail.value });
  },

  // 确认绑定
  async handleBind() {
    const code = this.data.inputCode;
    if (code.length < 6) {
      wx.showToast({ title: '请输入完整的6位邀请码', icon: 'none' });
      return;
    }

    this.setData({ binding: true });
    wx.showLoading({ title: '绑定中...' });

    try {
      await api.acceptInvite(code);
      wx.hideLoading();
      wx.showToast({ title: '绑定成功！', icon: 'success', duration: 1500 });

      // 延迟跳转到首页
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' });
      }, 1500);
    } catch (err) {
      wx.hideLoading();
      // error toast is already shown in api.js
    } finally {
      this.setData({ binding: false });
    }
  },

  onShareAppMessage() {
    if (this.data.inviteCode) {
      return {
        title: '加入我的「双人岛」',
        desc: `这是我的邀请码: ${this.data.inviteCode}`,
        path: '/pages/bind/bind'
      };
    }
    return {
      title: '双人岛 - 记录我们的故事',
      path: '/pages/bind/bind'
    };
  }
});
