#!/usr/bin/env node
/**
 * 命令行版自检脚本（和 App 里的取数逻辑一一对应）。
 * 用法：node tools/check_k4.js
 * 作用：不装 App 也能确认接口现在能返回 K4 的实时位置、站数算得对不对。
 */

const https = require('https');

const CONFIG = {
  baseUrl: 'https://ts-api.tundrey.com',
  cityId: '566',          // 西宁
  lineId: '566308918036639', // K4 绿地·云香郡 → 青海师范大学东
  targetOrder: 21,        // 宁张路二十里铺
  stationId: '566-374',
  lat: '36.712286',
  lng: '101.762056',
  stationName: '宁张路二十里铺',
};

function getJson(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, { headers: { Accept: 'application/json' } }, (res) => {
      let data = '';
      res.setEncoding('utf8');
      res.on('data', (c) => (data += c));
      res.on('end', () => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(`HTTP ${res.statusCode}: ${data.slice(0, 200)}`));
          return;
        }
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(new Error(`返回的不是 JSON: ${data.slice(0, 200)}`));
        }
      });
    });
    req.on('error', reject);
    req.setTimeout(9000, () => req.destroy(new Error('请求超时')));
  });
}

function stopsText(n) {
  return n <= 0 ? '即将到站' : `还有 ${n} 站`;
}

function etaText(sec) {
  if (sec < 0) return '时间未知';
  if (sec <= 45) return '即将到站';
  if (sec < 60) return `${sec} 秒`;
  return `${Math.floor(sec / 60)} 分钟`;
}

function etaApprox(sec) {
  if (sec < 0) return '时间未知';
  if (sec <= 45) return '即将到站';
  return `约 ${etaText(sec)}`;
}

async function main() {
  const q = new URLSearchParams({
    city_id: CONFIG.cityId,
    line_id: CONFIG.lineId,
    target_order: String(CONFIG.targetOrder),
    station_id: CONFIG.stationId,
    lat: CONFIG.lat,
    lng: CONFIG.lng,
  });
  const url = `${CONFIG.baseUrl}/v1/lines/realtime?${q.toString()}`;
  const root = await getJson(url);

  const buses = (root.buses || [])
    .filter((b) => b.order >= 1 && b.order <= CONFIG.targetOrder)
    .map((b) => ({
      licence: b.licence || b.busId || '未知车牌',
      order: b.order,
      stops: CONFIG.targetOrder - b.order,
      meters: typeof b.distanceToWaitStn === 'number' ? b.distanceToWaitStn : -1,
      eta: b.eta && typeof b.eta.travelTime === 'number' ? b.eta.travelTime : -1,
    }))
    .sort((a, b) => a.stops - b.stops);

  console.log(`线路：K4 绿地·云香郡 → 青海师范大学东`);
  console.log(`目标站：${CONFIG.stationName}（第 ${CONFIG.targetOrder} 站）`);
  console.log(`实时定位：${root.realData ? '有' : '无'}  前方共 ${buses.length} 辆\n`);

  buses.slice(0, 3).forEach((b, i) => {
    const dist = b.meters < 0 ? '距离未知' : b.meters < 1000 ? `${b.meters} 米` : `${(b.meters / 1000).toFixed(1)} 公里`;
    console.log(`${i + 1}. ${b.licence}  ${stopsText(b.stops)}  ${dist}  ${etaApprox(b.eta)}`);
  });

  if (buses.length === 0) console.log('（现在没有开往该站的车）');
}

main().catch((e) => {
  console.error('查询失败：', e.message);
  process.exitCode = 1;
});
