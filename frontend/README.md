# 데이터 표준 용어사전 - 프론트엔드

React 19 + TypeScript + Vite + Ant Design 6. 백엔드(Spring Boot, 저장소 루트)의 REST API를 사용한다.

## 실행

```bash
npm install
npm run dev
```

- http://localhost:5173 에 접속한다. `/api` 요청은 개발 서버가 백엔드(기본 `http://localhost:8080`)로 넘긴다.
- 백엔드를 다른 포트로 띄웠다면 `.env.local` 파일에 `VITE_API_PROXY_TARGET=http://localhost:8081`을 적는다.
- 로컬 관리자 계정은 백엔드 `application-local.yml`의 `app.initial-admin` 설정을 따른다.

## 스크립트

| 명령 | 설명 |
|---|---|
| `npm run dev` | 개발 서버 |
| `npm run build` | 타입 검사 후 `dist/`로 빌드 |
| `npm run typecheck` | 타입 검사만 |
| `npm run api:fetch` | 백엔드 API 문서를 `openapi/api-docs.json`으로 저장 (`API_DOCS_URL`로 주소 변경) |
| `npm run api:types` | API 문서로 `src/api/schema.d.ts` 타입 생성 |
| `npm run api:generate` | 위 두 개를 차례로 실행 |

## API 호출

```tsx
import { $api } from '@/api/client'

const { data } = $api.useQuery('get', '/api/words', { params: { query: { keyword: '고객', page: 0, size: 20 } } })
const createWord = $api.useMutation('post', '/api/words')
createWord.mutate({ body: { name: '고객', abbreviation: 'CUST', englishName: 'Customer', classifier: false } })
```

경로, 파라미터, 요청 본문, 응답 타입은 `src/api/schema.d.ts`(생성 파일)에서 온다. 백엔드 API가 바뀌면 `npm run api:generate`를 실행한다.
