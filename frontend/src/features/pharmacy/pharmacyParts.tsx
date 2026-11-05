import { Input, Space, Tag, Typography } from 'antd'
import type { TagSpec } from './pharmacyFields'

/**
 * 코드값을 색 태그로 보여준다. 값이 없으면 '-'.
 *
 * @example codeTag(DISPENSING_STATUSES, record.status)
 */
export function codeTag<T extends string>(table: Record<T, TagSpec>, value: T | undefined) {
    return value ? <Tag color={table[value].color}>{table[value].label}</Tag> : '-'
}

interface DateRangeFilterProps {
    /** 시작일 (yyyy-MM-dd). 없으면 조건 없음 */
    from?: string
    /** 종료일 (yyyy-MM-dd). 없으면 조건 없음 */
    to?: string
    /** 입력칸 이름에 붙일 업무 이름 (예: '조제' → '조제 시작일') */
    label: string
    /** 바뀐 쪽만 넘긴다. 지우면 undefined */
    onChange: (patch: { from?: string; to?: string }) => void
}

/**
 * 목록 검색 줄의 기간 입력 (시작일 ~ 종료일). 양끝을 포함한다(서버가 종료일 다음 날 0시 미만으로 찾는다).
 * dayjs가 없어 Ant Design DatePicker 대신 브라우저 기본 날짜 입력을 쓴다.
 */
export function DateRangeFilter({ from, to, label, onChange }: DateRangeFilterProps) {
    return (
        <Space size={4}>
            <Input
                type="date"
                aria-label={`${label} 시작일`}
                className="date-input"
                value={from ?? ''}
                max={to}
                onChange={(event) => onChange({ from: event.target.value || undefined })}
            />
            <Typography.Text type="secondary">~</Typography.Text>
            <Input
                type="date"
                aria-label={`${label} 종료일`}
                className="date-input"
                value={to ?? ''}
                min={from}
                onChange={(event) => onChange({ to: event.target.value || undefined })}
            />
        </Space>
    )
}
