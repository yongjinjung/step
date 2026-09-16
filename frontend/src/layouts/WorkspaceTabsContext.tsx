import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { NAV_ITEMS } from './navigation'
import { loadWorkspaceTabs, saveWorkspaceTabs, type WorkspaceTab } from './workspaceTabsStorage'

export type { WorkspaceTab }

/** 첫 화면이자 탭을 모두 닫았을 때 돌아갈 경로 (메뉴의 첫 항목) */
const HOME_PATH = NAV_ITEMS[0].path
/** 동시에 열어 두는 탭 수. 넘으면 현재 탭이 아닌 가장 오래된 탭부터 닫는다 (모두 렌더링하므로 무한정 늘리지 않는다) */
const MAX_TABS = 12

interface WorkspaceTabsValue {
  tabs: WorkspaceTab[]
  /** 현재 보고 있는 탭 키 (= 주소의 pathname) */
  activeKey: string
  close: (key: string) => void
  closeOthers: (key: string) => void
  closeRight: (key: string) => void
  closeAll: () => void
  /** 탭 이름을 바꾼다. 화면 안에서는 {@link useTabTitle}을 쓴다 */
  setTabLabel: (key: string, label: string) => void
}

const WorkspaceTabsContext = createContext<WorkspaceTabsValue | null>(null)

/** 메뉴 정의에서 기본 탭 이름을 찾는다. 상세 화면(`/tables/3`)은 상위 메뉴 이름으로 시작한다. */
function defaultLabel(pathname: string): string {
  if (pathname === '/me') {
    return '내 정보'
  }
  const item = NAV_ITEMS.find((nav) => pathname === nav.path || pathname.startsWith(`${nav.path}/`))
  return item?.label ?? '화면'
}

/**
 * 저장된 탭을 복원한다 (새로고침 대비).
 * 관리자 전용 화면 탭은 일반 사용자에게 열어 주지 않는다. 로그아웃 때 저장 내용을 지우므로 보통은 걸리지 않지만,
 * 저장 값이 남아 있는 경우를 대비한 방어다 (권한 검사 자체는 화면과 서버가 한다).
 */
function restoreTabs(username: string, isAdmin: boolean): WorkspaceTab[] {
  return loadWorkspaceTabs(username).filter((tab) => {
    const item = NAV_ITEMS.find((nav) => tab.key === nav.path || tab.key.startsWith(`${nav.path}/`))
    return !item?.adminOnly || isAdmin
  })
}

/**
 * 화면 탭(포틀릿) 상태.
 *
 * 주소가 바뀌면 그 화면의 탭을 열고, 이미 열려 있으면 그 탭의 마지막 주소만 갱신한다.
 * 그래서 메뉴 클릭, 화면 안의 링크(`/tables/3`), 뒤로 가기 모두 같은 규칙으로 탭이 관리된다.
 */
