import { keepPreviousData } from '@tanstack/react-query'
import { Select, Tag, Typography } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import type { components } from '@/api/schema'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'

type CodeGroupResponse = components['schemas']['CodeGroupResponse']

interface CodeGroupSelectProps {
  /** 선택한 코드 그룹 ID */
  value?: number
  onChange?: (value: number | undefined, group: CodeGroupResponse | undefined) => void
  placeholder?: string
  allowClear?: boolean
  disabled?: boolean
  className?: string
}

/**
 * 공통 코드 그룹 검색 선택. 입력한 글자로 서버에서 코드ID·코드명을 검색한다(사용 중인 그룹만, 최대 20건).
 *
 * 미사용 그룹은 새로 매핑할 수 없으므로(서버 CODE_GROUP_NOT_USED) 검색 결과에서 뺀다.
 * 이미 매핑된 값이 검색 결과에 없으면(미사용으로 바뀐 그룹 포함) 단건 조회로 가져와 "미사용" 표시와 함께 보여준다.
 */
export function CodeGroupSelect({ value, onChange, placeholder = '코드 그룹 검색', allowClear, disabled, className }: CodeGroupSelectProps) {
  const [search, setSearch] = useState('')
  const keyword = useDebouncedValue(search.trim(), 300)

  const listQuery = $api.useQuery(
    'get',
    '/api/code-groups',
    { params: { query: { keyword: keyword || undefined, used: true, size: 20 } } },
    { placeholderData: keepPreviousData, enabled: !disabled },
  )
  const listed = listQuery.data?.content ?? []
  const selectedInList = value !== undefined && listed.some((group) => group.id === value)

  const selectedQuery = $api.useQuery(
    'get',
    '/api/code-groups/{id}',
    { params: { path: { id: value ?? 0 } } },
    { enabled: value !== undefined && !selectedInList },
  )
  const groups = selectedQuery.data && !selectedInList ? [selectedQuery.data, ...listed] : listed

  const options = groups
    .filter((group) => group.id !== undefined)
    .map((group) => ({ value: group.id as number, label: `${group.codeId} (${group.codeName})`, group }))

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
      notFoundContent={listQuery.isFetching ? '검색 중…' : '사용 중인 코드 그룹이 없습니다'}
      optionRender={(option) => {
        const group = option.data.group as CodeGroupResponse
        return (
          <span>
            <span className="mono">{group.codeId}</span>{' '}
            <Typography.Text type="secondary" className="option-hint">
              {group.codeName}
            </Typography.Text>
            {!group.used && (
              <Tag className="option-tag" color="default">
                미사용
              </Tag>
            )}
          </span>
        )
      }}
      onChange={(next, option) => {
        setSearch('')
        onChange?.(next, Array.isArray(option) ? undefined : (option?.group as CodeGroupResponse | undefined))
      }}
    />
  )
}
