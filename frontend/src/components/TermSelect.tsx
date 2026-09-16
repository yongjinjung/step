import { keepPreviousData } from '@tanstack/react-query'
import { Select, Typography } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import type { components } from '@/api/schema'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { formatDataType } from '@/utils/dataType'

type TermResponse = components['schemas']['TermResponse']

interface TermSelectProps {
  /** 선택한 용어 ID */
  value?: number
  /** 선택이 바뀌면 호출한다. 두 번째 인자로 용어 전체(영문약어·도메인 타입)를 넘긴다 */
  onChange?: (value: number | undefined, term: TermResponse | undefined) => void
  placeholder?: string
  allowClear?: boolean
  disabled?: boolean
  className?: string
}

/**
 * 표준 용어 검색 선택. 입력한 글자로 서버에서 용어명·영문약어를 검색한다(최대 20건).
 *
 * 목록에는 도메인 타입을 함께 보여준다. 선택된 값이 검색 결과에 없으면(수정 화면의 기존 값) 단건 조회로 가져온다.
 * 단건 조회 캐시는 컬럼 폼의 표준 검출 미리보기와 공유된다.
 */
export function TermSelect({ value, onChange, placeholder = '용어 검색', allowClear, disabled, className }: TermSelectProps) {
  const [search, setSearch] = useState('')
  const keyword = useDebouncedValue(search.trim(), 300)

  const listQuery = $api.useQuery(
    'get',
    '/api/terms',
    { params: { query: { keyword: keyword || undefined, size: 20 } } },
    { placeholderData: keepPreviousData, enabled: !disabled },
  )
  const listed = listQuery.data?.content ?? []
  const selectedInList = value !== undefined && listed.some((term) => term.id === value)

  const selectedQuery = $api.useQuery(
    'get',
    '/api/terms/{id}',
    { params: { path: { id: value ?? 0 } } },
    { enabled: value !== undefined && !selectedInList },
  )
  const terms = selectedQuery.data && !selectedInList ? [selectedQuery.data, ...listed] : listed

  const options = terms
    .filter((term) => term.id !== undefined)
    .map((term) => ({ value: term.id as number, label: `${term.name} (${term.abbreviation})`, term }))

  return (
    <Select
      value={value}
      placeholder={placeholder}
      allowClear={allowClear}
      disabled={disabled}
      className={className}
      options={options}
      loading={listQuery.isFetching}
      // 서버가 검색하므로 화면에서 다시 거르지 않는다.
      showSearch={{ filterOption: false, onSearch: setSearch }}
      notFoundContent={listQuery.isFetching ? '검색 중…' : '용어가 없습니다'}
      optionRender={(option) => {
        const term = option.data.term as TermResponse
        return (
          <span>
            {option.label}{' '}
            <Typography.Text type="secondary" className="option-hint">
              {term.domainName} · <span className="mono">{formatDataType(term.dataType, term.dataLength, term.dataScale)}</span>
            </Typography.Text>
          </span>
        )
      }}
      onChange={(next, option) => {
        setSearch('')
        onChange?.(next, Array.isArray(option) ? undefined : (option?.term as TermResponse | undefined))
      }}
    />
  )
}
