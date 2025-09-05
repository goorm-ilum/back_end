import { check, sleep } from 'k6';

export function parseCsvEnv(name, fallback) {
  const raw = __ENV[name];
  if (!raw) return fallback;
  return raw.split(',').map((s) => s.trim()).filter(Boolean);
}

export function pickRandom(array) {
  return array[Math.floor(Math.random() * array.length)];
}

export function toThresholds(defaultP95 = 300) {
  const p95 = Number(__ENV.P95_MS || defaultP95);
  return {
    http_req_failed: ['rate<0.01'],
    http_req_duration: [`p(95)<${p95}`],
  };
}

export function vusAndDuration(defaultVus = 30, defaultDuration = '1m') {
  const vus = Number(__ENV.VUS || defaultVus);
  const duration = __ENV.DURATION || defaultDuration;
  return { vus, duration };
}

export function getBaseUrl() {
  return __ENV.BASE_URL || 'http://localhost:8080';
}

export function ok(resp, label = 'status 200') {
  return check(resp, { [label]: (r) => r && r.status === 200 });
}

export function rampingStages() {
  // Optional ramping, controlled via env (USE_RAMPING=true)
  if (__ENV.USE_RAMPING !== 'true') return undefined;
  const s = __ENV.STAGES || '30s:20,60s:50,30s:0';
  return s.split(',').map((seg) => {
    const [dur, vus] = seg.split(':');
    return { duration: dur, target: Number(vus) };
  });
}

export function sleepMs(ms) {
  sleep(ms / 1000);
}





