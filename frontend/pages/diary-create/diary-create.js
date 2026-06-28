const api = require('../../utils/api');
const CONFIG = require('../../utils/config');

// 高德地图逆地址解析：经纬度 → 省/市/区
function reverseGeocode(latitude, longitude) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: 'https://restapi.amap.com/v3/geocode/regeo',
      data: {
        output: 'json',
        location: `${longitude},${latitude}`,
        key: CONFIG.AMAP_KEY,
        radius: 1000
      },
      success: (res) => {
        if (res.data && res.data.status === '1' && res.data.regeocode) {
          const ac = res.data.regeocode.addressComponent;
          // 直辖市 city 为空数组，此时用 province 作为 city
          const cityVal = Array.isArray(ac.city) && ac.city.length === 0
            ? ac.province : (ac.city || ac.province);
          resolve({
            province: ac.province || '',
            city: cityVal || '',
            district: ac.district || '',
            address: res.data.regeocode.formatted_address || ''
          });
        } else {
          reject(new Error(res.data ? res.data.info : '逆地址解析失败'));
        }
      },
      fail: (err) => reject(err)
    });
  });
}

Page({
  data: {
    title: '',
    content: '',
    mood: '',
    location: '',
    province: '',
    city: '',
    latitude: '',
    longitude: '',
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

  selectMood(e) {
    this.setData({ mood: e.currentTarget.dataset.mood });
  },

  onLocationInput(e) {
    const text = e.detail.value;
    // 检测是否为城市名称（含"市"或"省"）
    const cityInfo = this.parseAddress(text);
    this.setData({
      location: text,
      province: cityInfo.province || '',
      city: cityInfo.city || ''
    });
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
    // 先获取用户当前位置，作为定位地图的默认中心
    wx.getLocation({
      type: 'gcj02',
      success: (loc) => {
        wx.chooseLocation({
          latitude: loc.latitude,
          longitude: loc.longitude,
          success: (res) => {
            const parts = [res.name, res.address].filter(Boolean);
            const loc = parts.join(' · ') || '';
            this.setData({
              location: loc,
              latitude: res.latitude,
              longitude: res.longitude
            });
            // 通过逆地址解析获取标准省/市信息
            this.resolveCity(res.latitude, res.longitude);
          },
          fail: (err) => {
            console.error('选择位置失败', err);
            wx.showToast({ title: '获取位置失败', icon: 'none' });
          }
        });
      },
      fail: () => {
        // 获取当前位置失败，直接打开选择（无默认位置）
        wx.chooseLocation({
          success: (res) => {
            const parts = [res.name, res.address].filter(Boolean);
            const loc = parts.join(' · ') || '';
            this.setData({
              location: loc,
              latitude: res.latitude,
              longitude: res.longitude
            });
            this.resolveCity(res.latitude, res.longitude);
          },
          fail: (err) => {
            console.error('选择位置失败', err);
            wx.showToast({ title: '获取位置失败', icon: 'none' });
          }
        });
      }
    });
  },

  /** 逆地址解析：经纬度 → 省/市 */
  resolveCity(latitude, longitude) {
    reverseGeocode(latitude, longitude).then((geo) => {
      this.setData({
        province: geo.province,
        city: geo.city
      });
      console.log('定位解析成功:', geo.province, geo.city);
    }).catch((err) => {
      console.warn('逆地址解析失败，使用地址文本解析兜底', err);
      // 兜底：从 wx.chooseLocation 的 address 文本中解析
      const { province, city } = this.parseAddress(this.data.location);
      if (city) this.setData({ province, city });
    });
  },

  /** 获取当前所在城市（一键定位城市） */
  getCurrentCity() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        wx.showLoading({ title: '定位中...' });
        reverseGeocode(res.latitude, res.longitude).then((geo) => {
          wx.hideLoading();
          this.setData({
            location: geo.city || geo.province,
            province: geo.province,
            city: geo.city,
            latitude: res.latitude,
            longitude: res.longitude
          });
          wx.showToast({
            title: '当前城市：' + (geo.city || geo.province),
            icon: 'none',
            duration: 2000
          });
        }).catch(() => {
          wx.hideLoading();
          // AMap 解析失败，用坐标作为兜底
          this.setData({
            latitude: res.latitude,
            longitude: res.longitude
          });
          wx.showToast({ title: '已记录位置，城市识别失败', icon: 'none' });
        });
      },
      fail: () => {
        wx.showToast({ title: '请开启位置权限', icon: 'none' });
      }
    });
  },

  // 从地址字符串解析省份和城市
  // 例如："浙江省杭州市西湖区..." → { province: "浙江省", city: "杭州市" }
  parseAddress(address) {
    if (!address) return { province: '', city: '' };
    // 直辖市：北京市/天津市/上海市/重庆市
    const municipalities = ['北京市', '天津市', '上海市', '重庆市'];
    for (const m of municipalities) {
      if (address.startsWith(m)) {
        return { province: m, city: m };
      }
    }
    // 常规省份："浙江省杭州市..."
    const provinceMatch = address.match(/^([^省]+省)/);
    if (provinceMatch) {
      const province = provinceMatch[1];
      const rest = address.substring(province.length);
      const cityMatch = rest.match(/^([^市]+市)/);
      if (cityMatch) {
        return { province, city: cityMatch[1] };
      }
      return { province, city: '' };
    }
    // 自治区："广西壮族自治区南宁市..."
    const regionMatch = address.match(/^([^自治区]+自治区)/);
    if (regionMatch) {
      const province = regionMatch[1];
      const rest = address.substring(province.length);
      const cityMatch = rest.match(/^([^市]+市)/);
      return { province, city: cityMatch ? cityMatch[1] : '' };
    }
    // 仅输入城市名（如"北京市""杭州市"）
    const cityOnlyMatch = address.match(/^([^市]+市)/);
    if (cityOnlyMatch) {
      return { province: '', city: cityOnlyMatch[1] };
    }
    return { province: '', city: '' };
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
        mood: this.data.mood,
        location: this.data.location,
        province: this.data.province,
        city: this.data.city,
        latitude: this.data.latitude,
        longitude: this.data.longitude,
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
