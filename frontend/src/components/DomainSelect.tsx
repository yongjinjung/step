import { keepPreviousData } from '@tanstack/react-query'
import { Select, Typography } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import type { components } from '@/api/schema'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { formatDataType } from '@/utils/dataType'

type DomainResponse = components['schemas']['DomainResponse']

interface DomainSelectProps {
  /** 선택한 도메인 ID */
  value?: number
  /** 선택이 바뀌면 호출한다. 두 번째 인자로 도메인 전체(분류어·타입)를 넘긴다 */
  onChange?: (value: number | undefined, domain: DomainResponse | undefined) => void
  /** 이 분류어 단어의 도메인만 검색한다. 용어 폼에서 마지막 단어에 맞는 도메인만 보여줄 때 쓴다 */
  classifierWordId?: number
  placeholder?: string
  allowClear?: boolean
  disabled?: boolean
  className?: string
}

/**
 * 도메인 검색 선택. 입력한 글자로 서버에서 도메인명을 검색한다(최대 20건).
 *
 * 목록에는 분류어와 타입을 함께 보여준다. 선택된 값이 검색 결과에 없으면(URL 복원, 수정 화면,
 * 분류어 필터와 맞지 않는 기존 값) 단건 조회로 가져와 이름으로 보여준다.
 */
export function DomainSelect({
  value,
  onChange,
  classifierWordId,
  placeholder = '도메인 검색',
  allowClear,
  disabled,
  className,
}: DomainSelectProps) {
  const [search, setSearch] = useState('')
  const keyword = useDebouncedValue(search.trim(), 300)

  const listQuery = $api.useQuery(
    'get',
    '/api/domains',
    { params: { query: { classifierWordId, keyword: keyword || undefined, size: 20 } } },
    { placeholderData: keepPreviousData, enabled: !disabled },
  )
  const listed = listQuery.data?.content ?? []
  const selectedInList = value !== undefined && listed.some((domain) => domain.id === value)

  const selectedQuery = $api.useQuery(
    'get',
    '/api/domains/{id}',
    { params: { path: { id: value ?? 0 } } },
    { enabled: value !== undefined && !selectedInList },
  )
  const domains = selectedQuery.data && !selectedInList ? [selectedQuery.data, ...listed] : listed

  const options = domains
    .filter((domain) => domain.id !== undefined)
    .map((domain) => ({ value: domain.id as number, label: domain.name, domain }))

  return (
    <Select
      value={value}
      placeholder={placeholder}
      allowClear={allowClear}
      disabled={disabled}
      className={className}
      options={options}
      loading={listQuery.isFetching}
      showSearch={{ filterOption: false, onSearch: setSearch }}
      notFoundContent={listQuery.isFetching ? '검색 중…' : '도메인이 없습니다'}
      optionRender={(option) => {
        const domain = option.data.domain as DomainResponse
        return (
          <span>
            {domain.name}{' '}
            <Typography.Text type="secondary" className="option-hint">
              {domain.classifierWordName} · <span className="mono">{formatDataType(domain.dataType, domain.dataLength, domain.dataScale)}</span>
            </Typography.Text>
          </span>
        )
      }}
      onChange={(next, option) => {
        setSearch('')
        onChange?.(next, Array.isArray(option) ? undefined : (option?.domain as DomainResponse | undefined))
      }}
    />
  )
}
