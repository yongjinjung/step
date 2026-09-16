import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { $api } from '@/api/client'
import type { components } from '@/api/schema'

export type MenuResponse = components['schemas']['MenuResponse']

/** 화면 경로로 찾은 메뉴와 그 상위 메뉴. */
export interface MenuMatch {
  /** 하위(LEFT) 메뉴 */
  menu: MenuResponse
  /** 그 메뉴가 속한 상위(TOP) 메뉴 */
  top: MenuResponse
}

interface MenuContextValue {
  /** 상위(TOP) 메뉴 목록. 각 항목의 children이 왼쪽에 보일 하위 메뉴다 */
  topMenus: MenuResponse[]
  /** 아직 메뉴를 불러오는 중인지 (처음 로딩) */
  loading: boolean
  /** 메뉴를 불러오지 못했는지 */
  failed: boolean
  /** 로그인 후 처음 보여줄 화면 경로. 메뉴가 없으면 undefined */
  homePath: string | undefined
  /** 주소에 해당하는 메뉴를 찾는다. 상세 화면(`/tables/3`)은 상위 경로(`/tables`)의 메뉴로 찾는다 */
  findByPath: (pathname: string) => MenuMatch | undefined
}

const MenuContext = createContext<MenuContextValue | null>(null)

/**
 * 화면 메뉴 제공자.
 *
 * 메뉴는 DB에서 관리하므로(`/api/menus`) 화면 구성이 서버에서 온다. 응답에는 로그인한 사용자가
 * 볼 수 있는 메뉴만 담겨 있다(사용 중 + 역할에 맞는 것). 메뉴 관리 화면에서 메뉴를 바꾸면
 * 이 조회를 무효화해 메뉴 막대가 바로 바뀐다.
 */
export function MenuProvider({ children }: { children: ReactNode }) {
  // 메뉴는 자주 바뀌지 않으므로 창을 다시 볼 때마다 다시 부르지 않는다(기본 staleTime 30초).
  const menuQuery = $api.useQuery('get', '/api/menus')
  const topMenus = useMemo(() => menuQuery.data ?? [], [menuQuery.data])

  const value = useMemo<MenuContextValue>(() => {
    /** 경로 → {하위 메뉴, 상위 메뉴}. 한 번 만들어 두고 주소가 바뀔 때마다 다시 만들지 않는다. */
    const byPath = new Map<string, MenuMatch>()
    for (const top of topMenus) {
      for (const menu of top.children ?? []) {
        if (menu.path) {
          byPath.set(menu.path, { menu, top })
        }
      }
    }

    const findByPath = (pathname: string): MenuMatch | undefined => {
      const exact = byPath.get(pathname)
      if (exact) {
        return exact
      }
      // 상세 화면(/tables/3)은 메뉴 경로(/tables)로 찾는다. 더 긴 경로가 우선이다.
      let matched: MenuMatch | undefined
      for (const [path, match] of byPath) {
        if (pathname.startsWith(`${path}/`) && (!matched || path.length > (matched.menu.path?.length ?? 0))) {
          matched = match
        }
      }
      return matched
    }

    return {
      topMenus,
      loading: menuQuery.isPending,
      failed: menuQuery.isError,
      homePath: topMenus.flatMap((top) => top.children ?? []).find((menu) => menu.path)?.path,
      findByPath,
    }
  }, [topMenus, menuQuery.isPending, menuQuery.isError])

  return <MenuContext value={value}>{children}</MenuContext>
}

export function useMenus(): MenuContextValue {
  const context = useContext(MenuContext)
  if (!context) {
    throw new Error('useMenus는 MenuProvider 안에서만 쓸 수 있습니다.')
  }
  return context
}
