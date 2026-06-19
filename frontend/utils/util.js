/**
 * 工具函数
 */

// 日期格式化
function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

// 日期时间格式化（含秒）
function formatDateTime(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  const sec = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${min}:${sec}`;
}

// 相对时间描述
function timeAgo(dateStr) {
  if (!dateStr) return '';
  const now = Date.now();
  const date = new Date(dateStr).getTime();
  const diff = now - date;

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 30) return `${days}天前`;
  return formatDate(dateStr);
}

// 复制文本
function copyText(text) {
  wx.setClipboardData({
    data: text,
    success: () => {
      wx.showToast({ title: '已复制', icon: 'success' });
    }
  });
}

// 显示加载
function showLoading(title = '加载中...') {
  wx.showLoading({ title, mask: true });
}

// 隐藏加载
function hideLoading() {
  wx.hideLoading();
}

// 订阅消息
function requestSubscribe(tmplIds) {
  return new Promise((resolve, reject) => {
    wx.requestSubscribeMessage({
      tmplIds: tmplIds,
      success: (res) => {
        // 检查每个模板的订阅结果
        let allAccepted = true;
        tmplIds.forEach(id => {
          if (res[id] !== 'accept') allAccepted = false;
        });
        resolve(allAccepted);
      },
      fail: (err) => {
        console.log('订阅消息请求失败', err);
        resolve(false);
      }
    });
  });
}

module.exports = {
  formatDate,
  formatDateTime,
  timeAgo,
  copyText,
  showLoading,
  hideLoading,
  requestSubscribe
};
