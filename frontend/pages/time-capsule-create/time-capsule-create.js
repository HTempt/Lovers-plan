const api = require('../../utils/api');

Page({
  data: {
    step: 1,
    capsuleTypes: [],
    openOptions: [],
    formType: '',
    formTitle: '',
    formContent: '',
    formOpenDays: 30,
    customDate: '',
    showCustomDate: false,
    dualMode: false,
    mediaList: [],
    submitting: false
  },

  onLoad() {
    this.loadOptions();
  },

  async loadOptions() {
    try {
      const [types, options] = await Promise.all([
        api.getCapsuleTypes(),
        api.getOpenOptions()
      ]);
      this.setData({ capsuleTypes: types || [], openOptions: options || [] });
    } catch (err) {
      console.error('加载选项失败', err);
    }
  },

  // === 类型选择 ===
  onTypeChange(e) {
    this.setData({ formType: e.currentTarget.dataset.value });
  },

  onDualModeChange(e) {
    this.setData({ dualMode: e.detail.value });
  },

  goStep2() {
    if (!this.data.formType) {
      wx.showToast({ title: '请选择胶囊类型', icon: 'none' });
      return;
    }
    this.setData({ step: 2 });
  },

  // === 内容填写 ===
  onTitleInput(e) {
    this.setData({ formTitle: e.detail.value });
  },

  onContentInput(e) {
    this.setData({ formContent: e.detail.value });
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

  chooseVoice() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['audio'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = res.tempFiles.map(f => ({
          tempPath: f.tempFilePath,
          mediaType: 'audio'
        }));
        this.setData({ mediaList: [...this.data.mediaList, ...files] });
      }
    });
  },

  chooseVideo() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['video'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = res.tempFiles.map(f => ({
          tempPath: f.tempFilePath,
          mediaType: 'video'
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

  goStep3() {
    if (!this.data.formTitle) {
      wx.showToast({ title: '请填写胶囊标题', icon: 'none' });
      return;
    }
    this.setData({ step: 3 });
  },

  prevStep() {
    if (this.data.step > 1) {
      this.setData({ step: this.data.step - 1 });
    }
  },

  // === 时间选择 ===
  onOpenDaysChange(e) {
    const days = parseInt(e.currentTarget.dataset.days);
    this.setData({
      formOpenDays: days,
      showCustomDate: days === -1,
      customDate: days === -1 ? this.getDefaultCustomDate() : ''
    });
  },

  onCustomDateChange(e) {
    this.setData({ customDate: e.detail.value });
  },

  getDefaultCustomDate() {
    const d = new Date();
    d.setFullYear(d.getFullYear() + 1);
    return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`;
  },

  // === 提交 ===
  async handleSubmit() {
    if (this.data.submitting) return;

    if (!this.data.formTitle) {
      wx.showToast({ title: '请填写胶囊标题', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({ title: '封存中...' });

    try {
      // 上传媒体
      const uploadedMedia = [];
      for (const media of this.data.mediaList) {
        wx.showLoading({ title: `上传中 ${uploadedMedia.length + 1}/${this.data.mediaList.length}` });
        const result = await api.uploadFile(media.tempPath, media.mediaType);
        uploadedMedia.push({
          fileUrl: result.url,
          mediaType: media.mediaType
        });
      }

      // 计算 openAt
      let openAt;
      if (this.data.showCustomDate && this.data.customDate) {
        openAt = this.data.customDate + ' 00:00:00';
      } else {
        const d = new Date();
        d.setDate(d.getDate() + this.data.formOpenDays);
        openAt = `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} 00:00:00`;
      }

      const data = {
        type: this.data.formType,
        title: this.data.formTitle,
        content: this.data.formContent,
        openAt: openAt,
        dualMode: this.data.dualMode,
        mediaList: uploadedMedia
      };

      await api.createCapsule(data);
      wx.hideLoading();
      wx.showToast({ title: '💌 胶囊已封存', icon: 'success' });
      wx.navigateBack();
    } catch (err) {
      wx.hideLoading();
    } finally {
      this.setData({ submitting: false });
    }
  },

  getTypeInfo(type) {
    const map = {
      'to_future_ta': { icon: '💌', label: '给未来的TA' },
      'to_future_us': { icon: '💑', label: '给未来的我们' },
      'birthday': { icon: '🎂', label: '生日胶囊' },
      'anniversary': { icon: '💍', label: '纪念日胶囊' },
      'wish': { icon: '✨', label: '愿望达成胶囊' }
    };
    return map[type] || { icon: '💌', label: '时光胶囊' };
  }
});
