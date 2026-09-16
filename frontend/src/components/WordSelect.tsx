import { keepPreviousData } from '@tanstack/react-query'
import { Select, Tag } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import type { components } from '@/api/schema'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'

type WordResponse = components['schemas']['WordResponse']

interface WordSelectProps {
  /** 선택한 단어 ID */
  value?: number
  /**
   * 선택이 바뀌면 호출한다. 두 번째 인자로 단어 전체를 넘겨 이름·분류어 여부를 쓸 수 있게 한다.
   * Form.Item 안에서 쓰면 Form이 첫 번째 인자(ID)를 값으로 쓴다.
   */
  onChange?: (value: number | undefined, word: WordResponse | undefined) => void
  /** true면 분류어 단어만 검색한다 */
  classifierOnly?: boolean
  placeholder?: string
  allowClear?: boolean
  disabled?: boolean
  className?: string
}

/**
 * 단어 검색 선택. 입력한 글자로 서버에서 검색한다(단어명·영문약어·영문명, 최대 20건).
 *
 * 선택된 값이 지금 검색 결과에 없으면(주소에서 복원한 필터, 수정 화면의 기존 값) 단건 조회로 가져와
 * 숫자 ID 대신 "고객 (CUST)"처럼 보여준다. 단건 조회 캐시는 같은 단어를 조회하는 다른 화면(용어 미리보기)과 공유된다.
 */
export function WordSelect({
  value,
  onChange,
  classifierOnly = false,
  placeholder,
  allowClear,
  disabled,
  className,
}: WordSelectProps) {
  const [search, setSearch] = useState('')
  const keyword = useDebouncedValue(search.trim(), 300)
  const target = classifierOnly ? '분류어 단어' : '단어'

  const listQuery = $api.useQuery(
    'get',
    '/api/words',
    { params: { query: { classifier: classifierOnly ? true : undefined, keyword: keyword || undefined, size: 20 } } },
    { placeholderData: keepPreviousData },
  )
  const listed = listQuery.data?.content ?? []
  const selectedInList = value !== undefined && listed.some((word) => word.id === value)

  const selectedQuery = $api.useQuery(
    'get',
    '/api/words/{id}',
    { params: { path: { id: value ?? 0 } } },
    { enabled: value !== undefined && !selectedInList },
  )
  const words = selectedQuery.data && !selectedInList ? [selectedQuery.data, ...listed] : listed

  const options = words
    .filter((word) => word.id !== undefined)
    .map((word) => ({ value: word.id as number, label: `${word.name} (${word.abbreviation})`, word }))

  return (
    <Select
      value={value}
      placeholder={placeholder ?? `${target} 검색`}
      allowClear={allowClear}
      disabled={disabled}
      className={className}
      options={options}
      loading={listQuery.isFetching}
      // 서버가 검색하므로 화면에서 다시 거르지 않는다.
      showSearch={{ filterOption: false, onSearch: setSearch }}
      notFoundContent={listQuery.isFetching ? '검색 중…' : `${target}가 없습니다`}
      // 용어 구성처럼 일반 단어와 분류어가 섞인 목록에서는 분류어를 표시해 마지막 단어 고르기를 돕는다.
      optionRender={(option) => (
        <span>
          {option.label}
          {!classifierOnly && option.data.word.classifier && (
            <Tag color="geekblue" className="option-tag">
              분류어
            </Tag>
          )}
        </span>
      )}
      onChange={(next, option) => {
        setSearch('')
        onChange?.(next, Array.isArray(option) ? undefined : (option?.word as WordResponse | undefined))
      }}
    />
  )
}
