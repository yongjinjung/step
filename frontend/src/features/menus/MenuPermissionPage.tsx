import { SaveOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, App, Button, Card, Col, Empty, Input, Row, Space, Switch, Table, Tag, Tree, Typography, type TableColumnsType } from 'antd'
import { useEffect, useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import type { components } from '@/api/schema'
import { PageHeader } from '@/components/PageHeader'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { menuIcon } from '@/layouts/menuIcons'
import type { MenuResponse } from './menuFields'

type UserResponse = components['schemas']['UserResponse']

/**
 * 메뉴 권한 화면 (관리자 전용): 사용자마다 볼 수 있는 메뉴를 지정한다.
 *
 * - 지정하지 않은 사용자는 기본 규칙(사용 중인 메뉴 + 역할)대로 본다. "기본 권한 사용"을 끄면 고른 메뉴만 보인다.
 * - 지정은 좁히는 쪽으로만 동작한다. 미사용 메뉴나 관리자 전용 메뉴는 골라도 일반 사용자에게 보이지 않는다.
 * - 메뉴가 보이는지의 문제일 뿐, 기능 권한은 서버가 API마다 그대로 검사한다.
 */
export function MenuPermissionPage() {
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()

  const [keyword, setKeyword] = useState<string | undefined>(undefined)
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null)
  const [useDefault, setUseDefault] = useState(true)
  const [checkedKeys, setCheckedKeys] = useState<string[]>([])

  const usersQuery = $api.useQuery(
    'get',
    '/api/users',
    { params: { query: { keyword, size: 50 } } },
    { placeholderData: keepPreviousData },
  )
  const menusQuery = $api.useQuery('get', '/api/menus/all')
  const permissionQuery = $api.useQuery(
    'get',
    '/api/users/{userId}/menus',
    { params: { path: { userId: selectedUserId ?? 0 } } },
    { enabled: selectedUserId !== null },
  )
  const savePermission = $api.useMutation('put', '/api/users/{userId}/menus')

  const users = usersQuery.data?.content ?? []
  const topMenus = menusQuery.data ?? []
  const selectedUser = users.find((user) => user.id === selectedUserId)
  const permission = permissionQuery.data

  // 사용자를 고르거나 저장된 권한이 도착하면 편집 상태를 그 값으로 맞춘다.
  useEffect(() => {
    if (permission) {
      setUseDefault(permission.useDefault ?? true)
      setCheckedKeys((permission.menuIds ?? []).map(String))
    }
  }, [permission])

  /** 하위(LEFT) 메뉴 ID. 저장할 때 상위 메뉴 체크는 빼고 하위 메뉴만 보낸다. */
  const childMenuIds = new Set(
    topMenus.flatMap((top) => (top.children ?? []).map((menu) => menu.id).filter((id): id is number => id !== undefined)),
  )

  const treeData = topMenus.map((top) => ({
    key: String(top.id),
    title: menuTitle(top),
    children: (top.children ?? []).map((menu) => ({ key: String(menu.id), title: menuTitle(menu) })),
  }))

  const handleSave = () => {
    if (selectedUserId === null) {
      return
    }
    const menuIds = checkedKeys.map(Number).filter((id) => childMenuIds.has(id))
    savePermission.mutate(
      { params: { path: { userId: selectedUserId } }, body: { useDefault, menuIds } },
      {
        onSuccess: () => {
          message.success('메뉴 권한을 저장했습니다.')
          // 내 권한을 바꿨을 수도 있으므로 화면 메뉴도 다시 불러온다.
          invalidate('/api/users/{userId}/menus', '/api/menus')
        },
        onError: (error: unknown) => message.error(errorMessage(error)),
      },
    )
  }

  const userColumns: TableColumnsType<UserResponse> = [
    { title: '아이디', dataIndex: 'username', width: 130 },
    { title: '이름', dataIndex: 'name' },
    {
      title: '역할',
      dataIndex: 'role',
      width: 90,
      align: 'center',
      render: (role: string) => (role === 'ADMIN' ? <Tag color="volcano">관리자</Tag> : <Tag color="blue">사용자</Tag>),
    },
    {
      title: '활성',
      dataIndex: 'enabled',
      width: 80,
      align: 'center',
      render: (enabled: boolean) => (enabled ? <Tag color="green">사용</Tag> : <Tag>중지</Tag>),
    },
  ]

  return (
    <>
      <PageHeader title="메뉴 권한" />

      <Alert
        type="info"
        showIcon
        className="page-notice"
        title="사용자마다 볼 수 있는 메뉴를 지정합니다."
        description="지정하지 않으면 역할 기준으로 봅니다. 지정하면 고른 메뉴만 보이며, 미사용·관리자 전용 메뉴는 골라도 보이지 않습니다. 화면에 보이는지의 문제이고 기능 권한은 서버가 따로 검사합니다."
      />

      <Row gutter={16}>
        <Col xs={24} lg={10}>
          <Card title="사용자">
            <Input.Search
              placeholder="아이디, 이름"
              allowClear
              className="page-notice"
              onSearch={(value) => setKeyword(value || undefined)}
            />
            {usersQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(usersQuery.error)} />}
            <Table<UserResponse>
              rowKey="id"
              size="small"
              columns={userColumns}
              dataSource={users}
              loading={usersQuery.isFetching}
              pagination={false}
              scroll={{ y: 420 }}
              rowClassName={(user) => (user.id === selectedUserId ? 'clickable-row selected-row' : 'clickable-row')}
              onRow={(user) => ({ onClick: () => user.id !== undefined && setSelectedUserId(user.id) })}
              locale={{ emptyText: '사용자가 없습니다' }}
            />
          </Card>
        </Col>

        <Col xs={24} lg={14}>
          <Card
            title={selectedUser ? `${selectedUser.name} (${selectedUser.username}) 메뉴 권한` : '메뉴 권한'}
            extra={
              selectedUser && (
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  loading={savePermission.isPending}
                  onClick={handleSave}
                >
                  저장
                </Button>
              )
            }
          >
            {!selectedUser ? (
              <Empty description="왼쪽에서 사용자를 고르세요" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <>
                <Space className="page-notice" size={12} wrap>
                  <Switch checked={useDefault} onChange={setUseDefault} checkedChildren="기본" unCheckedChildren="지정" />
                  <Typography.Text type="secondary">
                    {useDefault
                      ? '기본 권한 사용: 역할 기준으로 메뉴를 봅니다.'
                      : '지정한 메뉴만 보여줍니다. 아무것도 고르지 않으면 메뉴가 보이지 않습니다.'}
                  </Typography.Text>
                </Space>

                {selectedUser.role === 'ADMIN' && (
                  <Alert
                    type="warning"
                    showIcon
                    className="page-notice"
                    title="관리자도 지정한 메뉴만 보게 됩니다. 메뉴가 사라져도 주소로는 들어갈 수 있습니다."
                  />
                )}

                {permissionQuery.isError && (
                  <Alert type="error" showIcon className="page-notice" title={errorMessage(permissionQuery.error)} />
                )}

                <Tree
                  checkable
                  disabled={useDefault}
                  defaultExpandAll
                  treeData={treeData}
                  checkedKeys={checkedKeys}
                  onCheck={(checked) => setCheckedKeys((Array.isArray(checked) ? checked : checked.checked).map(String))}
                  selectable={false}
                />
              </>
            )}
          </Card>
        </Col>
      </Row>
    </>
  )
}

/** 메뉴 하나의 트리 표시: 아이콘 + 이름 + (경로) + 미사용·관리자 전용 표시 */
function menuTitle(menu: MenuResponse) {
  return (
    <Space size={6}>
      {menuIcon(menu.icon)}
      <span>{menu.name}</span>
      {menu.path && <Typography.Text type="secondary" className="mono option-hint">{menu.path}</Typography.Text>}
      {!menu.used && <Tag>미사용</Tag>}
      {menu.adminOnly && <Tag color="volcano">관리자</Tag>}
    </Space>
  )
}
