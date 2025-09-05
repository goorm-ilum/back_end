import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';
import { ok, toThresholds, vusAndDuration, getBaseUrl, sleepMs } from '../lib/util.js';

const { vus, duration } = vusAndDuration(30, '2m');

export const options = {
  vus,
  duration,
  thresholds: toThresholds(400),
};

const detailDuration = new Trend('product_detail_ms');
const detailCount = new Counter('product_detail_count');

function randomProductId() {
  // 대용량 데이터 가정: 1..10_000_000 범위 임의 선택
  const min = 1;
  const max = Number(__ENV.PRODUCT_ID_MAX || '10000000');
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

export default function () {
  const base = getBaseUrl();
  const productId = randomProductId();
  const page = 0; // 내부 페이지네이션(리뷰, 이미지 등)
  const size = 3;
  const url = `${base}/api/products/${productId}?page=${page}&size=${size}`;
  const params = { tags: { endpoint: 'GET /api/products/{id}' }, timeout: '60s' };
  const start = Date.now();
  const res = http.get(url, params);
  detailDuration.add(Date.now() - start);
  detailCount.add(1);
  ok(res);
  sleepMs(100);
}


