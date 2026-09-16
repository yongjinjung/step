import { Space, Tag, Tooltip, Typography } from 'antd'
import type { ReactNode } from 'react'
import { formatDataType } from '@/utils/dataType'
import { COLUMN_ISSUE_LABELS, type MetaColumnResponse } from './tableFields'

/**
 * 컬럼 표의 공통 셀. 테이블 상세와 비표준 컬럼 화면이 같은 모양으로 검출 결과를 보여주도록 한다.
 * 검출 결과(standard, issues, expected)는 서버가 조회할 때마다 현재 용어·도메인으로 계산한 값이다.
 */

interface MismatchValueProps {
  mismatch?: boolean
  /** 매핑 용어 기준 표준 값 */
  expected?: string
  children: ReactNode
}

/** 표준과 다른 값은 빨간 글자로 보여주고, 마우스를 올리면 표준 값을 알려준다. */
export function MismatchValue({ mismatch, expected, children }: MismatchValueProps) {
  if (!mismatch) {
    return <>{children}</>
  }
  return (
    <Tooltip title={`표준: ${expected ?? '-'}`}>
      <Typography.Text type="danger">{children}</Typography.Text>
    </Tooltip>
  )
}

/** 물리명. 용어 영문약어와 다르면(NAME_MISMATCH) 표시한다. */
export function ColumnPhysicalName({ column }: { column: MetaColumnResponse }) {
  return (
    <MismatchValue mismatch={column.issues?.includes('NAME_MISMATCH')} expected={column.expected?.physicalName}>
      <span className="mono">{column.physicalName}</span>
    </MismatchValue>
  )
}

/** 데이터 타입. 도메인과 타입 또는 길이·소수점이 다르면 표시한다. */
export function ColumnDataType({ column }: { column: MetaColumnResponse }) {
  const { expected } = column
  return (
    <MismatchValue
      mismatch={column.issues?.includes('TYPE_MISMATCH') || column.issues?.includes('LENGTH_MISMATCH')}
      expected={expected && formatDataType(expected.dataType, expected.dataLength, expected.dataScale)}
    >
      <span className="mono">{formatDataType(column.dataType, column.dataLength, column.dataScale)}</span>
    </MismatchValue>
  )
}

/** 검출 결과 태그. 용어 미매핑은 기준이 없는 상태라 불일치(빨강)와 색을 나눈다. */
export function ColumnIssueTags({ column }: { column: MetaColumnResponse }) {
  if (column.standard) {
    return <Tag color="green">표준</Tag>
  }
  return (
    <Space size={[0, 4]} wrap>
      {column.issues?.map((issue) => (
        <Tag key={issue} color={issue === 'NO_TERM' ? 'orange' : 'red'}>
          {COLUMN_ISSUE_LABELS[issue]}
        </Tag>
      ))}
    </Space>
  )
}
