const api = require('../../utils/api');

Page({
  data: {
    question: null,
    selectedAnswer: '',
    submitted: false,
    result: null,
    loading: true,
    error: '',
    submitting: false,
    typeLabel: ''
  },

  onShow() {
    this.loadToday();
  },

  async loadToday() {
    this.setData({ loading: true, error: '' });
    try {
      const q = await api.getTodayQuestion();
      this.setData({
        question: q,
        typeLabel: this.getTypeLabel(q.questionType),
        loading: false,
        submitted: false,
        selectedAnswer: '',
        result: null
      });
      // 检查是否已答过
      await this.checkResult();
    } catch (err) {
      const msg = err.message || '';
      // 问题池为空时给友好提示
      if (msg.includes('问题池为空')) {
        this.setData({
          loading: false,
          error: '暂没有问题可用',
          errorDetail: '管理员正在准备新问题，请稍后再来'
        });
      } else {
        this.setData({ loading: false, error: '加载失败', errorDetail: msg });
      }
    }
  },

  async checkResult() {
    try {
      const res = await api.getQuizResult();
      if (res.answered && res.score) {
        this.setData({ submitted: true, result: res });
      } else if (res.waitingPartner) {
        this.setData({ submitted: true, result: null });
      }
    } catch (err) {
      // ignore
    }
  },

  selectAnswer(e) {
    this.setData({ selectedAnswer: e.currentTarget.dataset.answer });
  },

  async handleSubmit() {
    if (!this.data.selectedAnswer || this.data.submitting) return;
    this.setData({ submitting: true });
    wx.showLoading({ title: '提交中...' });
    try {
      const res = await api.submitAnswer({
        dailyQuestionId: this.data.question.dailyQuestionId,
        answer: this.data.selectedAnswer
      });
      wx.hideLoading();
      this.setData({ submitted: true, submitting: false });

      if (res.bothAnswered && res.score) {
        // 重新获取完整结果
        const result = await api.getQuizResult();
        this.setData({ result });
        wx.showToast({
          title: res.matched ? '💞 默契一致！' : '💔 有点差异',
          icon: 'none'
        });
      } else {
        wx.showToast({ title: '已提交，等待TA作答', icon: 'none' });
      }
    } catch (err) {
      wx.hideLoading();
      this.setData({ submitting: false });
    }
  },

  getTypeLabel(type) {
    const map = {
      memory: '💭 回忆类',
      preference: '💖 偏好类',
      emotion: '💕 情感类',
      future: '🌟 未来类',
      fun: '😄 娱乐类'
    };
    return map[type] || '❤️ 问答';
  },

  getOptionText(answer) {
    if (!this.data.question || !answer) return '';
    const map = { A: 'optionA', B: 'optionB', C: 'optionC', D: 'optionD' };
    return this.data.question[map[answer]] || '';
  }
});
