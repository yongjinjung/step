import { Alert, Empty, Skeleton, Space, Tag, Timeline, Typography } from 'antd'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import type { components } from '@/api/schema'
import { formatDateTime, formatSnapshotValue } from '@/utils/format'

type ChangeHistoryResponse = components['schemas']['ChangeHistoryResponse']
type HistoryTargetType = NonNullable<ChangeHistoryResponse['targetType']>

const CHANGE_TYPES = {
  CREATE: { label: '등록', color: 'green' },
  UPDATE: { label: '수정', color: 'blue' },
  DELETE: { label: '삭제', color: 'red' },
} as const

interface ChangeHistoryTimelineProps {
  targetType: HistoryTargetType
  targetId: number
  /** 필드 이름 → 화면 표시 이름. 없으면 필드 이름 그대로 보여준다 */
  fieldLabels?: Record<string, string>
}

/**
 * 대상 하나의 변경 이력 (최신순 최대 50건). 수정 이력은 달라진 필드의 이전 값 → 새 값을 보여준다.
 * 신청 승인으로 반영된 변경은 신청 번호를 함께 표시한다.
 */
export function ChangeHistoryTimeline({ targetType, targetId, fieldLabels = {} }: ChangeHistoryTimelineProps) {
  const historyQuery = $api.useQuery('get', '/api/histories', {
    params: { query: { targetType, targetId, size: 50 } },
  })

  if (historyQuery.isPending) {
    return <Skeleton active />
  }
  if (historyQuery.isError) {
    return <Alert type="error" showIcon title={errorMessage(historyQuery.error)} />
  }
  const histories = historyQuery.data.content ?? []
  if (histories.length === 0) {
    return <Empty description="변경 이력이 없습니다" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  }

  return (
    <Timeline
      items={histories.map((history) => {
        const changeType = CHANGE_TYPES[history.changeType ?? 'UPDATE']
        // before/after는 대상 스냅샷 JSON 객체다. 생성 타입(JsonNode)이 실제 모양과 달라 일반 객체로 다룬다.
        const before = (history.before ?? {}) as Record<string, unknown>
        const after = (history.after ?? {}) as Record<string, unknown>
        return {
          key: history.id,
          color: changeType.color,
          content: (
            <div>
              <Space size={6} wrap>
                <Tag color={changeType.color}>{changeType.label}</Tag>
                <Typography.Text type="secondary">
                  {formatDateTime(history.changedAt)} · {history.changedByName ?? history.changedByUsername ?? '시스템'}
                </Typography.Text>
                {history.requestId && <Tag>신청 #{history.requestId}</Tag>}
              </Space>
              {history.changeType === 'UPDATE' && (
                <ul className="history-changes">
                  {(history.changedFields ?? []).map((field) => (
                    <li key={field}>
                      <Typography.Text strong>{fieldLabels[field] ?? field}</Typography.Text>{' '}
                      <span className="history-value">{formatSnapshotValue(before[field])}</span> →{' '}
                      <span className="history-value">{formatSnapshotValue(after[field])}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ),
        }
      })}
    />
  )
}