export function WorkspaceTabsProvider({ children }: { children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuth()
  // 탭은 사용자별로 저장한다. 로그아웃하면 AuthContext가 저장 내용을 지우므로 다른 사용자로 로그인하면 탭이 비어 있다.
  const username = user?.username ?? ''
  const [tabs, setTabs] = useState<WorkspaceTab[]>(() => restoreTabs(username, user?.isAdmin ?? false))

  const activeKey = location.pathname
  const activePath = location.pathname + location.search

  useEffect(() => {
    setTabs((previous) => {
      const index = previous.findIndex((tab) => tab.key === activeKey)
      if (index >= 0) {
        // 이미 열린 탭이면 검색 조건만 기억해 둔다 (같은 값이면 상태를 바꾸지 않아 다시 그리지 않는다).
        if (previous[index].path === activePath) {
          return previous
        }
        const updated = [...previous]
        updated[index] = { ...updated[index], path: activePath }
        return updated
      }
      const opened = [...previous, { key: activeKey, label: defaultLabel(activeKey), path: activePath }]
      while (opened.length > MAX_TABS) {
        const oldest = opened.findIndex((tab) => tab.key !== activeKey)
        if (oldest < 0) {
          break
        }
        opened.splice(oldest, 1)
      }
      return opened
    })
  }, [activeKey, activePath])

  useEffect(() => {
    // 로그아웃 직후(아이디 없음)에는 저장하지 않는다. 방금 지운 내용이 되살아나지 않게 한다.
    if (username) {
      saveWorkspaceTabs(username, tabs)
    }
  }, [username, tabs])

  /** 탭을 닫는다. 보고 있던 탭을 닫으면 왼쪽 탭으로, 없으면 첫 탭으로 옮긴다. */
  const close = useCallback(
    (key: string) => {
      const index = tabs.findIndex((tab) => tab.key === key)
      if (index < 0) {
        return
      }
      const remaining = tabs.filter((tab) => tab.key !== key)
      setTabs(remaining)
      if (key === activeKey) {
        const neighbor = remaining[index - 1] ?? remaining[0]
        navigate(neighbor?.path ?? HOME_PATH)
      }
    },
    [tabs, activeKey, navigate],
  )

  const closeOthers = useCallback(
    (key: string) => {
      const kept = tabs.filter((tab) => tab.key === key)
      setTabs(kept)
      if (key !== activeKey) {
        navigate(kept[0]?.path ?? HOME_PATH)
      }
    },
    [tabs, activeKey, navigate],
  )

  const closeRight = useCallback(
    (key: string) => {
      const index = tabs.findIndex((tab) => tab.key === key)
      if (index < 0) {
        return
      }
      const kept = tabs.slice(0, index + 1)
      setTabs(kept)
      // 오른쪽 탭을 보고 있었다면 기준 탭으로 옮긴다.
      if (!kept.some((tab) => tab.key === activeKey)) {
        navigate(kept[index].path)
      }
    },
    [tabs, activeKey, navigate],
  )

  /** 모두 닫고 첫 화면만 남긴다. 탭이 하나도 없으면 아무 화면도 그려지지 않으므로 직접 첫 탭을 만든다. */
  const closeAll = useCallback(() => {
    setTabs([{ key: HOME_PATH, label: defaultLabel(HOME_PATH), path: HOME_PATH }])
    navigate(HOME_PATH)
  }, [navigate])

  const setTabLabel = useCallback((key: string, label: string) => {
    setTabs((previous) =>
      previous.some((tab) => tab.key === key && tab.label !== label)
        ? previous.map((tab) => (tab.key === key ? { ...tab, label } : tab))
        : previous,
    )
  }, [])

  const value = useMemo<WorkspaceTabsValue>(
    () => ({ tabs, activeKey, close, closeOthers, closeRight, closeAll, setTabLabel }),
    [tabs, activeKey, close, closeOthers, closeRight, closeAll, setTabLabel],
  )

  return <WorkspaceTabsContext.Provider value={value}>{children}</WorkspaceTabsContext.Provider>
}

export function useWorkspaceTabs(): WorkspaceTabsValue {
  const value = useContext(WorkspaceTabsContext)
  if (!value) {
    throw new Error('useWorkspaceTabs는 WorkspaceTabsProvider 안에서만 쓸 수 있습니다.')
  }
  return value
}

/**
 * 탭 이름을 화면 내용으로 바꾼다 (상세 화면에서 대상 이름을 보여줄 때).
 *
 * 화면마다 자기 탭의 주소를 `useLocation()`으로 얻는다. 탭 내용은 `<Routes location={탭 주소}>`로
 * 그려지므로, 보고 있지 않은 탭에서도 자기 주소가 나온다.
 *
 * @param label 탭에 보여줄 이름. 아직 불러오는 중이면 undefined를 넘겨 기본 이름을 유지한다
 */
export function useTabTitle(label: string | undefined | null) {
  const { pathname } = useLocation()
  const { setTabLabel } = useWorkspaceTabs()

  useEffect(() => {
    if (label) {
      setTabLabel(pathname, label)
    }
  }, [label, pathname, setTabLabel])
}
