import { PlusOutlined, SaveOutlined } from '@ant-design/icons'
import { Alert, App, Button, Card, Checkbox, Col, Empty, Row, Space, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useEffect, useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import type { MenuResponse } from '@/layouts/MenuContext'
import { menuIcon } from '@/layouts/menuIcons'
import { RoleFormModal } from './RoleFormModal'
import { PERMISSION_COLUMNS, type PermissionKey, type RoleResponse } from './roleFields'

/** 메뉴 하나에 대한 권한 상태 (매트릭스 편집용) */
type MenuPermissionState = Record<PermissionKey, boolean>

const EMPTY_PERMISSION: MenuPermissionState = { canRead: false, canCreate: false, canUpdate: false, canDelete: false }

/**
 * 역할 관리 화면: 역할 목록과 역할별 메뉴 권한 매트릭스.
 *
 * - 왼쪽에서 역할을 고르면 오른쪽에 메뉴별 조회/등록/수정/삭제 권한이 나온다.
 * - 권한은 하위(LEFT) 메뉴에 준다. 상위 메뉴는 보여줄 하위가 있으면 자동으로 보인다.
 * - 전권 역할은 매트릭스와 무관하게 모든 기능을 쓸 수 있어 편집을 잠근다.
 * - 저장하면 서버 검사(요청 권한)와 화면(메뉴·버튼)에 함께 반영된다.
 */
export function RoleListPage() {
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()

  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null)
  const [formRole, setFormRole] = useState<{ open: boolean; role: RoleResponse | null }>({ open: false, role: null })
  const [permissions, setPermissions] = useState<Record<number, MenuPermissionState>>({})

  const rolesQuery = $api.useQuery('get', '/api/roles')
  const menusQuery = $api.useQuery('get', '/api/menus/all')
  const permissionQuery = $api.useQuery(
    'get',
    '/api/roles/{id}/menus',
    { params: { path: { id: selectedRoleId ?? 0 } } },
    { enabled: selectedRoleId !== null },
  )
  const savePermissions = $api.useMutation('put', '/api/roles/{id}/menus')
  const deleteRole = $api.useMutation('delete', '/api/roles/{id}')

  const roles = rolesQuery.data ?? []
  const topMenus = menusQuery.data ?? []
  const selectedRole = roles.find((role) => role.id === selectedRoleId)

  // 역할을 고르거나 저장된 권한이 도착하면 매트릭스를 그 값으로 맞춘다.
  useEffect(() => {
    if (!permissionQuery.data) {
      return
    }
    const next: Record<number, MenuPermissionState> = {}
    for (const permission of permissionQuery.data) {
      if (permission.menuId !== undefined) {
        next[permission.menuId] = {
          canRead: permission.canRead ?? false,
          canCreate: permission.canCreate ?? false,
          canUpdate: permission.canUpdate ?? false,
          canDelete: permission.canDelete ?? false,
        }
      }
    }
    setPermissions(next)
  }, [permissionQuery.data])

  const permissionOf = (menuId?: number): MenuPermissionState =>
    (menuId !== undefined && permissions[menuId]) || EMPTY_PERMISSION

  const togglePermission = (menuId: number, key: PermissionKey, checked: boolean) => {
    setPermissions((previous) => {
      const current = previous[menuId] ?? EMPTY_PERMISSION
      // 조회 권한을 끄면 화면 자체가 보이지 않으므로 나머지 권한도 함께 끈다.
      const next = key === 'canRead' && !checked ? { ...EMPTY_PERMISSION } : { ...current, [key]: checked }
      // 등록·수정·삭제를 켜면 조회는 자동으로 켠다 (화면을 봐야 쓸 수 있다).
      if (key !== 'canRead' && checked) {
        next.canRead = true
      }
      return { ...previous, [menuId]: next }
    })
  }

  const handleSave = () => {
    if (selectedRoleId === null) {
      return
    }
    const body = {
      permissions: Object.entries(permissions)
        .filter(([, value]) => value.canRead || value.canCreate || value.canUpdate || value.canDelete)
        .map(([menuId, value]) => ({ menuId: Number(menuId), ...value })),
    }
    savePermissions.mutate(
      { params: { path: { id: selectedRoleId } }, body },
      {
        onSuccess: () => {
          message.success('메뉴 권한을 저장했습니다.')
          // 내 역할의 권한을 바꿨을 수도 있으므로 화면 메뉴도 다시 불러온다.
          invalidate('/api/roles/{id}/menus', '/api/menus')
        },
        onError: (error: unknown) => message.error(errorMessage(error)),
      },
    )
  }

  const handleDelete = (role: RoleResponse) => {
    if (role.id === undefined) {
      return
    }
    const id = role.id
    modal.confirm({
      title: '역할을 삭제할까요?',
      content: `${role.code} (${role.name}) — 사용자에게 부여된 역할은 삭제할 수 없습니다.`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteRole
          .mutateAsync({ params: { path: { id } } })
          .then(() => {
            message.success('역할을 삭제했습니다.')
            if (selectedRoleId === id) {
              setSelectedRoleId(null)
            }
            invalidate('/api/roles', '/api/menus')
          })
          .catch((error: unknown) => {
            message.error(errorMessage(error))
          }),
    })
  }

  const roleColumns: TableColumnsType<RoleResponse> = [
    {
      title: '코드',
      dataIndex: 'code',
      width: 150,
      render: (code: string, role) => (
        <Space size={6}>
          <span className="mono">{code}</span>
          {role.superAdmin && <Tag color="volcano">전권</Tag>}
          {!role.used && <Tag>미사용</Tag>}
        </Space>
      ),
    },
    { title: '역할명', dataIndex: 'name' },
    {
      title: '작업',
      key: 'actions',
      width: 110,
      render: (_, role) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => setFormRole({ open: true, role })}>
            수정
          </Button>
          <Button type="link" size="small" danger disabled={role.builtIn} onClick={() => handleDelete(role)}>
            삭제
          </Button>
        </Space>
      ),
    },
  ]

  // 메뉴 트리를 그대로 쓰되, 하위가 없는 상위 메뉴는 펼침 표시가 나오지 않게 한다.
  const menuRows = topMenus.map((top) => ({ ...top, children: top.children?.length ? top.children : undefined }))

  const permissionColumns: TableColumnsType<MenuResponse> = [
    {
      title: '메뉴',
      dataIndex: 'name',
      width: 240,
      render: (name: string, menu) => (
        <Space size={6}>
          {menuIcon(menu.icon)}
          <span>{name}</span>
          {menu.path && <Typography.Text type="secondary" className="mono option-hint">{menu.path}</Typography.Text>}
          {!menu.used && <Tag>미사용</Tag>}
        </Space>
      ),
    },
    ...PERMISSION_COLUMNS.map(({ key, label }) => ({
      title: label,
      key,
      width: 80,
      align: 'center' as const,
      render: (_: unknown, menu: MenuResponse) =>
        // 상위 메뉴는 화면이 없어 권한을 주지 않는다 (하위가 보이면 자동으로 보인다).
        menu.path && menu.id !== undefined ? (
          <Checkbox
            checked={permissionOf(menu.id)[key]}
            disabled={selectedRole?.superAdmin}
            onChange={(event) => togglePermission(menu.id as number, key, event.target.checked)}
          />
        ) : null,
    })),
  ]

  return (
    <>
      <PageHeader
        title="역할 관리"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormRole({ open: true, role: null })}>
            역할 등록
          </Button>
        }
      />

      <Alert
        type="info"
        showIcon
        className="page-notice"
        title="역할에 메뉴별 권한(조회·등록·수정·삭제)을 주고, 사용자에게 역할을 부여합니다."
        description="권한은 역할들의 합집합입니다. 화면의 버튼뿐 아니라 서버 요청도 같은 권한으로 검사합니다. 전권 역할은 모든 기능을 쓸 수 있습니다."
      />

      <Row gutter={16}>
        <Col xs={24} lg={9}>
          <Card title="역할">
            {rolesQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(rolesQuery.error)} />}
            <Table<RoleResponse>
              rowKey="id"
              size="small"
              columns={roleColumns}
              dataSource={roles}
              loading={rolesQuery.isFetching}
              pagination={false}
              rowClassName={(role) => (role.id === selectedRoleId ? 'clickable-row selected-row' : 'clickable-row')}
              onRow={(role) => ({ onClick: () => role.id !== undefined && setSelectedRoleId(role.id) })}
              locale={{ emptyText: '역할이 없습니다' }}
            />
          </Card>
        </Col>

        <Col xs={24} lg={15}>
          <Card
            title={selectedRole ? `${selectedRole.name} (${selectedRole.code}) 메뉴 권한` : '메뉴 권한'}
            extra={
              selectedRole &&
              !selectedRole.superAdmin && (
                <Button type="primary" icon={<SaveOutlined />} loading={savePermissions.isPending} onClick={handleSave}>
                  저장
                </Button>
              )
            }
          >
            {!selectedRole ? (
              <Empty description="왼쪽에서 역할을 고르세요" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <>
                {selectedRole.superAdmin && (
                  <Alert
                    type="warning"
                    showIcon
                    className="page-notice"
                    title="전권 역할입니다. 메뉴 권한과 관계없이 모든 기능을 쓸 수 있어 여기서 설정하지 않습니다."
                  />
                )}
                {permissionQuery.isError && (
                  <Alert type="error" showIcon className="page-notice" title={errorMessage(permissionQuery.error)} />
                )}
                <Table<MenuResponse>
                  rowKey="id"
                  size="small"
                  columns={permissionColumns}
                  dataSource={menuRows}
                  loading={menusQuery.isFetching || permissionQuery.isFetching}
                  pagination={false}
                  expandable={{ defaultExpandAllRows: true }}
                  scroll={{ x: 640 }}
                  locale={{ emptyText: '메뉴가 없습니다' }}
                />
              </>
            )}
          </Card>
        </Col>
      </Row>

      <RoleFormModal
        open={formRole.open}
        role={formRole.role}
        onClose={() => setFormRole({ open: false, role: null })}
        onSaved={(saved, created) => created && saved.id !== undefined && setSelectedRoleId(saved.id)}
      />
    </>
  )
}
