const api = require('../../utils/api');

Page({
  data: {
    title: '',
    content: '',
    location: '',
    mediaList: [],
    uploading: false,
    submitting: false
  },

  onTitleInput(e) {
    this.setData({ title: e.detail.value });
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value });
  },

  onLocationInput(e) {
    this.setData({ location: e.detail.value });
  },

  chooseImage() {
    wx.chooseMedia({
      count: 9,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = res.tempFiles.map(f => ({
          tempPath: f.tempFilePath,
          mediaType: 'image'
        }));
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

  getLocation() {
    wx.chooseLocation({
      success: (res) => {
        // res.name 是具体地点名（如"故宫博物院"），res.address 是地址
        // 组合显示更完整，避免只返回"北京市北京市"这样的市级地址
        const parts = [res.name, res.address].filter(Boolean);
        const loc = parts.join(' · ') || '';
        this.setData({ location: loc });
      },
      fail: (err) => {
        console.error('选择位置失败', err);
        wx.showToast({ title: '获取位置失败', icon: 'none' });
      }
    });
  },

  async handleSubmit() {
    if (!this.data.title) {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({ title: '发布中...' });

    try {
      // 先上传媒体文件
      const uploadedMedia = [];
      for (const media of this.data.mediaList) {
        wx.showLoading({ title: `上传中 ${uploadedMedia.length + 1}/${this.data.mediaList.length}` });
        const result = await api.uploadFile(media.tempPath, media.mediaType);
        uploadedMedia.push({
          fileUrl: result.url,
          mediaType: media.mediaType
        });
      }

      // 创建日记
      const data = {
        title: this.data.title,
        content: this.data.content,
        location: this.data.location,
        mediaList: uploadedMedia
      };

      await api.createDiary(data);
      wx.hideLoading();
      wx.showToast({ title: '发布成功', icon: 'success' });
      wx.navigateBack();
    } catch (err) {
      wx.hideLoading();
    } finally {
      this.setData({ submitting: false });
    }
  }
});
