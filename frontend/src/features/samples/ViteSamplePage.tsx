import { Alert, Button, Space, Table, Tag, Typography } from 'antd'
import { useState } from 'react'
import { PageHeader } from '@/components/PageHeader'
import { SampleSection } from './SampleSection'

/**
 * Vite 샘플 화면.
 *
 * 개발 서버·빌드·환경 변수·프록시처럼 "화면 코드 바깥"에서 알아야 할 것들을 모았다.
 * 설정 파일은 `vite.config.ts` 하나이고, 이 프로젝트에 맞춘 설명을 함께 둔다.
 */
export function ViteSamplePage() {
  return (
    <>
      <PageHeader title="Vite 샘플" subTitle="개발 서버 · 프록시 · 환경 변수 · 빌드" />

      <Space orientation="vertical" size="large" className="full-width">
        <Alert
          type="info"
          showIcon
          title="Vite 8"
          description={
            <>
              Vite는 개발할 때 <b>번들링을 하지 않고</b> 브라우저의 ES 모듈을 그대로 쓰기 때문에 서버가 바로 뜨고 저장하면
              해당 모듈만 바꿔 치운다(HMR). 빌드할 때만 Rollup으로 묶는다.
            </>
          }
        />

        <SampleSection
          title="1. 명령어"
          summary={
            <>
              모두 <code>frontend/</code>에서 실행한다. 백엔드(<code>./gradlew bootRun</code>)를 먼저 띄워야 API가 붙는다.
            </>
          }
          points={[
            <>
              <code>npm run dev</code>는 타입 검사를 하지 않는다. 타입 오류는 에디터나 <code>npm run typecheck</code>로 본다.
            </>,
            <>
              <code>npm run build</code>는 <code>tsc -b</code>(타입 검사) 후 <code>vite build</code>다. 타입 오류가 있으면
              빌드가 멈춘다.
            </>,
            <>
              백엔드 API를 바꿨으면 <code>npm run api:generate</code>로 타입을 다시 만든다(백엔드가 떠 있어야 한다).
            </>,
          ]}
          demo={
            <Table
              size="small"
              pagination={false}
              rowKey="command"
              dataSource={COMMANDS}
              columns={[
                { title: '명령', dataIndex: 'command', width: 220, render: (value: string) => <code>{value}</code> },
                { title: '하는 일', dataIndex: 'description' },
              ]}
            />
          }
          code={`cd frontend
npm install            # 의존성 설치 (--save-exact로 버전 고정)
npm run dev            # http://localhost:5173, /api는 백엔드로 프록시
npm run typecheck      # 타입만 검사
npm run build          # tsc -b + vite build → dist/
npm run preview        # 빌드 결과를 미리 서빙 (프록시 없음)
npm run api:generate   # 백엔드 OpenAPI 문서 받기 + schema.d.ts 생성`}
        />

        <SampleSection
          title="2. vite.config.ts — 이 프로젝트의 설정 전부"
          summary={
            <>
              플러그인(React), 경로 별칭(<code>@</code>), 개발 서버 프록시 세 가지뿐이다. 설정을 늘리기 전에 이 세 가지로
              해결되는지 먼저 본다.
            </>
          }
          points={[
            <>
              <code>defineConfig(({'{ mode }'}) =&gt; ...)</code> 형태로 쓰면 모드별로 다른 값을 줄 수 있다.
            </>,
            <>
              별칭을 바꾸면 <code>tsconfig.app.json</code>의 <code>paths</code>도 같이 고쳐야 한다. 둘 중 하나만 고치면 빌드나
              에디터 중 한쪽이 깨진다.
            </>,
            <>
              <code>loadEnv</code>는 <code>VITE_</code> 접두어가 없는 변수까지 읽는다. 설정 파일은 Node에서 도니까 안전하다.
            </>,
          ]}
          code={`import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    resolve: {
      // import '@/api/client' 처럼 src 기준 경로를 쓴다 (tsconfig.app.json의 paths와 같게 유지)
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true },
      },
    },
  }
})`}
          codeTitle="frontend/vite.config.ts"
        />

        <SampleSection
          title="3. 프록시 — CORS 없이 백엔드 붙이기"
          summary={
            <>
              브라우저는 <code>http://localhost:5173/api/words</code>로 부르고, 개발 서버가 그 요청을 백엔드(8080)로 넘긴다.
              브라우저 입장에서는 같은 출처라 <b>백엔드에 CORS 설정이 필요 없다</b>.
            </>
          }
          points={[
            <>
              그래서 화면 코드의 API 주소는 항상 <code>/api/...</code>로 시작하는 상대 경로다. 절대 주소를 쓰면 배포 때 깨진다.
            </>,
            <>
              백엔드를 8081로 띄웠다면 <code>frontend/.env.local</code>에{' '}
              <code>VITE_API_PROXY_TARGET=http://localhost:8081</code>을 적고 개발 서버를 다시 띄운다.
            </>,
            <>
              <code>npm run preview</code>와 실제 배포에는 프록시가 없다. 그때는 웹 서버(nginx 등)나 백엔드가{' '}
              <code>/api</code>를 처리한다.
            </>,
          ]}
          code={`# frontend/.env.local (git에 올리지 않는다)
VITE_API_PROXY_TARGET=http://localhost:8081

# API 문서를 다른 주소에서 받을 때
API_DOCS_URL=http://localhost:8081/v3/api-docs npm run api:fetch`}
          codeTitle="frontend/.env.local"
        />

        <SampleSection
          title="4. 환경 변수 — VITE_ 접두어만 브라우저로 간다"
          summary={
            <>
              <code>.env</code> 파일의 값 중 <code>VITE_</code>로 시작하는 것만 화면 코드에서 읽을 수 있다. 빌드할 때 값이
              코드에 <b>그대로 박히므로</b> 비밀번호·키를 넣으면 안 된다.
            </>
          }
          points={[
            <>
              읽는 법은 <code>import.meta.env.VITE_XXX</code>. <code>process.env</code>는 브라우저에 없다.
            </>,
            <>
              파일 우선순위: <code>.env</code> &lt; <code>.env.[mode]</code> &lt; <code>.env.local</code> &lt;{' '}
              <code>.env.[mode].local</code>. <code>*.local</code>은 git에 올리지 않는다.
            </>,
            <>
              <code>DEV</code>/<code>PROD</code>로 개발용 코드를 갈라 두면 빌드에서 통째로 빠진다(트리 셰이킹).
            </>,
          ]}
          demo={<EnvDemo />}
          code={`if (import.meta.env.DEV) {
  console.log('개발 모드에서만 찍힌다') // 빌드 결과에는 포함되지 않는다
}

const apiDocs = import.meta.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080'

// 타입을 붙이려면 src/vite-env.d.ts 에
interface ImportMetaEnv {
  readonly VITE_API_PROXY_TARGET?: string
}`}
        />

        <SampleSection
          title="5. 경로 별칭과 import 정리"
          summary={
            <>
              <code>../../../</code>를 쓰지 않는다. <code>@/</code>는 <code>src/</code>를 가리킨다.
            </>
          }
          points={[
            <>
              같은 기능 폴더 안(<code>./wordFields</code>)은 상대 경로, 다른 폴더는 <code>@/</code>를 쓴다.
            </>,
            <>
              타입만 가져올 때는 <code>import type</code>(<code>verbatimModuleSyntax</code>).
            </>,
          ]}
          code={`import { $api } from '@/api/client'            // src/api/client.ts
import { PageHeader } from '@/components/PageHeader'
import type { components } from '@/api/schema'  // 타입만 가져올 때
import { WORD_FIELD_LABELS } from './wordFields' // 같은 폴더는 상대 경로`}
        />

        <SampleSection
          title="6. 정적 자산 — import 하면 URL이 나온다"
          summary={
            <>
              이미지·폰트는 <code>src</code> 안에 두고 import 한다. 빌드하면 해시가 붙은 파일명으로 바뀌어 캐시 문제가 없다.
              해시가 붙으면 안 되는 파일만 <code>public/</code>에 둔다.
            </>
          }
          points={[
            <>
              <code>public/</code>의 파일은 가공 없이 그대로 복사되고 <code>/파일명</code>으로 접근한다(예:{' '}
              <code>/favicon.ico</code>).
            </>,
            <>
              접미사로 형태를 고를 수 있다: <code>?url</code>(주소), <code>?raw</code>(문자열), <code>?inline</code>(data
              URL).
            </>,
            <>
              CSS는 그냥 import 한다. 이 프로젝트는 <code>src/main.tsx</code>에서 <code>./index.css</code> 하나만 불러온다.
            </>,
          ]}
          code={`import logoUrl from '@/assets/logo.svg'          // 빌드 후 해시 붙은 URL 문자열
import sqlText from '@/assets/sample.sql?raw'    // 파일 내용을 문자열로
import './index.css'                             // CSS는 그냥 import

<img src={logoUrl} alt="로고" />
<img src="/favicon.ico" alt="" />                {/* public/ 의 파일은 절대 경로 */}`}
        />

        <SampleSection
          title="7. 동적 import — 필요할 때 받아 오기"
          summary={
            <>
              <code>import(...)</code>로 부르면 그 모듈이 별도 파일로 쪼개져, 실제로 쓸 때 내려받는다. 무거운 화면이나
              라이브러리(차트·엑셀)에 쓴다.
            </>
          }
          points={[
            <>
              화면 단위로 나눌 때는 React의 <code>lazy</code> + <code>Suspense</code>와 같이 쓴다.
            </>,
            <>
              한 군데라도 정적으로 <code>import</code> 하면 본 번들에 합쳐져 분할이 무의미해진다. 빌드 로그의{' '}
              <code>INEFFECTIVE_DYNAMIC_IMPORT</code> 경고와 청크 목록으로 확인한다.
            </>,
            <>
              아래 데모가 부르는 <code>./dynamicSample.ts</code>는 다른 곳에서 import 하지 않으므로 실제로 별도 청크가 된다
              (브라우저 네트워크 탭에서 확인).
            </>,
          ]}
          demo={<DynamicImportDemo />}
          code={`// 필요할 때 받아 오기
const openExcel = async () => {
  const { downloadFile } = await import('@/api/download')
  await downloadFile('/api/excel/words', query)
}

// 화면 단위 분할
const HistoryListPage = lazy(() => import('@/features/histories/HistoryListPage'))

<Suspense fallback={<Skeleton active />}>
  <HistoryListPage />
</Suspense>`}
        />

        <SampleSection
          title="8. HMR — 저장하면 바뀌는 것, 새로고침되는 것"
          summary={
            <>
              컴포넌트 파일을 저장하면 <b>상태를 유지한 채</b> 그 컴포넌트만 바뀐다. 파일이 컴포넌트 외의 값을 내보내면
              전체 새로고침으로 떨어질 수 있다.
            </>
          }
          points={[
            <>
              그래서 상수·유틸은 <code>wordFields.ts</code>처럼 별도 파일로 뺀다(빠른 새로고침 유지 + 재사용).
            </>,
            <>
              <code>import.meta.hot</code>은 개발에서만 존재한다. 직접 다룰 일은 거의 없다.
            </>,
            <>
              화면이 이상하게 남아 있으면 브라우저 새로고침, 그래도 이상하면 개발 서버를 다시 띄운다(의존성 캐시는{' '}
              <code>node_modules/.vite</code>).
            </>,
          ]}
          code={`// 컴포넌트 파일은 컴포넌트만 내보낸다 (Fast Refresh 유지)
export function WordListPage() { /* ... */ }

// 상수·타입·헬퍼는 별도 파일로
// src/features/words/wordFields.ts
export const WORD_FIELD_LABELS = { name: '단어명', abbreviation: '영문약어' } as const`}
        />

        <SampleSection
          title="9. 빌드와 배포"
          summary={
            <>
              <code>npm run build</code>의 결과는 <code>dist/</code>다. <code>index.html</code>과 해시가 붙은 js/css가
              들어간다. 정적 파일이므로 아무 웹 서버에나 올릴 수 있다.
            </>
          }
          points={[
            <>
              배포 후 API가 404면 프록시가 없어서다. 웹 서버에서 <code>/api</code>를 백엔드로 넘기거나, 백엔드가{' '}
              <code>dist</code>를 같이 서빙하게 한다.
            </>,
            <>
              SPA라 새로고침 시 404가 나면 모든 경로를 <code>index.html</code>로 보내는 설정(fallback)이 필요하다.
            </>,
            <>
              <code>npm run preview</code>로 빌드 결과를 먼저 확인한다. 개발 서버에서만 되던 코드를 여기서 걸러 낸다.
            </>,
          ]}
          code={`npm run build      # dist/ 생성
npm run preview    # dist를 4173 포트로 서빙해 확인

dist/
├── index.html
└── assets/
    ├── index-a1b2c3d4.js    # 해시가 붙어 캐시가 안전하다
    └── index-e5f6a7b8.css`}
        />

        <Alert
          type="warning"
          showIcon
          title="버전 제약 (Node 22.16 기준)"
          description={
            <>
              orval은 Node 22.18+, react-router 8은 Node 22.22+가 필요해 쓰지 않았다. openapi-typescript가{' '}
              <code>typescript ^5</code>를 요구하므로 TypeScript는 5.x를 유지한다. 의존성은{' '}
              <code>npm i --save-exact</code>로 고정한다.
            </>
          }
        />
      </Space>
    </>
  )
}

