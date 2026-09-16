import { PlusOutlined } from '@ant-design/icons'
import { Alert, App, Button, Card, Space, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { PageHeader } from '@/components/PageHeader'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { menuIcon } from '@/layouts/menuIcons'
import { SCREEN_PATHS } from '@/layouts/screens'
import { MenuFormModal } from './MenuFormModal'
import type { MenuResponse } from './menuFields'

interface FormState {
  open: boolean
  /** 수정할 메뉴. null이면 등록 */
  menu: MenuResponse | null
  /** 하위 추가로 열었을 때의 상위 메뉴 */
  parentId?: number
  sortOrder: number
}

const CLOSED_FORM: FormState = { open: false, menu: null, sortOrder: 1 }

/**
 * 메뉴 관리 화면 (관리자 전용): 위쪽(TOP) 메뉴와 그 아래 왼쪽(LEFT) 메뉴를 등록·수정·삭제한다.
 *
 * 목록은 미사용·관리자 전용 메뉴까지 모두 보여준다(`/api/menus/all`). 저장하면 위쪽·왼쪽 메뉴 막대가 바로 바뀐다.
 * 화면 경로가 등록된 화면(`layouts/screens.tsx`)에 없으면 "화면 없음"으로 표시해, 메뉴만 만들어 둔 상태를 알 수 있게 한다.
 */
export function MenuManagePage() {
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const menusQuery = $api.useQuery('get', '/api/menus/all')
  const deleteMenu = $api.useMutation('delete', '/api/menus/{id}')

  const [form, setForm] = useState<FormState>(CLOSED_FORM)

  const topMenus = menusQuery.data ?? []
  // 하위가 없는 상위 메뉴에 펼침 표시가 나오지 않도록 빈 배열은 없앤다.
  const dataSource = topMenus.map((top) => ({ ...top, children: top.children?.length ? top.children : undefined }))
  const nextTopOrder = topMenus.reduce((max, top) => Math.max(max, top.sortOrder ?? 0), 0) + 1

  const openCreateTop = () => setForm({ open: true, menu: null, sortOrder: nextTopOrder })

  const openCreateChild = (top: MenuResponse) => {
    const nextOrder = (top.children ?? []).reduce((max, child) => Math.max(max, child.sortOrder ?? 0), 0) + 1
    setForm({ open: true, menu: null, parentId: top.id, sortOrder: nextOrder })
  }

  const handleDelete = (menu: MenuResponse) => {
    if (menu.id === undefined) {
      return
    }
    const id = menu.id
    const childCount = menu.children?.length ?? 0
    modal.confirm({
      title: '메뉴를 삭제할까요?',
      content:
        childCount > 0
          ? `${menu.name} — 하위 메뉴 ${childCount}개가 있어 삭제할 수 없습니다. 하위 메뉴를 먼저 옮기거나 지우세요.`
          : `${menu.name}${menu.path ? ` (${menu.path})` : ''}`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteMenu
          .mutateAsync({ params: { path: { id } } })
          .then(() => {
            message.success('메뉴를 삭제했습니다.')
            invalidate('/api/menus/all', '/api/menus')
          })
          .catch((error: unknown) => {
            message.error(errorMessage(error))
          }),
    })
  }

  const columns: TableColumnsType<MenuResponse> = [
    {
      title: '메뉴명',
      dataIndex: 'name',
      width: 260,
      render: (name: string, menu) => (
        <Space size={8}>
          {menuIcon(menu.icon)}
          <span>{name}</span>
          {menu.parentId === undefined || menu.parentId === null ? <Tag color="blue">상위</Tag> : null}
        </Space>
      ),
    },
    {
      title: '화면 경로',
      dataIndex: 'path',
      width: 260,
      render: (path?: string) =>
        path ? (
          <Space size={6}>
            <span className="mono">{path}</span>
            {!SCREEN_PATHS.includes(path) && <Tag color="orange">화면 없음</Tag>}
          </Space>
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        ),
    },
    { title: '아이콘', dataIndex: 'icon', width: 120, render: (icon?: string) => icon ?? <Typography.Text type="secondary">-</Typography.Text> },
    { title: '순서', dataIndex: 'sortOrder', width: 80, align: 'center' },
    {
      title: '사용',
      dataIndex: 'used',
      width: 90,
      align: 'center',
      render: (used: boolean) => (used ? <Tag color="green">사용</Tag> : <Tag>미사용</Tag>),
    },
    {
      title: '관리자 전용',
      dataIndex: 'adminOnly',
      width: 110,
      align: 'center',
      render: (adminOnly: boolean) => (adminOnly ? <Tag color="volcano">관리자</Tag> : <Typography.Text type="secondary">전체</Typography.Text>),
    },
    {
      title: '작업',
      key: 'actions',
      width: 190,
      fixed: 'right',
      render: (_, menu) => {
        const isTop = menu.parentId === undefined || menu.parentId === null
        return (
          <Space size={0}>
            {isTop && (
              <Button type="link" size="small" onClick={() => openCreateChild(menu)}>
                하위 추가
              </Button>
            )}
            <Button type="link" size="small" onClick={() => setForm({ open: true, menu, sortOrder: menu.sortOrder ?? 1 })}>
              수정
            </Button>
            <Button type="link" size="small" danger onClick={() => handleDelete(menu)}>
              삭제
            </Button>
          </Space>
        )
      },
    },
  ]

  return (
    <>
      <PageHeader
        title="메뉴 관리"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateTop}>
            상위 메뉴 등록
          </Button>
        }
      />

      <Alert
        type="info"
        showIcon
        className="page-notice"
        title="위쪽(TOP) 메뉴와 그 아래 왼쪽(LEFT) 메뉴를 2단으로 관리합니다."
        description="상위 메뉴는 묶음이라 화면 경로가 없고, 하위 메뉴의 경로가 실제 화면을 가리킵니다. 저장하면 메뉴 막대에 바로 반영됩니다."
      />

      <Card>
        {menusQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(menusQuery.error)} />}
        <Table<MenuResponse>
          rowKey="id"
          size="middle"
          columns={columns}
          dataSource={dataSource}
          loading={menusQuery.isFetching}
          scroll={{ x: 1100 }}
          // 메뉴는 2단이고 수가 적어 전부 보여준다.
          pagination={false}
          expandable={{ defaultExpandAllRows: true }}
          locale={{ emptyText: '메뉴가 없습니다. 상위 메뉴부터 등록하세요.' }}
        />
      </Card>

      <MenuFormModal
        open={form.open}
        menu={form.menu}
        defaultParentId={form.parentId}
        defaultSortOrder={form.sortOrder}
        topMenus={topMenus}
        onClose={() => setForm(CLOSED_FORM)}
      />
    </>
  )
}
