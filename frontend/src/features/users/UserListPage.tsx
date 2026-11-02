import { KeyOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Radio, Select, Space, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { PageHeader } from '@/components/PageHeader'
import { useListSearchParams } from '@/hooks/useListSearchParams'
import { useMenus } from '@/layouts/MenuContext'
import { formatDateTime } from '@/utils/format'
import { PasswordResetModal } from './PasswordResetModal'
import { UserFormModal } from './UserFormModal'
import type { UserResponse, UserRole } from './userFields'

/** URL 쿼리스트링에 두는 검색 조건 이름 */
const FILTER_KEYS = ['keyword', 'roleId', 'enabled'] as const

const ENABLED_OPTIONS = [
  { label: '전체', value: '' },
  { label: '활성', value: 'true' },
  { label: '비활성', value: 'false' },
]

/**
 * 사용자 관리 화면: 계정 검색 · 등록 · 수정 · 비밀번호 초기화.
 *
 * 계정은 삭제하지 않고 비활성으로 둔다(로그인 불가). 아이디는 등록 후 바꿀 수 없고,
 * 전권 역할을 가진 활성 사용자가 한 명도 남지 않는 변경은 서버가 막는다.
 * 사용자에게는 역할을 여러 개 줄 수 있고, 역할별 권한은 역할 관리 화면에서 정한다.
 */
export function UserListPage() {
  const { user: me } = useAuth()
  const { can } = useMenus()
  const canCreate = can('/users', 'create')
  const canUpdate = can('/users', 'update')
  const canManagePermissions = can('/menu-permissions', 'read')
  const navigate = useNavigate()
  const { filters, page, size, setFilters, setPaging } = useListSearchParams(FILTER_KEYS)

  const roleId = Number(filters.roleId) > 0 ? Number(filters.roleId) : undefined
  const enabled = filters.enabled === 'true' ? true : filters.enabled === 'false' ? false : undefined

  const rolesQuery = $api.useQuery('get', '/api/roles')
  const listQuery = $api.useQuery(
    'get',
    '/api/users',
    { params: { query: { keyword: filters.keyword, roleId, enabled, page: page - 1, size } } },
    { placeholderData: keepPreviousData },
  )

  const [formUser, setFormUser] = useState<{ open: boolean; user: UserResponse | null }>({ open: false, user: null })
  const [passwordUser, setPasswordUser] = useState<UserResponse | null>(null)

  /** 검색 조건 하나만 바꾸고 나머지는 유지한다. */
  const updateFilter = (patch: Partial<typeof filters>) => setFilters({ ...filters, ...patch })

  const roleOptions = (rolesQuery.data ?? []).map((role) => ({
    value: String(role.id),
    label: `${role.name} (${role.code})`,
  }))

  const columns: TableColumnsType<UserResponse> = [
    {
      title: '아이디',
      dataIndex: 'username',
      width: 170,
      render: (username: string) => (
        <Space size={6}>
          <span className="mono">{username}</span>
          {username === me?.username && <Tag>나</Tag>}
        </Space>
      ),
    },
    { title: '이름', dataIndex: 'name', width: 140 },
    {
      title: '역할',
      dataIndex: 'roles',
      width: 240,
      render: (roles: UserRole[] = [], user) => (
        <Space size={[0, 4]} wrap>
          {roles.map((role) => (
            <Tag key={role.id} color={user.superAdmin && role.code === 'ADMIN' ? 'volcano' : 'blue'}>
              {role.name}
            </Tag>
          ))}
          {roles.length === 0 && <Typography.Text type="secondary">없음</Typography.Text>}
        </Space>
      ),
    },
    {
      title: '상태',
      dataIndex: 'enabled',
      width: 90,
      align: 'center',
      render: (value: boolean) => (value ? <Tag color="green">활성</Tag> : <Tag>비활성</Tag>),
    },
    { title: '등록 일시', dataIndex: 'createdAt', width: 170, render: (value: string) => formatDateTime(value) },
    { title: '수정 일시', dataIndex: 'updatedAt', width: 170, render: (value: string) => formatDateTime(value) },
    {
      title: '작업',
      key: 'actions',
      width: 220,
      fixed: 'right',
      render: (_, user) => (
        <Space size={0}>
          {canUpdate && (
            <Button type="link" size="small" onClick={() => setFormUser({ open: true, user })}>
              수정
            </Button>
          )}
          {canUpdate && (
            <Button type="link" size="small" icon={<KeyOutlined />} onClick={() => setPasswordUser(user)}>
              비밀번호
            </Button>
          )}
          {canManagePermissions && (
            <Button type="link" size="small" onClick={() => navigate(`/menu-permissions?userId=${user.id}`)}>
              메뉴 권한
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title="사용자 관리"
        extra={
          canCreate && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormUser({ open: true, user: null })}>
              계정 등록
            </Button>
          )
        }
      />

      <Alert
        type="info"
        showIcon
        className="page-notice"
        title="계정은 삭제하지 않고 비활성으로 둡니다."
        description="권한은 사용자에게 준 역할에서 옵니다(역할 관리). 아이디는 등록 후 바꿀 수 없고, 전권 역할을 가진 활성 사용자가 한 명도 남지 않는 변경은 저장되지 않습니다."
      />

      <Card>
        <div className="list-toolbar">
          <Input.Search
            key={filters.keyword ?? ''}
            defaultValue={filters.keyword}
            placeholder="아이디, 이름"
            allowClear
            className="search-input"
            onSearch={(keyword) => updateFilter({ keyword })}
          />
          <Select
            value={filters.roleId}
            options={roleOptions}
            placeholder="역할 전체"
            allowClear
            className="filter-select"
            loading={rolesQuery.isFetching}
            onChange={(value?: string) => updateFilter({ roleId: value })}
          />
          <Radio.Group
            optionType="button"
            options={ENABLED_OPTIONS}
            value={filters.enabled ?? ''}
            onChange={(event) => updateFilter({ enabled: event.target.value || undefined })}
          />
          <Button icon={<ReloadOutlined />} onClick={() => setFilters({})}>
            초기화
          </Button>
        </div>

        {listQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(listQuery.error)} />}

        <Table<UserResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={listQuery.data?.content}
          loading={listQuery.isFetching}
          scroll={{ x: 1200 }}
          locale={{ emptyText: <Typography.Text type="secondary">사용자가 없습니다</Typography.Text> }}
          pagination={{
            current: page,
            pageSize: size,
            total: listQuery.data?.totalElements ?? 0,
            showSizeChanger: true,
            pageSizeOptions: [20, 50, 100],
            showTotal: (total) => `총 ${total.toLocaleString()}건`,
            onChange: setPaging,
          }}
        />
      </Card>

      <UserFormModal open={formUser.open} user={formUser.user} onClose={() => setFormUser({ open: false, user: null })} />

      <PasswordResetModal user={passwordUser} onClose={() => setPasswordUser(null)} />
    </>
  )
}
