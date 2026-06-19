const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    loading: true,
    stats: { totalPlaces: 0, totalCities: 0 },
    footprints: [],
    cityRanking: [],
    markers: [],
    latitude: 35.86,
    longitude: 104.19,
    scale: 4,
    hasData: false
  },

  onLoad() {
    this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [stats, footprints, cityRanking] = await Promise.all([
        api.getFootprintStats(),
        api.getFootprints(),
        api.getCityRanking()
      ]);
      const hasData = footprints && footprints.length > 0;
      const markers = hasData ? this.buildMarkers(footprints) : [];

      let centerLat = 35.86, centerLng = 104.19, scale = 4;
      if (hasData) {
        // 以第一个足迹为中心
        centerLat = parseFloat(footprints[0].latitude);
        centerLng = parseFloat(footprints[0].longitude);
        scale = 5;
      }

      this.setData({
        stats,
        footprints,
        cityRanking,
        markers,
        latitude: centerLat,
        longitude: centerLng,
        scale,
        hasData,
        loading: false
      });
    } catch (err) {
      console.error('加载足迹数据失败', err);
      this.setData({ loading: false });
    }
  },

  buildMarkers(footprints) {
    return footprints.map((fp, index) => ({
      id: fp.id,
      latitude: parseFloat(fp.latitude),
      longitude: parseFloat(fp.longitude),
      title: fp.locationName || fp.city || '足迹',
      callout: {
        content: fp.locationName || fp.city || '',
        fontSize: 12,
        borderRadius: 4,
        bgColor: '#ffffff',
        padding: 6,
        display: 'ALWAYS'
      },
      label: {
        content: (index + 1).toString(),
        fontSize: 10,
        color: '#ffffff'
      }
    }));
  },

  onMarkerTap(e) {
    const markerId = e.markerId;
    const fp = this.data.footprints.find(f => f.id === markerId);
    if (fp) {
      wx.showToast({
        title: fp.locationName || fp.city || '足迹',
        icon: 'none',
        duration: 1500
      });
    }
  }
});
