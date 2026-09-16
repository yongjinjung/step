import { useQueries } from '@tanstack/react-query'
import { Alert, Skeleton, Table, Typography, type TableColumnsType } from 'antd'
import { $api } from '@/api/client'
import type { DomainRequest } from '@/features/domains/domainFields'
import { composeTermAbbreviation, composeTermName, type TermRequest } from '@/features/terms/termFields'
import type { WordRequest, WordResponse } from '@/features/words/wordFields'
import { formatDataType } from '@/utils/dataType'
import { payloadOf, type ChangeRequestResponse, type RequestTargetType } from './requestFields'

/** 비교 표의 한 항목 값. 없거나 빈 값은 undefined */
type FieldValues = Record<string, string | undefined>

interface CompareRow {
  key: string
  label: string
  current?: string
  requested?: string
}

/** 대상별 비교 항목. 참조 ID(분류어 단어, 구성 단어, 도메인)는 이름으로 바꿔 보여준다 */
const FIELDS: Record<RequestTargetType, { key: string; label: string }[]> = {
  WORD: [
    { key: 'name', label: '단어명' },
    { key: 'abbreviation', label: '영문약어' },
    { key: 'englishName', label: '영문명' },
    { key: 'classifier', label: '분류어 여부' },
    { key: 'description', label: '설명' },
  ],
  DOMAIN: [
    { key: 'name', label: '도메인명' },
    { key: 'classifier', label: '분류어' },
    { key: 'dataType', label: '데이터 타입' },
    { key: 'description', label: '설명' },
  ],
  TERM: [
    { key: 'words', label: '구성 단어' },
    { key: 'name', label: '용어명' },
    { key: 'abbreviation', label: '영문약어' },
    { key: 'domain', label: '도메인' },
    { key: 'description', label: '설명' },
  ],
}

/** 글자 모양이 중요한 항목 */
const MONO_KEYS = new Set(['abbreviation', 'dataType'])

const textOf = (value?: string | null) => value || undefined

function wordValues(word: WordRequest | WordResponse): FieldValues {
  return {
    name: word.name,
    abbreviation: word.abbreviation,
    englishName: word.englishName,
    classifier: word.classifier === undefined ? undefined : word.classifier ? '분류어' : '일반',
    description: textOf(word.description),
  }
}

/**
 * 신청 내용 비교 표.
 *
 * - 대기 중인 수정·삭제 신청: 대상의 현재 값을 조회해 신청 값과 나란히 보여주고, 달라지는 항목을 강조한다.
 * - 등록 신청: 신청 값만 보여준다. 용어는 구성 단어로 만들어질 용어명·영문약어를 함께 계산한다.
 * - 처리가 끝난 신청: 현재 값은 이미 반영·변경된 값이라 비교가 의미 없으므로 신청 당시 내용만 보여준다.
 */
