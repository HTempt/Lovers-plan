const api = require('../../utils/api');

Page({
  data: {
    anniversaryList: [],
    showModal: false,
    isEditing: false,
    editId: null,
    formTitle: '',
    formDate: '',
    formRemindDays: 0,
    formIcon: '❤️',
    emojiList: ['❤️','💕','💍','🎂','🎉','🎊','🌟','🎁','🥂','🌹','💑','🏠','✈️','🏖️','⛰️','📸','🎬','🎵','🍜','☕'],
    namePresets: ['恋爱纪念日','结婚纪念日','第一次见面','第一次旅行','生日','100天纪念','一周年纪念','自定义'],
    showCustomName: false,
    loading: true,
    error: false
  },

  onShow() {
    this.loadList();
  },

  onPullDownRefresh() {
    this.loadList().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  async loadList() {
    this.setData({ loading: true, error: false });
    try {
      const list = await api.getAnniversaryList();
      this.setData({ anniversaryList: list || [], loading: false });
    } catch (err) {
      this.setData({ loading: false, error: true });
    }
  },

  // 获取纪念日图标
  getAnniversaryIcon(title) {
    if (!title) return '❤️';
    if (title.includes('恋爱') || title.includes('在一起')) return '💕';
    if (title.includes('生日')) return '🎂';
    if (title.includes('结婚') || title.includes('婚礼')) return '💍';
    if (title.includes('纪念')) return '🎉';
    return '❤️';
  },

  // 显示创建弹窗
  showCreateModal() {
    this.setData({
      showModal: true,
      isEditing: false,
      editId: null,
      formTitle: '',
      formDate: '',
      formRemindDays: 0,
      formIcon: '❤️',
      showCustomName: false,
      nameIndex: -1
    });
  },

  // 显示编辑弹窗
  showEditModal(e) {
    const title = e.currentTarget.dataset.title;
    const isCustom = !this.data.namePresets.includes(title);
    const presetIndex = isCustom ? -1 : this.data.namePresets.indexOf(title);
    this.setData({
      showModal: true,
      isEditing: true,
      editId: parseInt(e.currentTarget.dataset.id),
      formTitle: title,
      formDate: e.currentTarget.dataset.date,
      formRemindDays: parseInt(e.currentTarget.dataset.remind || 0),
      formIcon: e.currentTarget.dataset.icon || '❤️',
      showCustomName: isCustom,
      nameIndex: presetIndex
    });
  },

  hideModal() {
    this.setData({ showModal: false });
  },

  onTitleInput(e) { this.setData({ formTitle: e.detail.value }); },
  onDateInput(e) { this.setData({ formDate: e.detail.value }); },
  onRemindChange(e) { this.setData({ formRemindDays: parseInt(e.currentTarget.dataset.value) }); },
  onIconChange(e) { this.setData({ formIcon: e.currentTarget.dataset.icon }); },
  onNameChange(e) {
    const index = e.detail.value;
    const name = this.data.namePresets[index];
    if (name === '自定义') {
      this.setData({ nameIndex: index, showCustomName: true, formTitle: '' });
    } else {
      this.setData({ nameIndex: index, showCustomName: false, formTitle: name });
    }
  },

  // 创建或保存
  async handleSave() {
    if (!this.data.formTitle || !this.data.formDate) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' });
      return;
    }

    // 校验名称是否重复
    const title = this.data.formTitle.trim();
    const isDuplicate = this.data.anniversaryList.some(item => {
      if (this.data.isEditing && item.id === this.data.editId) return false;
      return item.title === title;
    });
    if (isDuplicate) {
      wx.showToast({ title: '该纪念日名称已存在', icon: 'none' });
      return;
    }

    wx.showLoading({ title: this.data.isEditing ? '保存中...' : '创建中...' });
    try {
      if (this.data.isEditing) {
        await api.updateAnniversary({
          id: this.data.editId,
          title: title,
          anniversaryDate: this.data.formDate,
          remindDays: this.data.formRemindDays,
          icon: this.data.formIcon
        });
        wx.hideLoading();
        this.hideModal();
        wx.showToast({ title: '已更新', icon: 'success' });
      } else {
        await api.createAnniversary({
          title: this.data.formTitle,
          anniversaryDate: this.data.formDate,
          remindDays: this.data.formRemindDays,
          icon: this.data.formIcon
        });
        wx.hideLoading();
        this.hideModal();
        wx.showToast({ title: '创建成功', icon: 'success' });
      }
      this.loadList();
    } catch (err) { wx.hideLoading(); }
  },

  async handleDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个纪念日吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.deleteAnniversary(id);
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadList();
          } catch (err) {}
        }
      }
    });
  },

  // 从编辑弹窗删除
  handleDeleteModal() {
    const id = this.data.editId;
    if (!id) return;
    this.hideModal();
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个纪念日吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.deleteAnniversary(id);
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadList();
          } catch (err) {}
        }
      }
    });
  },

  getDaysText(daysLeft) {
    if (daysLeft === 0) return '今天';
    if (daysLeft < 0) return `${Math.abs(daysLeft)}天前`;
    return `${daysLeft}天后`;
  }
});
