/**
 * 高德地图 Web API 配置
 *
 * ⚠️ 重要：逆地址解析使用的是「Web服务」API，
 * 请在 https://lbs.amap.com/ 创建「Web服务」类型的 Key（不是微信小程序类型）
 *
 * 使用步骤：
 * 1. 登录高德开放平台 → 应用管理 → 创建应用
 * 2. 添加 Key → 选择「Web服务」平台
 * 3. 将下方 AMAP_KEY 替换为获取的 Key
 * 4. 微信公众平台「开发管理 - 服务器域名」添加 https://restapi.amap.com
 */
const CONFIG = {
  // ↓↓↓ 替换为你的高德 Web 服务 Key（不是微信小程序 Key）↓↓↓
  AMAP_KEY: '20c55f42642985f2a1d10e05e1626b5a',
};

module.exports = CONFIG;
