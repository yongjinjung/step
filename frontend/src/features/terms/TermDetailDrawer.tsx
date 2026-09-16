import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { Alert, Button, Descriptions, Drawer, Skeleton, Space, Tag, Typography } from 'antd'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { ChangeHistoryTimeline } from '@/components/ChangeHistoryTimeline'
import { formatDataType } from '@/utils/dataType'
import { formatDateTime } from '@/utils/format'
import { TERM_FIELD_LABELS, type TermResponse } from './termFields'

interface TermDetailDrawerProps {
  /** 보여줄 용어 ID. null이면 닫힌다 */
  termId: number | null
  onClose: () => void
  onEdit: (term: TermResponse) => void
  onDelete: (term: TermResponse) => void
}

/**
 * 용어 상세: 기본 정보(구성 단어 순서, 도메인의 현재 타입·길이)와 변경 이력.
 * 도메인 타입·길이는 저장값이 아니라 조회 시점의 도메인 값이다.
 */
export function TermDetailDrawer({ termId, onClose, onEdit, onDelete }: TermDetailDrawerProps) {
  const { user } = useAuth()
  const isAdmin = user?.isAdmin ?? false
  const open = termId !== null

  const termQuery = $api.useQuery('get', '/api/terms/{id}', { params: { path: { id: termId ?? 0 } } }, { enabled: open })
  const term = termQuery.data

  return (
    <Drawer
      open={open}
      onClose={onClose}
      size="large"
      title={term?.name ?? '용어 상세'}
      extra={
        term && (
          <Space>
            <Button icon={<EditOutlined />} onClick={() => onEdit(term)}>
              {isAdmin ? '수정' : '수정 신청'}
            </Button>
            <Button danger icon={<DeleteOutlined />} onClick={() => onDelete(term)}>
              {isAdmin ? '삭제' : '삭제 신청'}
            </Button>
          </Space>
        )
      }
    >
      {termQuery.isPending && open ? (
        <Skeleton active />
      ) : termQuery.isError ? (
        <Alert type="error" showIcon title={errorMessage(termQuery.error)} />
      ) : (
        term && (
          <>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label={TERM_FIELD_LABELS.name}>{term.name}</Descriptions.Item>
              <Descriptions.Item label={TERM_FIELD_LABELS.abbreviation}>
                <span className="mono">{term.abbreviation}</span>
              </Descriptions.Item>
              <Descriptions.Item label={TERM_FIELD_LABELS.words}>
                <Space size={[4, 8]} wrap>
                  {term.words?.map((word, index) => (
                    // 같은 단어가 여러 번 들어갈 수 있어 순서를 키에 넣는다.
                    <Tag key={`${index}-${word.id}`}>
                      {word.name} <span className="mono">{word.abbreviation}</span>
                    </Tag>
                  ))}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label={TERM_FIELD_LABELS.domainName}>{term.domainName}</Descriptions.Item>
              <Descriptions.Item label={TERM_FIELD_LABELS.dataType}>
                <span className="mono">{formatDataType(term.dataType, term.dataLength, term.dataScale)}</span>
              </Descriptions.Item>
              <Descriptions.Item label={TERM_FIELD_LABELS.description}>{term.description || '-'}</Descriptions.Item>
              <Descriptions.Item label="등록 일시">{formatDateTime(term.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="수정 일시">{formatDateTime(term.updatedAt)}</Descriptions.Item>
            </Descriptions>

            <Typography.Title level={5} className="section-title">
              변경 이력
            </Typography.Title>
            {term.id !== undefined && <ChangeHistoryTimeline targetType="TERM" targetId={term.id} fieldLabels={TERM_FIELD_LABELS} />}
          </>
        )
      )}
    </Drawer>
  )
}
