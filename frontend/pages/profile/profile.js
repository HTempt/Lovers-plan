const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    userInfo: null,
    coupleInfo: null,
    loading: true,
    genderText: '设置性别',
    gridItems: [
      { key: 'achievement', icon: '🏆', name: '情侣成就', desc: '收集奖励' },
      { key: 'statistics', icon: '📊', name: '恋爱报告', desc: '数据分析' },
      { key: 'time-capsule', icon: '💌', name: '时光胶囊', desc: '寄往未来' },
      { key: 'subscribe', icon: '🔔', name: '消息订阅', desc: '推送通知' },
      { key: 'setting', icon: '⚙️', name: '设置', desc: '偏好管理' },
      { key: 'feedback', icon: '💬', name: '意见反馈', desc: '告诉我们' }
    ]
  },

  onShow() {
    this.loadInfo();
  },

  onPullDownRefresh() {
    this.loadInfo().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  async loadInfo() {
    this.setData({ loading: true });
    try {
      const userInfo = await api.getUserInfo();
      this.setData({ userInfo });
      app.setUserInfo(userInfo);
      const genderMap = { 0: '设置性别', 1: '♂ 男', 2: '♀ 女' };
      wx.setNavigationBarTitle({ title: '' });
      this.setData({ genderText: genderMap[userInfo.gender] || '设置性别' });
    } catch (err) {}

    const coupleId = this.data.userInfo?.coupleId;
    if (coupleId) {
      try {
        const coupleInfo = await api.getCoupleInfo();
        this.setData({ coupleInfo });
      } catch (err) {}
    }
    this.setData({ loading: false });
  },

  editNickname() {
    wx.showModal({
      title: '修改昵称',
      editable: true,
      placeholderText: '请输入新昵称',
      success: async (res) => {
        if (res.confirm && res.content) {
          try {
            await api.updateUserInfo({ nickname: res.content });
            wx.showToast({ title: '修改成功', icon: 'success' });
            this.loadInfo();
          } catch (err) {}
        }
      }
    });
  },

  // 更换头像
  changeAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        if (res.tempFilePaths.length === 0) return;
        wx.showLoading({ title: '上传中...' });
        try {
          const fileData = await api.uploadFile(res.tempFilePaths[0], 'image');
          const fileUrl = typeof fileData === 'string' ? fileData : fileData.url;
          await api.updateUserInfo({ avatar: fileUrl });
          wx.hideLoading();
          wx.showToast({ title: '头像已更新', icon: 'success' });
          this.loadInfo();
        } catch (err) {
          wx.hideLoading();
          wx.showToast({ title: '上传失败', icon: 'none' });
        }
      }
    });
  },

  goToPage(e) {
    const page = e.currentTarget.dataset.page;
    const navPages = {
      statistics: '/pages/statistics/statistics',
      achievement: '/pages/achievement/achievement',
      'love-tree': '/pages/love-tree/love-tree',
      'time-capsule': '/pages/time-capsule/time-capsule'
    };
    // 特殊页面处理
    if (page === 'subscribe') {
      wx.navigateTo({ url: '/pages/subscribe/subscribe' });
      return;
    }
    if (page === 'setting') {
      wx.showToast({ title: '功能开发中', icon: 'none' });
      return;
    }
    if (page === 'feedback') {
      wx.showToast({ title: '功能开发中', icon: 'none' });
      return;
    }
    const navUrl = navPages[page];
    if (navUrl) wx.navigateTo({ url: navUrl });
  },

  // 显示伴侣信息
  showPartnerInfo() {
    const coupleInfo = this.data.coupleInfo;
    if (!coupleInfo) {
      wx.showToast({ title: '暂未绑定伴侣', icon: 'none' });
      return;
    }
    wx.showModal({
      title: coupleInfo.partnerNickname || '我的另一半',
      content: coupleInfo.partnerDesc || '暂无介绍',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  // 编辑性别
  editGender() {
    wx.showActionSheet({
      itemList: ['♂ 男', '♀ 女'],
      success: async (res) => {
        const gender = res.tapIndex + 1;
        try {
          await api.updateUserInfo({ gender });
          wx.showToast({ title: '已更新', icon: 'success' });
          this.loadInfo();
        } catch (err) {}
      }
    });
  },

  // 编辑手机号
  editPhone() {
    wx.showModal({
      title: '绑定手机号',
      editable: true,
      placeholderText: '请输入手机号',
      success: async (res) => {
        if (res.confirm && res.content) {
          const phone = res.content.trim();
          if (!/^1\d{10}$/.test(phone)) {
            wx.showToast({ title: '手机号格式不正确', icon: 'none' });
            return;
          }
          try {
            await api.updateUserInfo({ phone });
            wx.showToast({ title: '已绑定', icon: 'success' });
            this.loadInfo();
          } catch (err) {}
        }
      }
    });
  },

  handleUnbind() {
    wx.showModal({
      title: '解除绑定',
      content: '确定要解除情侣绑定吗？解除后历史数据仍会保留。',
      confirmText: '确定解除',
      confirmColor: '#FF4D4F',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.unbind();
            wx.showToast({ title: '已解除绑定', icon: 'success' });
            setTimeout(() => {
              wx.reLaunch({ url: '/pages/bind/bind' });
            }, 1500);
          } catch (err) {}
        }
      }
    });
  },

  handleLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout();
        }
      }
    });
  }
});
