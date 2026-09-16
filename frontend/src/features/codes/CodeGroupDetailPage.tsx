import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, App, Button, Card, Descriptions, Drawer, Skeleton, Space, Table, Tag, Typography, type TableColumnsType } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { ChangeHistoryTimeline } from '@/components/ChangeHistoryTimeline'
import { PageHeader } from '@/components/PageHeader'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { useTabTitle } from '@/layouts/WorkspaceTabsContext'
import { formatDateTime } from '@/utils/format'
import { CodeGroupFormModal } from './CodeGroupFormModal'
import { CodeValueFormModal } from './CodeValueFormModal'
import {
  CODE_GROUP_FIELD_LABELS,
  CODE_VALUE_FIELD_LABELS,
  type CodeGroupColumnResponse,
  type CodeValueResponse,
} from './codeFields'
import { useDeleteCodeGroup } from './useDeleteCodeGroup'

/** 주소의 코드 그룹 ID. 양의 정수가 아니면 undefined */
function parseId(value: string | undefined): number | undefined {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : undefined
}

/**
 * 공통 코드 그룹 상세: 그룹 정보 · 코드값 목록(추가/수정/삭제) · 이 그룹에 매핑된 컬럼 · 변경 이력.
 *
 * 쓰기 버튼은 ADMIN에게만 보인다. 매핑된 컬럼이 있으면 그룹을 삭제할 수 없으므로(서버 CODE_GROUP_IN_USE)
 * 삭제 버튼 옆에 매핑 수를 알려주고, 컬럼 이름을 누르면 해당 테이블 상세로 이동한다.
 */
