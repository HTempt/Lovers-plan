const BASE_URL = 'http://localhost:8080/api';

/**
 * 发起HTTP请求
 */
function request(method, url, data = {}) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = { 'Content-Type': 'application/json' };
    if (token) {
      header['Authorization'] = 'Bearer ' + token;
    }

    wx.request({
      url: BASE_URL + url,
      method: method,
      data: data,
      header: header,
      timeout: 10000,
      success: (res) => {
        if (res.statusCode === 401) {
          // token过期，跳转登录页
          wx.removeStorageSync('token');
          wx.reLaunch({ url: '/pages/login/login' });
          reject(new Error('登录已过期'));
          return;
        }
        if (res.data.code === 200) {
          resolve(res.data.data);
        } else {
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' });
          reject(new Error(res.data.message));
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });
}

// 封装常用方法
const api = {
  // Auth
  login: (code) => request('POST', '/auth/login', { code }),
  getUserInfo: () => request('GET', '/auth/userinfo'),
  updateUserInfo: (data) => request('POST', '/auth/userinfo', data),

  // Couple
  createInvite: () => request('POST', '/couple/invite'),
  acceptInvite: (code) => request('POST', '/couple/bind', { code }),
  getCoupleInfo: () => request('GET', '/couple/info'),
  unbind: () => request('POST', '/couple/unbind'),

  // Status
  setStatus: (data) => request('POST', '/status/set', data),
  clearStatus: () => request('POST', '/status/clear'),
  getMyStatus: () => request('GET', '/status/current'),
  getPartnerStatus: () => request('GET', '/status/partner'),
  getStatusTemplates: () => request('GET', '/status/templates'),

  // Todo
  createTodo: (data) => request('POST', '/todo/create', data),
  updateTodo: (data) => request('POST', '/todo/update', data),
  completeTodo: (id) => request('POST', '/todo/complete', { id }),
  deleteTodo: (id) => request('POST', '/todo/delete', { id }),
  getTodoList: () => request('GET', '/todo/list'),

  // Diary
  createDiary: (data) => request('POST', '/diary/create', data),
  getTimeline: (params) => request('GET', '/diary/timeline', params),
  getAlbum: () => request('GET', '/diary/album'),
  getMapLocations: () => request('GET', '/diary/map'),
  deleteDiary: (id) => request('POST', '/diary/delete/' + id),
  restoreDiary: (id) => request('POST', '/diary/restore/' + id),
  getRecycleBin: () => request('GET', '/diary/recycle'),

  // File upload
  uploadFile: (filePath, mediaType) => {
    return new Promise((resolve, reject) => {
      const token = wx.getStorageSync('token');
      wx.uploadFile({
        url: BASE_URL + '/file/upload',
        filePath: filePath,
        name: 'file',
        formData: { mediaType },
        header: { 'Authorization': 'Bearer ' + token },
        success: (res) => {
          try {
            const data = JSON.parse(res.data);
            if (data.code === 200) resolve(data.data);
            else reject(new Error(data.message));
          } catch (e) {
            reject(e);
          }
        },
        fail: (err) => {
          wx.showToast({ title: '上传失败', icon: 'none' });
          reject(err);
        }
      });
    });
  },

  // Task
  createTask: (data) => request('POST', '/task/create', data),
  checkInTask: (taskId) => request('POST', '/task/check-in', { taskId }),
  confirmTask: (recordId) => request('POST', '/task/confirm', { recordId }),
  getTaskList: () => request('GET', '/task/list'),
  getBadges: () => request('GET', '/task/badges'),

  // Wish
  createWish: (data) => request('POST', '/wish/create', data),
  updateWishProgress: (data) => request('POST', '/wish/progress', data),
  achieveWish: (id) => request('POST', '/wish/achieve', { id }),
  getWishList: () => request('GET', '/wish/list'),

  // Anniversary
  createAnniversary: (data) => request('POST', '/anniversary/create', data),
  getAnniversaryList: () => request('GET', '/anniversary/list'),
  updateAnniversary: (data) => request('POST', '/anniversary/update', data),
  deleteAnniversary: (id) => request('POST', '/anniversary/delete/' + id),
  getUpcomingAnniversary: () => request('GET', '/anniversary/upcoming'),

  // Statistics
  getStatistics: () => request('GET', '/statistics/overview'),

  // Home
  getHomeData: () => request('GET', '/home/data'),

  // Common
  getLunarDate: () => request('GET', '/common/lunar'),

  // Subscribe
  getSubscribeTemplates: () => request('GET', '/subscribe/templates'),

  // Activity Feed
  getActivityFeed: (params) => request('GET', '/activity/list', params),

  // Love Tree
  getLoveTreeInfo: () => request('GET', '/love-tree/info'),
  getLoveTreeHistory: (params) => request('GET', '/love-tree/history', params),
  getLoveTreeLevels: () => request('GET', '/love-tree/levels'),
  getLoveTreeRewards: () => request('GET', '/love-tree/rewards'),

  // Sign In
  doSignIn: () => request('POST', '/sign-in/do'),
  getSignInStatus: () => request('GET', '/sign-in/status'),
};

module.exports = api;
