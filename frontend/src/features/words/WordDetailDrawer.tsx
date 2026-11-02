import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { Alert, Button, Descriptions, Drawer, Skeleton, Space, Tag, Typography } from 'antd'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useMenus } from '@/layouts/MenuContext'
import { ChangeHistoryTimeline } from '@/components/ChangeHistoryTimeline'
import { formatDateTime } from '@/utils/format'
import { WORD_FIELD_LABELS, type WordResponse } from './wordFields'

interface WordDetailDrawerProps {
  /** 보여줄 단어 ID. null이면 닫힌다 */
  wordId: number | null
  onClose: () => void
  onEdit: (word: WordResponse) => void
  onDelete: (word: WordResponse) => void
}

/** 단어 상세와 변경 이력. 목록의 행 데이터 대신 단건 API로 최신 값을 불러온다. */
export function WordDetailDrawer({ wordId, onClose, onEdit, onDelete }: WordDetailDrawerProps) {
  const { can } = useMenus()
  const canUpdate = can('/words', 'update')
  const canDelete = can('/words', 'delete')
  const open = wordId !== null

  const wordQuery = $api.useQuery(
    'get',
    '/api/words/{id}',
    { params: { path: { id: wordId ?? 0 } } },
    { enabled: open },
  )
  const word = wordQuery.data

  return (
    <Drawer
      open={open}
      onClose={onClose}
      size="large"
      title={word?.name ?? '단어 상세'}
      extra={
        word && (
          <Space>
            <Button icon={<EditOutlined />} onClick={() => onEdit(word)}>
              {canUpdate ? '수정' : '수정 신청'}
            </Button>
            <Button danger icon={<DeleteOutlined />} onClick={() => onDelete(word)}>
              {canDelete ? '삭제' : '삭제 신청'}
            </Button>
          </Space>
        )
      }
    >
      {wordQuery.isPending && open ? (
        <Skeleton active />
      ) : wordQuery.isError ? (
        <Alert type="error" showIcon title={errorMessage(wordQuery.error)} />
      ) : (
        word && (
          <>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label={WORD_FIELD_LABELS.name}>{word.name}</Descriptions.Item>
              <Descriptions.Item label={WORD_FIELD_LABELS.abbreviation}>
                <span className="mono">{word.abbreviation}</span>
              </Descriptions.Item>
              <Descriptions.Item label={WORD_FIELD_LABELS.englishName}>{word.englishName}</Descriptions.Item>
              <Descriptions.Item label={WORD_FIELD_LABELS.classifier}>
                {word.classifier ? <Tag color="geekblue">분류어</Tag> : <Tag>일반</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label={WORD_FIELD_LABELS.description}>{word.description || '-'}</Descriptions.Item>
              <Descriptions.Item label="등록 일시">{formatDateTime(word.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="수정 일시">{formatDateTime(word.updatedAt)}</Descriptions.Item>
            </Descriptions>

            <Typography.Title level={5} className="section-title">
              변경 이력
            </Typography.Title>
            {word.id !== undefined && (
              <ChangeHistoryTimeline targetType="WORD" targetId={word.id} fieldLabels={WORD_FIELD_LABELS} />
            )}
          </>
        )
      )}
    </Drawer>
  )
}
