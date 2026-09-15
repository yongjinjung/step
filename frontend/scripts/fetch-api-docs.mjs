// 백엔드의 OpenAPI 문서를 받아 openapi/api-docs.json으로 저장한다.
// 이 파일로 `npm run api:types`가 src/api/schema.d.ts(요청/응답 타입)를 생성한다.
// 저장한 문서를 커밋해 두면 API가 바뀔 때 diff로 변경 내용을 확인할 수 있고, 백엔드 없이도 타입을 다시 만들 수 있다.
//
// 기본 주소는 http://localhost:8080/v3/api-docs. 다른 주소는 환경 변수로 지정한다.
//   Git Bash:   API_DOCS_URL=http://localhost:8081/v3/api-docs npm run api:fetch
//   PowerShell: $env:API_DOCS_URL="http://localhost:8081/v3/api-docs"; npm run api:fetch
import { mkdir, writeFile } from 'node:fs/promises'

const url = process.env.API_DOCS_URL ?? 'http://localhost:8080/v3/api-docs'

let response
try {
  response = await fetch(url)
} catch (error) {
  console.error(`API 문서를 가져오지 못했습니다: ${url}\n백엔드가 실행 중인지 확인하세요. (${error.message})`)
  process.exit(1)
}
if (!response.ok) {
  console.error(`API 문서 요청 실패: ${response.status} ${url}`)
  process.exit(1)
}

const spec = await response.json()
// servers에는 문서를 받은 서버 주소가 들어가 실행 포트마다 달라지므로, 불필요한 diff가 생기지 않게 지운다.
delete spec.servers

const outDir = new URL('../openapi/', import.meta.url)
await mkdir(outDir, { recursive: true })
await writeFile(new URL('api-docs.json', outDir), `${JSON.stringify(spec, null, 2)}\n`, 'utf8')
console.log(`저장: openapi/api-docs.json (경로 ${Object.keys(spec.paths ?? {}).length}개, 출처 ${url})`)
