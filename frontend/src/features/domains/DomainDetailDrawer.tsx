import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { Alert, Button, Descriptions, Drawer, Skeleton, Space, Tag, Typography } from 'antd'
import { Link } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { ChangeHistoryTimeline } from '@/components/ChangeHistoryTimeline'
import { formatDataType } from '@/utils/dataType'
import { formatDateTime } from '@/utils/format'
import { DOMAIN_FIELD_LABELS, type DomainResponse } from './domainFields'

/** 상세에서 보여줄 사용 중인 용어 수 */
const TERM_PREVIEW_SIZE = 10

interface DomainDetailDrawerProps {
  /** 보여줄 도메인 ID. null이면 닫힌다 */
  domainId: number | null
  onClose: () => void
  onEdit: (domain: DomainResponse) => void
  onDelete: (domain: DomainResponse) => void
}

/**
 * 도메인 상세: 기본 정보, 이 도메인을 쓰는 용어, 변경 이력.
 * 사용 중인 용어가 있으면 삭제할 수 없으므로(DOMAIN_IN_USE) 삭제 전에 여기서 확인할 수 있게 한다.
 */
export function DomainDetailDrawer({ domainId, onClose, onEdit, onDelete }: DomainDetailDrawerProps) {
  const { user } = useAuth()
  const isAdmin = user?.isAdmin ?? false
  const open = domainId !== null

  const domainQuery = $api.useQuery('get', '/api/domains/{id}', { params: { path: { id: domainId ?? 0 } } }, { enabled: open })
  const termsQuery = $api.useQuery(
    'get',
    '/api/terms',
    { params: { query: { domainId: domainId ?? 0, size: TERM_PREVIEW_SIZE } } },
    { enabled: open },
  )
  const domain = domainQuery.data
  const terms = termsQuery.data?.content ?? []
  const termTotal = termsQuery.data?.totalElements ?? 0

  return (
    <Drawer
      open={open}
      onClose={onClose}
      size="large"
      title={domain?.name ?? '도메인 상세'}
      extra={
        domain && (
          <Space>
            <Button icon={<EditOutlined />} onClick={() => onEdit(domain)}>
              {isAdmin ? '수정' : '수정 신청'}
            </Button>
            <Button danger icon={<DeleteOutlined />} onClick={() => onDelete(domain)}>
              {isAdmin ? '삭제' : '삭제 신청'}
            </Button>
          </Space>
        )
      }
    >
      {domainQuery.isPending && open ? (
        <Skeleton active />
      ) : domainQuery.isError ? (
        <Alert type="error" showIcon title={errorMessage(domainQuery.error)} />
      ) : (
        domain && (
          <>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label={DOMAIN_FIELD_LABELS.name}>{domain.name}</Descriptions.Item>
              <Descriptions.Item label={DOMAIN_FIELD_LABELS.classifierWordName}>{domain.classifierWordName}</Descriptions.Item>
              <Descriptions.Item label={DOMAIN_FIELD_LABELS.dataType}>
                <span className="mono">{formatDataType(domain.dataType, domain.dataLength, domain.dataScale)}</span>
              </Descriptions.Item>
              <Descriptions.Item label={DOMAIN_FIELD_LABELS.description}>{domain.description || '-'}</Descriptions.Item>
              <Descriptions.Item label="등록 일시">{formatDateTime(domain.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="수정 일시">{formatDateTime(domain.updatedAt)}</Descriptions.Item>
            </Descriptions>

            <Typography.Title level={5} className="section-title">
              사용 중인 용어 {termsQuery.isSuccess && <Typography.Text type="secondary">({termTotal.toLocaleString()}건)</Typography.Text>}
            </Typography.Title>
            {termsQuery.isPending ? (
              <Skeleton active paragraph={{ rows: 1 }} />
            ) : termTotal === 0 ? (
              <Typography.Text type="secondary">이 도메인을 쓰는 용어가 없습니다.</Typography.Text>
            ) : (
              <Space size={[4, 8]} wrap>
                {terms.map((term) => (
                  <Tag key={term.id}>
                    {term.name} <span className="mono">{term.abbreviation}</span>
                  </Tag>
                ))}
                {termTotal > terms.length && (
                  <Link to={`/terms?domainId=${domain.id}`}>외 {(termTotal - terms.length).toLocaleString()}건 보기</Link>
                )}
              </Space>
            )}

            <Typography.Title level={5} className="section-title">
              변경 이력
            </Typography.Title>
            {domain.id !== undefined && (
              <ChangeHistoryTimeline targetType="DOMAIN" targetId={domain.id} fieldLabels={DOMAIN_FIELD_LABELS} />
            )}
          </>
        )
      )}
    </Drawer>
  )
}
