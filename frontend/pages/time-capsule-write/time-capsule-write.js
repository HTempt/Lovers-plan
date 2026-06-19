const api = require('../../utils/api');

Page({
  data: {
    pairCapsuleId: null,
    capsule: null,
    content: '',
    mediaList: [],
    submitting: false
  },

  onLoad(options) {
    if (options.pairId) {
      this.setData({ pairCapsuleId: parseInt(options.pairId) });
      this.loadCapsuleInfo();
    }
  },

  async loadCapsuleInfo() {
    try {
      const detail = await api.getCapsuleDetail(this.data.pairCapsuleId);
      this.setData({ capsule: detail });
    } catch (err) {
      wx.showToast({ title: '获取胶囊信息失败', icon: 'none' });
    }
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value });
  },

  chooseImage() {
    wx.chooseMedia({
      count: 9, mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = res.tempFiles.map(f => ({ tempPath: f.tempFilePath, mediaType: 'image' }));
        this.setData({ mediaList: [...this.data.mediaList, ...files] });
      }
    });
  },

  chooseVoice() {
    wx.chooseMedia({
      count: 1, mediaType: ['audio'],
      success: (res) => {
        const files = res.tempFiles.map(f => ({ tempPath: f.tempFilePath, mediaType: 'audio' }));
        this.setData({ mediaList: [...this.data.mediaList, ...files] });
      }
    });
  },

  chooseVideo() {
    wx.chooseMedia({
      count: 1, mediaType: ['video'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = res.tempFiles.map(f => ({ tempPath: f.tempFilePath, mediaType: 'video' }));
        this.setData({ mediaList: [...this.data.mediaList, ...files] });
      }
    });
  },

  removeMedia(e) {
    const index = e.currentTarget.dataset.index;
    const list = [...this.data.mediaList];
    list.splice(index, 1);
    this.setData({ mediaList: list });
  },

  async handleSubmit() {
    if (this.data.submitting) return;
    this.setData({ submitting: true });
    wx.showLoading({ title: '封存中...' });

    try {
      // 上传媒体
      const uploadedMedia = [];
      for (const media of this.data.mediaList) {
        wx.showLoading({ title: `上传中 ${uploadedMedia.length + 1}/${this.data.mediaList.length}` });
        const result = await api.uploadFile(media.tempPath, media.mediaType);
        uploadedMedia.push({ fileUrl: result.url, mediaType: media.mediaType });
      }

      await api.writePartnerCapsule({
        pairCapsuleId: this.data.pairCapsuleId,
        content: this.data.content,
        mediaList: uploadedMedia
      });

      wx.hideLoading();
      wx.showToast({ title: '💌 已封存', icon: 'success' });
      wx.navigateBack();
    } catch (err) {
      wx.hideLoading();
    } finally {
      this.setData({ submitting: false });
    }
  },

  getTypeIcon(type) {
    const map = {
      'to_future_ta': '💌', 'to_future_us': '💑',
      'birthday': '🎂', 'anniversary': '💍', 'wish': '✨'
    };
    return map[type] || '💌';
  }
});
