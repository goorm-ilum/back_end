import http from 'k6/http';
import { ok, toThresholds, vusAndDuration, getBaseUrl, sleepMs } from '../lib/util.js';

const { vus, duration } = vusAndDuration(5, '10s');

export const options = {
  vus,
  duration,
  thresholds: toThresholds(400),
};

export default function () {
  const base = getBaseUrl();
  const params = { tags: { endpoint: 'GET /actuator/info' }, timeout: '5s' };
  const res = http.get(`${base}/actuator/info`, params);
  ok(res);
  sleepMs(100);
}





