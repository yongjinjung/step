import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { useMenus } from './MenuContext'
import { loadWorkspaceTabs, saveWorkspaceTabs, type WorkspaceTab } from './workspaceTabsStorage'

export type { WorkspaceTab }

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
  /** 상세 화면이 탭에 대상 이름을 알려준다. 화면 안에서는 {@link useTabTitle}을 쓴다 */
  setTabTitle: (key: string, title: string) => void
}

const WorkspaceTabsContext = createContext<WorkspaceTabsValue | null>(null)

/**
 * 화면 탭(포틀릿) 상태.
 *
 * 주소가 바뀌면 그 화면의 탭을 열고, 이미 열려 있으면 그 탭의 마지막 주소만 갱신한다.
 * 그래서 메뉴 클릭, 화면 안의 링크(`/tables/3`), 뒤로 가기 모두 같은 규칙으로 탭이 관리된다.
 * 탭에 보일 이름은 저장하지 않고 그릴 때 메뉴에서 찾는다({@code WorkspaceTabs}).
 */
export function WorkspaceTabsProvider({ children }: { children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { homePath } = useMenus()
  // 탭은 사용자별로 저장한다. 로그아웃하면 AuthContext가 저장 내용을 지우므로 다른 사용자로 로그인하면 탭이 비어 있다.
  const username = user?.username ?? ''
  const [tabs, setTabs] = useState<WorkspaceTab[]>(() => loadWorkspaceTabs(username))

  const activeKey = location.pathname
  const activePath = location.pathname + location.search
  /** 탭을 모두 닫았을 때 돌아갈 화면. 메뉴를 아직 못 불러왔으면 첫 화면으로 보낸다. */
  const fallbackPath = homePath ?? '/'

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
      const opened = [...previous, { key: activeKey, path: activePath }]
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
        navigate(neighbor?.path ?? fallbackPath)
      }
    },
    [tabs, activeKey, navigate, fallbackPath],
  )

  const closeOthers = useCallback(
    (key: string) => {
      const kept = tabs.filter((tab) => tab.key === key)
      setTabs(kept)
      if (key !== activeKey) {
        navigate(kept[0]?.path ?? fallbackPath)
      }
    },
    [tabs, activeKey, navigate, fallbackPath],
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
    setTabs(homePath ? [{ key: homePath, path: homePath }] : [])
    navigate(fallbackPath)
  }, [navigate, homePath, fallbackPath])

  const setTabTitle = useCallback((key: string, title: string) => {
    setTabs((previous) =>
      previous.some((tab) => tab.key === key && tab.title !== title)
        ? previous.map((tab) => (tab.key === key ? { ...tab, title } : tab))
        : previous,
    )
  }, [])

  const value = useMemo<WorkspaceTabsValue>(
    () => ({ tabs, activeKey, close, closeOthers, closeRight, closeAll, setTabTitle }),
    [tabs, activeKey, close, closeOthers, closeRight, closeAll, setTabTitle],
  )

  return <WorkspaceTabsContext value={value}>{children}</WorkspaceTabsContext>
}

export function useWorkspaceTabs(): WorkspaceTabsValue {
  const value = useContext(WorkspaceTabsContext)
  if (!value) {
    throw new Error('useWorkspaceTabs는 WorkspaceTabsProvider 안에서만 쓸 수 있습니다.')
  }
  return value
}

/**
 * 탭에 대상 이름을 알려준다 (상세 화면에서 "메뉴명 · 대상 이름"으로 보이게).
 *
 * 화면마다 자기 탭의 주소를 `useLocation()`으로 얻는다. 탭 내용은 `<Routes location={탭 주소}>`로
 * 그려지므로, 보고 있지 않은 탭에서도 자기 주소가 나온다.
 *
 * @param title 대상 이름. 아직 불러오는 중이면 undefined를 넘겨 메뉴명만 보여준다
 */
export function useTabTitle(title: string | undefined | null) {
  const { pathname } = useLocation()
  const { setTabTitle } = useWorkspaceTabs()

  useEffect(() => {
    if (title) {
      setTabTitle(pathname, title)
    }
  }, [title, pathname, setTabTitle])
}
