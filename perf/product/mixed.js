import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';
import { ok, toThresholds, getBaseUrl, rampingStages, parseCsvEnv, pickRandom, sleepMs } from '../lib/util.js';

export const options = {
  thresholds: toThresholds(500),
  scenarios: {
    list_heavy: {
      executor: __ENV.USE_RAMPING === 'true' ? 'ramping-vus' : 'constant-vus',
      exec: 'listExec',
      vus: Number(__ENV.LIST_VUS || 40),
      duration: __ENV.LIST_DURATION || '2m',
      stages: rampingStages(),
    },
    detail_mix: {
      executor: 'constant-vus',
      exec: 'detailExec',
      vus: Number(__ENV.DETAIL_VUS || 20),
      duration: __ENV.DETAIL_DURATION || '2m',
    },
    ai_light: {
      executor: 'constant-vus',
      exec: 'aiExec',
      vus: Number(__ENV.AI_VUS || 10),
      duration: __ENV.AI_DURATION || '2m',
    },
  },
};

const countries = parseCsvEnv('COUNTRIES', ['전체']);
const pageSizes = parseCsvEnv('PAGE_SIZES', ['10', '20', '50']).map((v) => Number(v));
const questions = parseCsvEnv('QUESTIONS', [
  '여름 바다 휴양지 추천', '가족 여행 3박4일', '겨울 스키 패키지', '유럽 배낭여행 코스',
]);

const listMs = new Trend('mixed_list_ms');
const detailMs = new Trend('mixed_detail_ms');
const aiMs = new Trend('mixed_ai_ms');
const listCount = new Counter('mixed_list_count');
const detailCount = new Counter('mixed_detail_count');
const aiCount = new Counter('mixed_ai_count');

function randomProductId() {
  const min = 1;
  const max = Number(__ENV.PRODUCT_ID_MAX || '10000000');
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

export function listExec() {
  const base = getBaseUrl();
  const country = pickRandom(countries);
  const size = pickRandom(pageSizes);
  const page = Math.floor(Math.random() * 50);
  const url = `${base}/api/products?countryName=${encodeURIComponent(country)}&page=${page}&size=${size}`;
  const start = Date.now();
  const res = http.get(url, { tags: { endpoint: 'GET /api/products' } });
  listMs.add(Date.now() - start);
  listCount.add(1);
  ok(res);
  sleepMs(100);
}

export function detailExec() {
  const base = getBaseUrl();
  const productId = randomProductId();
  const url = `${base}/api/products/${productId}?page=0&size=3`;
  const start = Date.now();
  const res = http.get(url, { tags: { endpoint: 'GET /api/products/{id}' } });
  detailMs.add(Date.now() - start);
  detailCount.add(1);
  ok(res);
  sleepMs(120);
}

export function aiExec() {
  const base = getBaseUrl();
  const q = pickRandom(questions);
  const url = `${base}/api/products/aisearch?question=${encodeURIComponent(q)}&page=0&size=10`;
  const start = Date.now();
  const res = http.get(url, { tags: { endpoint: 'GET /api/products/aisearch' } });
  aiMs.add(Date.now() - start);
  aiCount.add(1);
  ok(res);
  sleepMs(200);
}





