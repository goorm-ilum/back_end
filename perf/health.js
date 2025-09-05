import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || '10'),
  duration: __ENV.DURATION || '10s',
};

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://172.16.24.32:8080';
  const res = http.get(`${baseUrl}/actuator/health`, { tags: { endpoint: 'GET /actuator/health' } });
  check(res, { 'status 200': (r) => r.status === 200 });
}


