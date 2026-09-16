/**
 * 열어 둔 화면 탭 저장소.
 *
 * sessionStorage에 두므로 새로고침에는 살아남고 브라우저 탭을 닫으면 사라진다.
 * 저장 키에 아이디를 넣고 로그아웃할 때 지운다. 같은 브라우저 탭에서 로그아웃한 뒤
 * 다른 사용자가 로그인했을 때 이전 사용자가 보던 화면이 남아 있으면 안 되기 때문이다.
 *
 * 탭 상태(`WorkspaceTabsContext`)와 따로 둔 이유: 로그인 상태(`AuthContext`)가 로그아웃할 때
 * 이 파일만 부르면 되고, 탭 상태 파일을 직접 참조하면 import 순환이 생긴다.
 */

/**
 * 열어 둔 화면 탭 하나.
 *
 * 탭 키는 경로(pathname)라서 같은 화면은 탭이 하나만 열린다. 상세 화면은 ID가 경로에 있어
 * (`/tables/3`, `/tables/5`) 대상마다 다른 탭이 된다.
 */
export interface WorkspaceTab {
  /** 탭 키이자 화면 경로 (pathname) */
  key: string
  /** 이 탭에서 마지막으로 보던 주소 (검색 조건·페이지 포함) */
  path: string
  /**
   * 상세 화면이 알려 준 대상 이름 (예: TB_ORDER). 메뉴명은 저장하지 않고 그릴 때 메뉴에서 찾는다.
   * 메뉴는 서버에서 늦게 도착하고 관리 화면에서 이름이 바뀔 수도 있어, 저장해 두면 옛 이름이 남는다.
   */
  title?: string
}

/** 사용자별 저장 키 접두어. 로그아웃 때 이 접두어로 시작하는 값을 모두 지운다. */
const KEY_PREFIX = 'workspace-tabs:'

function storageKey(username: string): string {
  return KEY_PREFIX + username
}

/** 저장된 탭 목록. 값이 없거나 형식이 깨졌으면 빈 목록. */
export function loadWorkspaceTabs(username: string): WorkspaceTab[] {
  // 저장 값을 못 읽어도(사생활 보호 모드 등) 화면은 그대로 동작해야 하므로 실패는 무시한다.
  try {
    const saved: unknown = JSON.parse(sessionStorage.getItem(storageKey(username)) ?? '[]')
    if (!Array.isArray(saved)) {
      return []
    }
    return saved.filter(
      (tab): tab is WorkspaceTab =>
        typeof tab === 'object' && tab !== null && typeof (tab as WorkspaceTab).key === 'string' &&
        typeof (tab as WorkspaceTab).path === 'string',
    )
  } catch {
    return []
  }
}

export function saveWorkspaceTabs(username: string, tabs: WorkspaceTab[]): void {
  try {
    sessionStorage.setItem(storageKey(username), JSON.stringify(tabs))
  } catch {
    // 저장하지 못해도 이번 세션 동안은 메모리 상태로 동작한다.
  }
}

/**
 * 저장된 탭을 모두 지운다. 로그아웃(직접 로그아웃, 토큰 만료, 401 응답)에서 부른다.
 * 아이디를 몰라도 되도록 접두어로 지우며, 사용자별 키를 쓰기 전에 저장된 값('workspace-tabs')도 함께 지운다.
 */
export function clearWorkspaceTabs(): void {
  try {
    sessionStorage.removeItem('workspace-tabs')
    Object.keys(sessionStorage)
      .filter((key) => key.startsWith(KEY_PREFIX))
      .forEach((key) => sessionStorage.removeItem(key))
  } catch {
    // 지우지 못해도 로그아웃 자체는 진행한다.
  }
}