export function RequestPayloadCompare({ request }: { request: ChangeRequestResponse }) {
  const targetType = request.targetType ?? 'WORD'
  const { requestType, targetId, status } = request
  const payload = requestType === 'DELETE' ? undefined : payloadOf(request)
  const showCurrent = requestType !== 'CREATE' && status === 'REQUESTED' && targetId !== undefined && targetId !== null
  const currentPath = { params: { path: { id: targetId ?? 0 } } }

  // 현재 대상 조회. 훅은 항상 같은 순서로 부르고 대상 종류에 맞는 것만 켠다.
  const currentWord = $api.useQuery('get', '/api/words/{id}', currentPath, { enabled: showCurrent && targetType === 'WORD' })
  const currentDomain = $api.useQuery('get', '/api/domains/{id}', currentPath, { enabled: showCurrent && targetType === 'DOMAIN' })
  const currentTerm = $api.useQuery('get', '/api/terms/{id}', currentPath, { enabled: showCurrent && targetType === 'TERM' })
  const currentQuery = targetType === 'WORD' ? currentWord : targetType === 'DOMAIN' ? currentDomain : currentTerm

  const wordPayload = targetType === 'WORD' ? (payload as WordRequest | undefined) : undefined
  const domainPayload = targetType === 'DOMAIN' ? (payload as DomainRequest | undefined) : undefined
  const termPayload = targetType === 'TERM' ? (payload as TermRequest | undefined) : undefined

  // 신청 내용의 참조 ID를 이름으로 바꾸기 위한 조회. 단건 조회 캐시는 다른 화면과 공유된다.
  const lookupWordIds = [
    ...new Set([...(termPayload?.wordIds ?? []), ...(domainPayload?.classifierWordId ? [domainPayload.classifierWordId] : [])]),
  ]
  const wordLookups = useQueries({
    queries: lookupWordIds.map((id) => $api.queryOptions('get', '/api/words/{id}', { params: { path: { id } } })),
  })
  const lookupById = new Map(lookupWordIds.map((id, index) => [id, wordLookups[index]]))
  const domainLookup = $api.useQuery(
    'get',
    '/api/domains/{id}',
    { params: { path: { id: termPayload?.domainId ?? 0 } } },
    { enabled: termPayload?.domainId !== undefined },
  )
  /** 참조 단어 이름. 승인 전에 단어가 삭제됐으면 번호로 보여준다 */
  const wordLabel = (id: number) => {
    const lookup = lookupById.get(id)
    return lookup?.data?.name ?? (lookup?.isError ? `#${id} (없는 단어)` : `#${id}`)
  }

  let requested: FieldValues | undefined
  if (wordPayload) {
    requested = wordValues(wordPayload)
  } else if (domainPayload) {
    requested = {
      name: domainPayload.name,
      classifier: wordLabel(domainPayload.classifierWordId),
      dataType: formatDataType(domainPayload.dataType, domainPayload.dataLength, domainPayload.dataScale),
      description: textOf(domainPayload.description),
    }
  } else if (termPayload) {
    const words = termPayload.wordIds.map((id) => lookupById.get(id)?.data)
    const ready = words.every((word) => word !== undefined)
    requested = {
      words: termPayload.wordIds.map(wordLabel).join(' + '),
      name: ready ? composeTermName(words as WordResponse[]) : undefined,
      abbreviation: ready ? composeTermAbbreviation(words as WordResponse[]) : undefined,
      domain: domainLookup.data?.name ?? `#${termPayload.domainId}`,
      description: textOf(termPayload.description),
    }
  }

  let current: FieldValues | undefined
  if (showCurrent) {
    if (targetType === 'WORD' && currentWord.data) {
      current = wordValues(currentWord.data)
    } else if (targetType === 'DOMAIN' && currentDomain.data) {
      const domain = currentDomain.data
      current = {
        name: domain.name,
        classifier: domain.classifierWordName,
        dataType: formatDataType(domain.dataType, domain.dataLength, domain.dataScale),
        description: textOf(domain.description),
      }
    } else if (targetType === 'TERM' && currentTerm.data) {
      const term = currentTerm.data
      current = {
        words: term.words?.map((word) => word.name).join(' + '),
        name: term.name,
        abbreviation: term.abbreviation,
        domain: term.domainName,
        description: textOf(term.description),
      }
    }
  }

  const rows: CompareRow[] = FIELDS[targetType].map((field) => ({
    ...field,
    current: current?.[field.key],
    requested: requested?.[field.key],
  }))
  const isChanged = (row: CompareRow) => current !== undefined && requested !== undefined && (row.current ?? '') !== (row.requested ?? '')

  const renderValue = (value: string | undefined, row: CompareRow, emphasize = false) => {
    if (!value) {
      return <Typography.Text type="secondary">-</Typography.Text>
    }
    return (
      <Typography.Text strong={emphasize} className={MONO_KEYS.has(row.key) ? 'mono' : undefined}>
        {value}
      </Typography.Text>
    )
  }

  const columns: TableColumnsType<CompareRow> = [{ title: '항목', dataIndex: 'label', width: 110 }]
  if (showCurrent) {
    columns.push({ title: '현재 값', key: 'current', render: (_, row) => renderValue(row.current, row) })
  }
  if (payload) {
    columns.push({
      title: requestType === 'UPDATE' && showCurrent ? '신청 값' : '신청 내용',
      key: 'requested',
      render: (_, row) => renderValue(row.requested, row, isChanged(row)),
    })
  }

  if (showCurrent && currentQuery.isError) {
    return (
      <Alert
        type="warning"
        showIcon
        title={`대상 #${targetId}을(를) 찾을 수 없습니다.`}
        description="신청 이후 삭제되었을 수 있습니다. 이 상태로는 승인할 수 없으니 반려하세요."
      />
    )
  }
  if (showCurrent && currentQuery.isPending) {
    return <Skeleton active paragraph={{ rows: 3 }} />
  }
  if (columns.length === 1) {
    // 처리가 끝난 삭제 신청: 보여줄 값이 없다.
    return <Typography.Text type="secondary">대상 #{targetId} 삭제 신청입니다.</Typography.Text>
  }

  return (
    <>
      {requestType !== 'CREATE' && status !== 'REQUESTED' && (
        <Typography.Paragraph type="secondary">처리가 끝난 신청이라 신청 당시 내용만 보여줍니다.</Typography.Paragraph>
      )}
      {requestType === 'DELETE' && showCurrent && (
        <Typography.Paragraph type="secondary">승인하면 아래 대상이 삭제됩니다.</Typography.Paragraph>
      )}
      <Table<CompareRow>
        rowKey="key"
        size="small"
        bordered
        pagination={false}
        className="compare-table"
        columns={columns}
        dataSource={rows}
        rowClassName={(row) => (isChanged(row) ? 'row-changed' : '')}
      />
    </>
  )
}
