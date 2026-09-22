/**
 * 协议与隐私合规配置
 *
 * ⚠️ 上线前必须替换下面【待填写】的内容：
 *    1. OPERATOR_NAME —— 必须与小程序主体名称一致（微信审核会核对）；
 *    2. CONTACT_EMAIL —— 必须是真实可达的邮箱（用于接收查阅/删除/注销申请）。
 *    同时请确保微信公众平台「小程序用户隐私保护指引」中填写的内容与本文件的
 *    《隐私政策》保持一致，否则审核会以「隐私政策与实际收集行为不符」驳回。
 *
 * 协议内容发生实质性变更时（新增收集字段、扩大使用目的、变更共享对象等），
 * 必须递增 VERSION 并更新 UPDATED_AT，登录页会重新要求用户主动同意。
 */
const LEGAL = {
  APP_NAME: '双人岛',

  // 协议版本号
  VERSION: '1.0.0',

  // 生效/更新日期
  UPDATED_AT: '2026-01-01',

  // 运营者主体名称（须与小程序主体一致）
  OPERATOR_NAME: '【待填写：小程序主体名称】',

  // 个人信息保护负责人 / 客服联系邮箱（须真实可达）
  CONTACT_EMAIL: '【待填写：联系邮箱】',

  // 处理用户请求的时限（工作日）
  HANDLE_DAYS: 15
};

// 本地留存的同意记录 key
const CONSENT_KEY = 'legal_consent_record';

/**
 * 记录用户本次「主动同意」的协议版本与时间，便于合规审计
 */
function saveConsent() {
  const record = {
    version: LEGAL.VERSION,
    agreedAt: Date.now()
  };
  try {
    wx.setStorageSync(CONSENT_KEY, record);
  } catch (e) {
    // 存储失败不影响主流程
  }
  return record;
}

/**
 * 读取本地同意记录（无记录时返回 null）
 */
function getConsent() {
  try {
    return wx.getStorageSync(CONSENT_KEY) || null;
  } catch (e) {
    return null;
  }
}

/**
 * 清除同意记录（用户撤回授权时调用）
 */
function clearConsent() {
  try {
    wx.removeStorageSync(CONSENT_KEY);
  } catch (e) {
    // 忽略
  }
}

module.exports = {
  LEGAL,
  CONSENT_KEY,
  saveConsent,
  getConsent,
  clearConsent
};