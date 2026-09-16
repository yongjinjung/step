import { Table, Typography, type TableColumnsType } from 'antd'
import { formatSnapshotValue } from '@/utils/format'
import { HISTORY_FIELD_LABELS, snapshotOf, type ChangeHistoryResponse } from './historyFields'

interface DetailRow {
  key: string
  label: string
  before?: unknown
  after?: unknown
}

/** 펼친 행에서는 긴 설명도 끝까지 보여준다 */
const DETAIL_MAX_LENGTH = 1000

/**
 * 이력 한 건의 변경 내용 표 (목록의 펼친 행).
 *
 * - 수정: 서버가 계산한 변경 필드(changedFields)만 이전 값 → 변경 값으로 보여준다.
 * - 등록: 등록된 스냅샷 전체, 삭제: 삭제 직전 스냅샷 전체. ID는 목록에 있으므로 뺀다.
 */
export function HistoryChangeDetail({ history }: { history: ChangeHistoryResponse }) {
  const labels = HISTORY_FIELD_LABELS[history.targetType ?? 'WORD']
  const before = snapshotOf(history.before)
  const after = snapshotOf(history.after)
  const snapshot = after ?? before ?? {}
  const isUpdate = history.changeType === 'UPDATE'

  // 등록·삭제는 필드 정의 순서대로, 정의에 없는 필드는 뒤에 붙인다.
  const keys = isUpdate
    ? (history.changedFields ?? [])
    : [...Object.keys(labels).filter((key) => key in snapshot), ...Object.keys(snapshot).filter((key) => !(key in labels))].filter(
        (key) => key !== 'id',
      )
  const rows: DetailRow[] = keys.map((key) => ({ key, label: labels[key] ?? key, before: before?.[key], after: after?.[key] }))

  if (isUpdate && rows.length === 0) {
    return <Typography.Text type="secondary">달라진 항목이 없습니다.</Typography.Text>
  }

  const renderValue = (value: unknown) => {
    const text = formatSnapshotValue(value, DETAIL_MAX_LENGTH)
    return value === null || value === undefined || value === '' ? (
      <Typography.Text type="secondary">{text}</Typography.Text>
    ) : (
      <span className="history-value">{text}</span>
    )
  }

  const columns: TableColumnsType<DetailRow> = [{ title: '항목', dataIndex: 'label', width: 140 }]
  if (before) {
    columns.push({ title: isUpdate ? '이전 값' : '삭제 전 값', key: 'before', render: (_, row) => renderValue(row.before) })
  }
  if (after) {
    columns.push({ title: isUpdate ? '변경 값' : '등록 값', key: 'after', render: (_, row) => renderValue(row.after) })
  }

  return <Table<DetailRow> rowKey="key" size="small" bordered pagination={false} columns={columns} dataSource={rows} />
}
