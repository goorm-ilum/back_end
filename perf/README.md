# Product 성능 테스트(k6)

## 실행 전 공통
- 대상 서버 URL 지정(기본: http://localhost:8080)
```
set BASE_URL=http://localhost:8080
```
- 부하 크기/기간/지연 임계치 조정(옵션)
```
set VUS=50
set DURATION=2m
set P95_MS=400
```

## 시나리오
1) 목록 검색 반복
```
k6 run perf/product/listing.js
```
환경변수(옵션): `COUNTRIES=전체,한국,일본` `PAGE_SIZES=10,20,50`

2) 상세 조회 반복(무작위 ID)
```
k6 run perf/product/detail.js
```
환경변수(옵션): `PRODUCT_ID_MAX=10000000`

3) AI 검색 반복
```
k6 run perf/product/ai-search.js
```
환경변수(옵션): `QUESTIONS=여름 바다 휴양지 추천,가족 여행 3박4일`

4) 혼합 시나리오(목록/상세/AI 동시)
```
k6 run perf/product/mixed.js
```
환경변수(옵션): `LIST_VUS` `DETAIL_VUS` `AI_VUS` `USE_RAMPING=true` `STAGES=30s:20,60s:50,30s:0`

## 모니터링 연동
- Prometheus 타겟: http://localhost:9090/targets
- Grafana 접속: http://localhost:3000 (admin/admin)
  - Data source: Prometheus (`http://prometheus:9090`)
  - 대시보드: Micrometer/Spring Boot 임포트 권장





