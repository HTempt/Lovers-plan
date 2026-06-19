const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    userInfo: null,
    coupleInfo: null,
    loading: true,
    genderText: '设置性别'
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
      // 计算性别显示文字
      const genderMap = { 0: '设置性别', 1: '♂ 男', 2: '♀ 女' };
      wx.setNavigationBarTitle({ title: '' });
      this.setData({ genderText: genderMap[userInfo.gender] || '设置性别' });
    } catch (err) {}

    try {
      const coupleInfo = await api.getCoupleInfo();
      this.setData({ coupleInfo });
    } catch (err) {}
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
          const fileData = await api.uploadFile(res.tempFilePaths[0], 'avatar');
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
    const tabPages = {
      diary: '/pages/diary/diary',
      task: '/pages/task/task',
      wish: '/pages/wish/wish'
    };
    const navPages = {
      anniversary: '/pages/anniversary/anniversary',
      todo: '/pages/todo/todo',
      statistics: '/pages/statistics/statistics'
    };
    const tabUrl = tabPages[page];
    if (tabUrl) wx.switchTab({ url: tabUrl });
    else {
      const navUrl = navPages[page];
      if (navUrl) wx.navigateTo({ url: navUrl });
    }
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

  // 消息订阅
  handleSubscribe() {
    wx.navigateTo({ url: '/pages/subscribe/subscribe' });
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
