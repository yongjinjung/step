import { LogoutOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Dropdown, Layout, Menu, Tag, type MenuProps } from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { NAV_GROUPS, NAV_ITEMS } from './navigation'

/**
 * 로그인 후 공통 레이아웃: 왼쪽 메뉴 + 위쪽 사용자 영역 + 본문(Outlet).
 * 관리자 전용 메뉴는 ADMIN에게만 보인다.
 */
export function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)

  const menuItems: MenuProps['items'] = NAV_GROUPS.map((group) => ({
    type: 'group' as const,
    key: group.label,
    label: collapsed ? null : group.label,
    children: group.items
      .filter((item) => !item.adminOnly || user?.isAdmin)
      .map((item) => ({ key: item.path, icon: item.icon, label: item.label })),
  })).filter((group) => group.children.length > 0)

  // 상세 화면(/words/3 등)에서도 상위 메뉴가 선택돼 보이도록 경로 앞부분으로 찾는다.
  const selectedKey = NAV_ITEMS.find((item) => location.pathname.startsWith(item.path))?.path

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
      <Layout.Sider collapsible collapsed={collapsed} onCollapse={setCollapsed} breakpoint="lg" width={220}>
        <div className="app-logo">{collapsed ? '용어' : '데이터 표준 용어사전'}</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKey ? [selectedKey] : []}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header className="app-header">
          <Tag color={user?.isAdmin ? 'volcano' : 'blue'}>{user?.isAdmin ? '관리자' : '사용자'}</Tag>
          <Dropdown menu={userMenu} placement="bottomRight">
            <Button type="text" icon={<UserOutlined />}>
              {user?.username}
            </Button>
          </Dropdown>
        </Layout.Header>
        <Layout.Content className="app-content">
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
