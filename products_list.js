import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// 실행 옵션: VU 50명, 2분 동안 실행
export const options = {
    vus: 1,
    duration: '5s',
    thresholds: {
        http_req_failed: ['rate<0.01'],      // 실패율 1% 미만
        http_req_duration: ['p(95)<500'],    // 95% 요청이 500ms 미만
    },
};

// 커스텀 메트릭
const listDuration = new Trend('product_list_ms', true);
const listCount = new Counter('product_list_count');

export default function () {
    const base = __ENV.BASE_URL || 'http://localhost:8080';

    // 컨트롤러 defaultValue를 쓰기 때문에 page, size, sort는 전달하지 않음
    const url = `${base}/api/products?countryName=${encodeURIComponent('전체')}`;

    const start = Date.now();
    const res = http.get(url, {
        tags: { endpoint: 'GET /api/products' },
        timeout: '10s',
    });
    listDuration.add(Date.now() - start);
    listCount.add(1);

    // 디버그 로그로 상태/본문 일부 확인
    console.log(`status=`, res.status);
    if (res.body) {
        console.log(`body(200 bytes)=`, res.body.substring(0, 200));
    }

    check(res, { 'status is 2xx': (r) => r.status >= 200 && r.status < 300 });

    // 0.1초 휴식 (사용자 think time 흉내)
    sleep(0.1);
}
