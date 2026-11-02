/**
 * Vite 샘플 화면의 동적 import 데모가 쓰는 모듈.
 *
 * 이 파일은 어디서도 정적으로 import 하지 않는다. 그래야 빌드할 때 별도 청크로 떨어지고,
 * 화면에서 버튼을 누른 순간에 내려받는 것을 네트워크 탭에서 확인할 수 있다.
 * (한 곳이라도 정적으로 import 하면 Rollup이 본 번들에 합쳐 버린다 — 빌드 로그에 INEFFECTIVE_DYNAMIC_IMPORT 경고가 뜬다.)
 */

/** 데모용. 모듈을 언제 받아 왔는지 알 수 있게 실행 시각을 함께 돌려준다. */
export function describeChunk(): string {
  const now = new Date().toLocaleTimeString('ko-KR')
  return `별도 청크에서 실행됨 (${now})`
}
