import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';
import { ok, toThresholds, vusAndDuration, getBaseUrl, sleepMs, parseCsvEnv, pickRandom } from '../lib/util.js';

const { vus, duration } = vusAndDuration(20, '2m');

export const options = {
  vus,
  duration,
  thresholds: toThresholds(800), // AI 검색은 상대적으로 느릴 수 있음
};

const questions = parseCsvEnv('QUESTIONS', [
  '여름 바다 휴양지 추천',
  '가족 여행 3박4일',
  '겨울 스키 패키지',
  '유럽 배낭여행 코스',
]);

const aiDuration = new Trend('product_ai_search_ms');
const aiCount = new Counter('product_ai_search_count');

export default function () {
  const base = getBaseUrl();
  const q = pickRandom(questions);
  const url = `${base}/api/products/aisearch?question=${encodeURIComponent(q)}&page=0&size=10`;
  const params = { tags: { endpoint: 'GET /api/products/aisearch' } };
  const start = Date.now();
  const res = http.get(url, params);
  aiDuration.add(Date.now() - start);
  aiCount.add(1);
  ok(res);
  sleepMs(200);
}





