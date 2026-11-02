import { LogoutOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Dropdown, Layout, Menu, Skeleton, Tag, type MenuProps } from 'antd'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { useMenus } from './MenuContext'
import { menuIcon } from './menuIcons'
import { WorkspaceTabs } from './WorkspaceTabs'
import { useWorkspaceTabs, WorkspaceTabsProvider } from './WorkspaceTabsContext'

/**
 * 로그인 후 공통 레이아웃: 위쪽(TOP) 상위 메뉴 + 왼쪽(LEFT) 하위 메뉴 + 화면 탭(포틀릿).
 *
 * 메뉴는 DB에서 관리한다(`/api/menus`). 위쪽에서 상위 메뉴를 고르면 왼쪽에 그 하위 메뉴가 나오고,
 * 하위 메뉴를 누르면 화면이 탭으로 열린다.
 */
export function AppLayout() {
  return (
    <WorkspaceTabsProvider>
      <AppLayoutFrame />
    </WorkspaceTabsProvider>
  )
}

/** 탭 상태(WorkspaceTabsProvider)를 쓰기 위해 provider 안쪽에 두는 실제 화면 틀. */
function AppLayoutFrame() {
  const { user, logout } = useAuth()
  const { topMenus, loading, failed, findByPath } = useMenus()
  const { tabs } = useWorkspaceTabs()
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)

  // 지금 보고 있는 화면이 속한 상위 메뉴를 고른다. 메뉴에 없는 화면(내 정보 등)이면 첫 상위 메뉴를 고른다.
  const match = findByPath(location.pathname)
  const activeTop = match?.top ?? topMenus[0]

  /** 이미 열려 있는 화면이면 그 탭이 기억하는 주소(검색 조건 포함)로 돌아간다. */
  const openPath = (path: string) => navigate(tabs.find((tab) => tab.key === path)?.path ?? path)

  const topItems: MenuProps['items'] = topMenus.map((top) => ({
    key: String(top.id),
    icon: menuIcon(top.icon),
    label: top.name,
  }))

  const leftItems: MenuProps['items'] = (activeTop?.children ?? [])
    .filter((menu) => menu.path)
    .map((menu) => ({ key: menu.path as string, icon: menuIcon(menu.icon), label: menu.name }))

  /** 상위 메뉴를 누르면 그 묶음의 첫 화면을 연다. */
  const openTopMenu = (topId: string) => {
    const firstPath = topMenus.find((top) => String(top.id) === topId)?.children?.find((menu) => menu.path)?.path
    if (firstPath) {
      openPath(firstPath)
    }
  }

  const userMenu: MenuProps = {
    items: [
      { key: 'me', icon: <UserOutlined />, label: '내 정보' },
      { type: 'divider' },
      { key: 'logout', icon: <LogoutOutlined />, label: '로그아웃', danger: true },
    ],
    onClick: ({ key }) => (key === 'logout' ? logout() : navigate('/me')),
  }

  return (
    <Layout className="app-layout">
      <Layout.Header className="app-header">
        <div className="app-logo">
          <span className="app-logo-name">STEP</span>
          <span className="app-logo-tagline">Start · Transform · Explore · Progress</span>
        </div>
        <Menu
          theme="dark"
          mode="horizontal"
          className="app-top-menu"
          selectedKeys={activeTop?.id !== undefined ? [String(activeTop.id)] : []}
          items={topItems}
          onClick={({ key }) => openTopMenu(key)}
        />
        <div className="app-header-user">
          {/* 토큰에 담긴 역할 코드. 실제 기능 권한은 서버가 역할의 메뉴 권한으로 검사한다. */}
          <Tag color={user?.isAdmin ? 'volcano' : 'blue'}>{user?.roles.join(', ') || '역할 없음'}</Tag>
          <Dropdown menu={userMenu} placement="bottomRight">
            <Button type="text" className="app-user-button" icon={<UserOutlined />}>
              {user?.username}
            </Button>
          </Dropdown>
        </div>
      </Layout.Header>

      <Layout>
        <Layout.Sider
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          breakpoint="lg"
          width={220}
          theme="light"
          className="app-sider"
        >
          {loading ? (
            <Skeleton active className="app-sider-loading" paragraph={{ rows: 4 }} title={false} />
          ) : (
            <Menu
              mode="inline"
              selectedKeys={match?.menu.path ? [match.menu.path] : []}
              items={leftItems}
              onClick={({ key }) => openPath(key)}
            />
          )}
        </Layout.Sider>
        <Layout.Content className="app-content">
          {failed && (
            <Alert
              type="error"
              showIcon
              className="workspace-pane"
              title="메뉴를 불러오지 못했습니다. 새로고침해도 같으면 관리자에게 알려주세요."
            />
          )}
          <WorkspaceTabs />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