export function CodeGroupDetailPage() {
  const params = useParams()
  const groupId = parseId(params.id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.isAdmin ?? false
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const confirmDeleteGroup = useDeleteCodeGroup()

  const enabled = groupId !== undefined
  const groupQuery = $api.useQuery('get', '/api/code-groups/{id}', { params: { path: { id: groupId ?? 0 } } }, { enabled })
  const valuesQuery = $api.useQuery('get', '/api/code-groups/{groupId}/values', { params: { path: { groupId: groupId ?? 0 } } }, { enabled })
  const columnsQuery = $api.useQuery('get', '/api/code-groups/{id}/columns', { params: { path: { id: groupId ?? 0 } } }, { enabled })
  const group = groupQuery.data
  const values = valuesQuery.data ?? []
  const mappedColumns = columnsQuery.data ?? []
  // 탭 이름을 코드ID로 바꾼다 (여러 코드 그룹을 탭으로 열어 둘 때 구분하려고).
  useTabTitle(group?.codeId)

  const [editingGroup, setEditingGroup] = useState(false)
  const [formValue, setFormValue] = useState<{ open: boolean; value: CodeValueResponse | null }>({ open: false, value: null })
  const [historyValue, setHistoryValue] = useState<CodeValueResponse | null>(null)

  const deleteValue = $api.useMutation('delete', '/api/code-groups/{groupId}/values/{valueId}')

  const nextSortOrder = values.reduce((max, value) => Math.max(max, value.sortOrder ?? 0), 0) + 1
  const backToList = () => navigate('/codes')

  const handleDeleteValue = (value: CodeValueResponse) => {
    if (groupId === undefined || value.id === undefined) {
      return
    }
    const valueId = value.id
    modal.confirm({
      title: '코드값을 삭제할까요?',
      content: `${value.codeValue} (${value.valueName})`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteValue
          .mutateAsync({ params: { path: { groupId, valueId } } })
          .then(() => {
            message.success('코드값을 삭제했습니다.')
            invalidate('/api/code-groups/{groupId}/values', '/api/histories')
          })
          .catch((error: unknown) => {
            message.error(errorMessage(error))
          }),
    })
  }

  const valueColumns: TableColumnsType<CodeValueResponse> = [
    { title: '순서', dataIndex: 'sortOrder', width: 70, align: 'center' },
    { title: '코드값', dataIndex: 'codeValue', width: 140, render: (value: string) => <span className="mono">{value}</span> },
    { title: '코드값명', dataIndex: 'valueName', width: 200 },
    {
      title: '사용',
      dataIndex: 'used',
      width: 90,
      align: 'center',
      render: (used: boolean) => (used ? <Tag color="green">사용</Tag> : <Tag>미사용</Tag>),
    },
    { title: '설명', dataIndex: 'description', ellipsis: true },
    {
      title: '작업',
      key: 'actions',
      width: isAdmin ? 150 : 70,
      fixed: 'right',
      render: (_, value) => (
        <Space size={0}>
          {isAdmin && (
            <Button type="link" size="small" onClick={() => setFormValue({ open: true, value })}>
              수정
            </Button>
          )}
          <Button type="link" size="small" onClick={() => setHistoryValue(value)}>
            이력
          </Button>
          {isAdmin && (
            <Button type="link" size="small" danger onClick={() => handleDeleteValue(value)}>
              삭제
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const mappedColumnColumns: TableColumnsType<CodeGroupColumnResponse> = [
    {
      title: '테이블',
      key: 'table',
      width: 280,
      render: (_, column) => (
        <Space size={6}>
          <Link to={`/tables/${column.tableId}`} className="mono">
            {column.tablePhysicalName}
          </Link>
          <Typography.Text type="secondary">{column.tableLogicalName}</Typography.Text>
        </Space>
      ),
    },
    { title: '컬럼 물리명', dataIndex: 'physicalName', width: 220, render: (value: string) => <span className="mono">{value}</span> },
    { title: '컬럼 논리명', dataIndex: 'logicalName' },
  ]

  if (groupId === undefined || groupQuery.isError) {
    return (
      <>
        <PageHeader title="공통 코드" onBack={backToList} />
        <Alert
          type="error"
          showIcon
          title={groupQuery.isError ? errorMessage(groupQuery.error) : '코드 그룹을 찾을 수 없습니다.'}
          action={<Button onClick={backToList}>목록으로</Button>}
        />
      </>
    )
  }

  return (
    <>
      <PageHeader
        title={group?.codeId ?? '공통 코드'}
        subTitle={group?.codeName}
        onBack={backToList}
        extra={
          group &&
          isAdmin && (
            <Space wrap>
              <Button icon={<EditOutlined />} onClick={() => setEditingGroup(true)}>
                그룹 수정
              </Button>
              <Button
                danger
                icon={<DeleteOutlined />}
                disabled={mappedColumns.length > 0}
                title={mappedColumns.length > 0 ? '컬럼에 매핑된 코드 그룹은 삭제할 수 없습니다.' : undefined}
                onClick={() => confirmDeleteGroup(group, { valueCount: valuesQuery.data?.length, onDeleted: backToList })}
              >
                그룹 삭제
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setFormValue({ open: true, value: null })}>
                코드값 추가
              </Button>
            </Space>
          )
        }
      />

      <Card className="page-card">
        {groupQuery.isPending ? (
          <Skeleton active paragraph={{ rows: 2 }} />
        ) : (
          group && (
            <Descriptions column={{ xs: 1, md: 2 }} size="small">
              <Descriptions.Item label={CODE_GROUP_FIELD_LABELS.codeId}>
                <span className="mono">{group.codeId}</span>
              </Descriptions.Item>
              <Descriptions.Item label={CODE_GROUP_FIELD_LABELS.codeName}>{group.codeName}</Descriptions.Item>
              <Descriptions.Item label={CODE_GROUP_FIELD_LABELS.used}>
                {group.used ? <Tag color="green">사용</Tag> : <Tag>미사용</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label={CODE_GROUP_FIELD_LABELS.description}>{group.description || '-'}</Descriptions.Item>
              <Descriptions.Item label="등록 일시">{formatDateTime(group.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="수정 일시">{formatDateTime(group.updatedAt)}</Descriptions.Item>
            </Descriptions>
          )
        )}
      </Card>

      <Card
        className="page-card"
        title={
          <Space size={8} wrap>
            코드값
            {valuesQuery.isSuccess && (
              <Typography.Text type="secondary" className="card-title-hint">
                {values.length}개 · 사용 {values.filter((value) => value.used).length}
              </Typography.Text>
            )}
          </Space>
        }
      >
        {valuesQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(valuesQuery.error)} />}
        <Table<CodeValueResponse>
          rowKey="id"
          size="middle"
          columns={valueColumns}
          dataSource={values}
          loading={valuesQuery.isFetching}
          scroll={{ x: 800 }}
          // 그룹 하나의 코드값 전체를 한 번에 받으므로 나누지 않는다.
          pagination={false}
          locale={{ emptyText: isAdmin ? '코드값이 없습니다. 코드값 추가로 등록하세요.' : '코드값이 없습니다' }}
        />
      </Card>

      <Card
        className="page-card"
        title={
          <Space size={8} wrap>
            매핑된 컬럼
            {columnsQuery.isSuccess && (
              <Typography.Text type="secondary" className="card-title-hint">
                {mappedColumns.length}개
              </Typography.Text>
            )}
          </Space>
        }
      >
        {columnsQuery.isError && <Alert type="error" showIcon className="page-notice" title={errorMessage(columnsQuery.error)} />}
        <Table<CodeGroupColumnResponse>
          rowKey="columnId"
          size="middle"
          columns={mappedColumnColumns}
          dataSource={mappedColumns}
          loading={columnsQuery.isFetching}
          scroll={{ x: 700 }}
          pagination={false}
          locale={{ emptyText: '이 코드 그룹을 쓰는 컬럼이 없습니다. 테이블 정의의 컬럼 수정에서 매핑합니다.' }}
        />
      </Card>

      <Card className="page-card" title="코드 그룹 변경 이력">
        <ChangeHistoryTimeline targetType="CODE_GROUP" targetId={groupId} fieldLabels={CODE_GROUP_FIELD_LABELS} />
      </Card>

      <CodeGroupFormModal open={editingGroup} group={group ?? null} onClose={() => setEditingGroup(false)} />

      <CodeValueFormModal
        open={formValue.open}
        groupId={groupId}
        value={formValue.value}
        nextSortOrder={nextSortOrder}
        onClose={() => setFormValue({ open: false, value: null })}
      />

      <Drawer
        open={historyValue !== null}
        onClose={() => setHistoryValue(null)}
        size="large"
        title={historyValue ? `${historyValue.codeValue} 변경 이력` : '코드값 변경 이력'}
      >
        {historyValue?.id !== undefined && (
          <ChangeHistoryTimeline targetType="CODE_VALUE" targetId={historyValue.id} fieldLabels={CODE_VALUE_FIELD_LABELS} />
        )}
      </Drawer>
    </>
  )
}