/* ------------------------------------------------------------------ */
/* 데모 컴포넌트                                                        */
/* ------------------------------------------------------------------ */

interface CommandRow {
  command: string
  description: string
}

const COMMANDS: CommandRow[] = [
  { command: 'npm run dev', description: '개발 서버(5173). /api는 백엔드로 프록시. 저장하면 즉시 반영(HMR)' },
  { command: 'npm run typecheck', description: 'tsc -b 로 타입만 검사. 개발 서버는 타입을 검사하지 않는다' },
  { command: 'npm run build', description: '타입 검사 후 dist/ 생성' },
  { command: 'npm run preview', description: '빌드 결과를 그대로 서빙해 확인(프록시 없음)' },
  { command: 'npm run api:generate', description: '백엔드 OpenAPI 문서를 받아 src/api/schema.d.ts 재생성' },
]

/** 4번 데모: 현재 이 화면이 받은 환경 변수 값을 그대로 보여 준다 */
function EnvDemo() {
  const env = import.meta.env

  const rows = [
    { key: 'MODE', value: String(env.MODE), note: '개발은 development, 빌드는 production' },
    { key: 'DEV', value: String(env.DEV), note: '개발 서버에서만 true' },
    { key: 'PROD', value: String(env.PROD), note: '빌드 결과에서만 true' },
    { key: 'BASE_URL', value: String(env.BASE_URL), note: '앱이 서빙되는 기준 경로' },
    {
      key: 'VITE_API_PROXY_TARGET',
      value: env.VITE_API_PROXY_TARGET ?? '(설정 없음 → http://localhost:8080)',
      note: '.env.local 에서 바꾼다. VITE_ 접두어라 화면에서도 읽힌다',
    },
  ]

  return (
    <Table
      size="small"
      pagination={false}
      rowKey="key"
      dataSource={rows}
      columns={[
        { title: '변수', dataIndex: 'key', width: 220, render: (value: string) => <code>{value}</code> },
        { title: '현재 값', dataIndex: 'value', width: 260, render: (value: string) => <Tag color="blue">{value}</Tag> },
        { title: '설명', dataIndex: 'note' },
      ]}
    />
  )
}

/** 7번 데모: 버튼을 누른 뒤에야 모듈을 받아 온다 */
function DynamicImportDemo() {
  const [result, setResult] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const run = async () => {
    setLoading(true)
    try {
      // 이 import는 별도 청크가 되어, 버튼을 누르기 전에는 내려받지 않는다.
      const { describeChunk } = await import('./dynamicSample')
      setResult(describeChunk())
    } finally {
      setLoading(false)
    }
  }

  return (
    <Space wrap align="center">
      <Button type="primary" loading={loading} onClick={run}>
        모듈 받아서 실행
      </Button>
      <Typography.Text type="secondary">
        {result ?? '누르면 그때 ./dynamicSample.ts 청크를 내려받는다.'}
      </Typography.Text>
    </Space>
  )
}
