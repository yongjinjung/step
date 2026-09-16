import { keepPreviousData } from '@tanstack/react-query'
import { Select } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'

interface TableSelectProps {
  /** 선택한 테이블 정의 ID */
  value?: number
  onChange?: (value: number | undefined) => void
  placeholder?: string
  allowClear?: boolean
  className?: string
}

/**
 * 테이블 정의 검색 선택. 입력한 글자로 서버에서 물리명·논리명을 검색한다(최대 20건).
 * 선택된 값이 검색 결과에 없으면(주소에서 복원한 필터) 단건 조회로 가져와 물리명으로 보여준다.
 */
export function TableSelect({ value, onChange, placeholder = '테이블 검색', allowClear, className }: TableSelectProps) {
  const [search, setSearch] = useState('')
  const keyword = useDebouncedValue(search.trim(), 300)

  const listQuery = $api.useQuery(
    'get',
    '/api/tables',
    { params: { query: { keyword: keyword || undefined, size: 20 } } },
    { placeholderData: keepPreviousData },
  )
  const listed = listQuery.data?.content ?? []
  const selectedInList = value !== undefined && listed.some((table) => table.id === value)

  const selectedQuery = $api.useQuery(
    'get',
    '/api/tables/{id}',
    { params: { path: { id: value ?? 0 } } },
    { enabled: value !== undefined && !selectedInList },
  )
  const tables = selectedQuery.data && !selectedInList ? [selectedQuery.data, ...listed] : listed

  const options = tables
    .filter((table) => table.id !== undefined)
    .map((table) => ({ value: table.id as number, label: `${table.physicalName} (${table.logicalName})` }))

  return (
    <Select
      value={value}
      placeholder={placeholder}
      allowClear={allowClear}
      className={className}
      options={options}
      loading={listQuery.isFetching}
      // 서버가 검색하므로 화면에서 다시 거르지 않는다.
      showSearch={{ filterOption: false, onSearch: setSearch }}
      notFoundContent={listQuery.isFetching ? '검색 중…' : '테이블이 없습니다'}
      onChange={(next) => {
        setSearch('')
        onChange?.(next)
      }}
    />
  )
}
