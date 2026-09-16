import { MoreOutlined } from '@ant-design/icons'
import { Button, Dropdown, Space, Tabs, Typography, type MenuProps } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { NAV_ITEMS } from './navigation'
import { ScreenRoutes } from './screens'
import { useWorkspaceTabs, type WorkspaceTab } from './WorkspaceTabsContext'

/** 탭이 속한 메뉴. 상세 화면(`/tables/3`)은 상위 메뉴(`/tables`)로 찾는다. */
function menuOf(pathname: string) {
  return NAV_ITEMS.find((item) => pathname === item.path || pathname.startsWith(`${item.path}/`))
}

/**
 * 탭 이름. 메뉴 아이콘을 앞에 두고, 상세 탭은 "메뉴명 · 대상 이름"으로 보여준다
 * (같은 메뉴의 상세를 여러 개 열어도 어느 화면인지 알 수 있게).
 *
 * 대상 이름을 아직 받지 못했으면(화면이 {@code useTabTitle}을 부르기 전) 탭 이름이 메뉴명 그대로라
 * "테이블 정의 · 테이블 정의"가 되지 않도록 메뉴명만 보여준다.
 */
function TabLabel({ tab }: { tab: WorkspaceTab }) {
  const menu = menuOf(tab.key)
  const isDetail = menu !== undefined && tab.key !== menu.path && tab.label !== menu.label

  return (
    <Space size={6} className="workspace-tab-label">
      {menu?.icon}
      {isDetail ? (
        <span>
          <Typography.Text type="secondary">{menu.label}</Typography.Text> · {tab.label}
        </span>
      ) : (
        <span>{tab.label}</span>
      )}
    </Space>
  )
}

/**
 * 화면 탭(포틀릿) 영역: 위쪽 탭 줄 + 열어 둔 화면들.
 *
 * 열린 탭을 **모두 그려 두고** 보고 있는 탭만 보여준다(`hidden`). 그래서 탭을 옮겨 다녀도
 * 검색 조건, 스크롤이 아닌 입력 중인 값, 불러온 목록이 그대로 남고 다시 조회하지 않는다.
 * 대신 열어 둔 탭 수만큼 화면이 메모리에 남으므로 탭 수를 제한한다({@code MAX_TABS}).
 *
 * 탭을 누르면 그 탭이 마지막으로 보던 주소로 이동한다. 즉 주소가 곧 현재 탭이라
 * 새로고침·뒤로 가기·주소 공유가 그대로 동작한다.
 */
export function WorkspaceTabs() {
  const { tabs, activeKey, close, closeOthers, closeRight, closeAll } = useWorkspaceTabs()
  const navigate = useNavigate()

  // 한 번이라도 연 탭만 그린다. 새로고침으로 탭이 복원될 때 보이지 않는 화면까지 한꺼번에 그리면
  // 목록을 모두 조회하게 되고, 숨겨진 상태로 만들어진 표는 너비를 0으로 잡는다.
  const [mounted, setMounted] = useState<string[]>([])
  useEffect(() => {
    setMounted((previous) => (previous.includes(activeKey) ? previous : [...previous, activeKey]))
  }, [activeKey])

  const activeIndex = tabs.findIndex((tab) => tab.key === activeKey)
  const tabMenu: MenuProps = {
    items: [
      { key: 'others', label: '다른 탭 모두 닫기', disabled: tabs.length < 2 },
      { key: 'right', label: '오른쪽 탭 모두 닫기', disabled: activeIndex < 0 || activeIndex >= tabs.length - 1 },
      { type: 'divider' },
      { key: 'all', label: '모든 탭 닫기', disabled: tabs.length < 2 },
    ],
    onClick: ({ key }) => {
      if (key === 'others') {
        closeOthers(activeKey)
      } else if (key === 'right') {
        closeRight(activeKey)
      } else {
        closeAll()
      }
    },
  }

  return (
    <>
      <div className="workspace-tabbar">
        <Tabs
          type="editable-card"
          hideAdd
          size="small"
          activeKey={activeKey}
          // 탭이 기억하는 주소로 이동한다 (검색 조건이 남아 있는 목록으로 돌아간다).
          onChange={(key) => navigate(tabs.find((tab) => tab.key === key)?.path ?? key)}
          onEdit={(key, action) => {
            if (action === 'remove') {
              close(String(key))
            }
          }}
          items={tabs.map((tab) => ({
            key: tab.key,
            label: <TabLabel tab={tab} />,
            // 탭이 하나뿐이면 닫지 못하게 한다 (빈 화면이 되지 않도록).
            closable: tabs.length > 1,
          }))}
          tabBarExtraContent={
            <Dropdown menu={tabMenu} placement="bottomRight" trigger={['click']}>
              <Button type="text" size="small" icon={<MoreOutlined />} aria-label="탭 관리" />
            </Dropdown>
          }
        />
      </div>

      {tabs
        .filter((tab) => mounted.includes(tab.key))
        .map((tab) => (
          <div key={tab.key} className="workspace-pane" hidden={tab.key !== activeKey}>
            <ScreenRoutes location={tab.path} />
          </div>
        ))}
    </>
  )
}
