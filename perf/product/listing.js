import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';
import { ok, toThresholds, vusAndDuration, getBaseUrl, sleepMs } from '../lib/util.js';

const { vus, duration } = vusAndDuration(50, '2m');

export const options = {
  vus,
  duration,
  thresholds: toThresholds(400),
};

const listDuration = new Trend('product_list_ms');
const listCount = new Counter('product_list_count');

export default function () {
  const base = getBaseUrl();
  // 서버의 기본(defaultValue) 파라미터를 그대로 사용하기 위해 쿼리스트링 없이 호출
  const params = { tags: { endpoint: 'GET /api/products' }, timeout: '10s' };
  const url = `${base}/api/products`;
  const start = Date.now();
  const res = http.get(url, params);
  listDuration.add(Date.now() - start);
  listCount.add(1);
  ok(res);
  sleepMs(100);
}


