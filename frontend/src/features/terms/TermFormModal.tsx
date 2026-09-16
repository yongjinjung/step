import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { useQueries } from '@tanstack/react-query'
import { Alert, App, Button, Descriptions, Form, Input, Modal, Typography } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { DomainSelect } from '@/components/DomainSelect'
import { WordSelect } from '@/components/WordSelect'
import type { WordResponse } from '@/features/words/wordFields'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import {
  composeTermAbbreviation,
  composeTermName,
  MAX_TERM_ABBREVIATION_LENGTH,
  MAX_TERM_NAME_LENGTH,
  MAX_TERM_WORDS,
  TERM_ERROR_FIELDS,
  type TermRequest,
  type TermResponse,
} from './termFields'

interface TermFormValues {
  /** 조합 순서대로의 구성 단어 ID. 아직 고르지 않은 줄은 undefined */
  wordIds?: (number | undefined)[]
  domainId?: number
  description?: string
  reason?: string
}

interface TermFormModalProps {
  open: boolean
  /** 수정할 용어. null이면 등록 */
  term: TermResponse | null
  onClose: () => void
}

/** 등록 화면은 빈 단어 한 줄로 시작한다. */
const INITIAL_VALUES: TermFormValues = { wordIds: [undefined] }

/**
 * 용어 등록·수정 모달.
 *
 * - 용어명·영문약어는 입력받지 않는다. 고른 단어로 서버와 같은 규칙으로 미리 보여준다.
 * - 입력 순서: 구성 단어(순서 변경 가능) → 도메인. 도메인 목록은 마지막 단어(분류어)의 도메인만 보여준다.
 * - 조합 규칙(마지막 단어 분류어, 도메인 분류어 일치, 길이)은 미리보기에서 경고하고, 최종 검증은 서버가 한다.
 * - ADMIN은 바로 저장, USER는 변경 신청으로 보낸다.
 */
export function TermFormModal({ open, term, onClose }: TermFormModalProps) {
  const { user } = useAuth()
  const isAdmin = user?.isAdmin ?? false
  const isEdit = term?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<TermFormValues>()

  const wordIds = Form.useWatch('wordIds', form) ?? []
  const domainId = Form.useWatch('domainId', form)

  // 미리보기에 필요한 단어명·약어·분류어 여부를 줄마다 단건 조회한다.
  // WordSelect의 단건 조회와 조회 키가 같아 이미 불러온 단어는 다시 요청하지 않는다.
  const wordQueries = useQueries({
    queries: wordIds.map((id) => ({
      ...$api.queryOptions('get', '/api/words/{id}', { params: { path: { id: id ?? 0 } } }),
      enabled: open && id !== undefined,
    })),
  })
  const words: (WordResponse | undefined)[] = wordQueries.map((query) => query.data)
  // 모든 줄에 단어를 골랐고 정보도 불러왔을 때만 이름을 만든다. 일부만 보여주면 잘못된 이름으로 오해할 수 있다.
  const wordsReady = wordIds.length > 0 && words.every((word, index) => wordIds[index] !== undefined && word !== undefined)
  const termName = wordsReady ? composeTermName(words as WordResponse[]) : ''
  const termAbbreviation = wordsReady ? composeTermAbbreviation(words as WordResponse[]) : ''
  const lastWord = words.at(-1)
  const lastClassifier = lastWord?.classifier ? lastWord : undefined

  const domainQuery = $api.useQuery(
    'get',
    '/api/domains/{id}',
    { params: { path: { id: domainId ?? 0 } } },
    { enabled: open && domainId !== undefined },
  )
  const domain = domainId !== undefined ? domainQuery.data : undefined

  /** 서버에서 거절될 조합을 저장 전에 알려준다 (TermService의 검사와 같은 규칙). */
  const issues: string[] = []
  if (lastWord && !lastWord.classifier) {
    issues.push(`마지막 단어는 분류어여야 합니다. '${lastWord.name}'은(는) 분류어가 아닙니다.`)
  }
  if (termName.length > MAX_TERM_NAME_LENGTH) {
    issues.push(`용어명이 ${MAX_TERM_NAME_LENGTH}자를 넘습니다 (${termName.length}자).`)
  }
  if (termAbbreviation.length > MAX_TERM_ABBREVIATION_LENGTH) {
    issues.push(`영문약어가 ${MAX_TERM_ABBREVIATION_LENGTH}자를 넘습니다 (${termAbbreviation.length}자).`)
  }
  if (lastClassifier && domain && domain.classifierWordId !== lastClassifier.id) {
    issues.push(
      `도메인 '${domain.name}'의 분류어는 '${domain.classifierWordName}'입니다. 마지막 단어 '${lastClassifier.name}'의 도메인을 고르세요.`,
    )
  }

  const createTerm = $api.useMutation('post', '/api/terms')
  const updateTerm = $api.useMutation('put', '/api/terms/{id}')
  const requestTerm = $api.useMutation('post', '/api/requests/terms')
  const saving = createTerm.isPending || updateTerm.isPending || requestTerm.isPending

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (term) {
      form.setFieldsValue({
        wordIds: term.words?.map((word) => word.id),
        domainId: term.domainId,
        description: term.description,
      })
    }
  }, [open, term, form])

  const showError = (error: unknown, fromRequest: boolean) => {
    if (applyCodeError(form, error, TERM_ERROR_FIELDS)) {
      return
    }
    if (applyFieldErrors(form, error, fromRequest ? { stripPrefix: 'payload.' } : {})) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = (values: TermFormValues) => {
    const body: TermRequest = {
      // 줄마다 필수 검사를 통과했으므로 빈 값이 없다.
      wordIds: (values.wordIds ?? []) as number[],
      domainId: values.domainId as number,
      description: values.description?.trim() || undefined,
    }

    if (!isAdmin) {
      requestTerm.mutate(
        {
          body: {
            requestType: isEdit ? 'UPDATE' : 'CREATE',
            targetId: isEdit ? term?.id : undefined,
            payload: body,
            reason: values.reason?.trim() || undefined,
          },
        },
        {
          onSuccess: () => {
            message.success('신청했습니다. 관리자가 승인하면 반영됩니다.')
            invalidate('/api/requests')
            onClose()
          },
          onError: (error: unknown) => showError(error, true),
        },
      )
      return
    }

    const onSuccess = () => {
      message.success(isEdit ? '용어를 수정했습니다.' : '용어를 등록했습니다.')
      // 컬럼의 비표준 검출은 용어 영문약어·도메인 기준이라 컬럼 조회도 새로 불러온다.
      invalidate('/api/terms', '/api/terms/{id}', '/api/histories', '/api/tables/{tableId}/columns', '/api/columns/non-standard')
      onClose()
    }
    if (isEdit && term?.id !== undefined) {
      updateTerm.mutate({ params: { path: { id: term.id } }, body }, { onSuccess, onError: (error: unknown) => showError(error, false) })
    } else {
      createTerm.mutate({ body }, { onSuccess, onError: (error: unknown) => showError(error, false) })
    }
  }

  const action = isEdit ? '수정' : '등록'

  return (
    <Modal
      open={open}
      title={isAdmin ? `용어 ${action}` : `용어 ${action} 신청`}
      okText={isAdmin ? '저장' : '신청'}
      confirmLoading={saving}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={640}
    >
      {!isAdmin && (
        <Alert type="info" showIcon className="form-notice" title="변경 신청으로 접수되며, 관리자가 승인하면 반영됩니다." />
      )}
      {isEdit && (
        <Alert
          type="warning"
          showIcon
          className="form-notice"
          title="구성 단어를 바꾸면 용어명·영문약어가 다시 만들어지고, 이 용어에 매핑된 컬럼은 새 영문약어 기준으로 비표준 여부가 검출됩니다."
        />
      )}
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={INITIAL_VALUES}>
        <Form.List
          name="wordIds"
          rules={[
            {
              validator: async (_, ids?: unknown[]) => {
                if (!ids?.length) {
                  throw new Error('구성 단어를 하나 이상 추가하세요.')
                }
              },
            },
          ]}
        >
          {(fields, { add, remove, move }, { errors }) => (
            <Form.Item
              label="구성 단어"
              required
              extra="조합 순서대로 고릅니다. 마지막 단어는 분류어여야 하고, 같은 단어를 여러 번 쓸 수 있습니다."
            >
              {fields.map((field, index) => (
                <div key={field.key} className="term-word-row">
                  <span className="term-word-seq">{index + 1}</span>
                  <Form.Item name={field.name} className="term-word-select" rules={[{ required: true, message: '단어를 선택하세요.' }]}>
                    <WordSelect />
                  </Form.Item>
                  <Button
                    type="text"
                    icon={<ArrowUpOutlined />}
                    aria-label="앞으로"
                    disabled={index === 0}
                    onClick={() => move(index, index - 1)}
                  />
                  <Button
                    type="text"
                    icon={<ArrowDownOutlined />}
                    aria-label="뒤로"
                    disabled={index === fields.length - 1}
                    onClick={() => move(index, index + 1)}
                  />
                  <Button
                    type="text"
                    danger
                    icon={<DeleteOutlined />}
                    aria-label="빼기"
                    disabled={fields.length === 1}
                    onClick={() => remove(field.name)}
                  />
                </div>
              ))}
              <Button type="dashed" block icon={<PlusOutlined />} disabled={fields.length >= MAX_TERM_WORDS} onClick={() => add()}>
                단어 추가 ({fields.length}/{MAX_TERM_WORDS})
              </Button>
              <Form.ErrorList errors={errors} />
            </Form.Item>
          )}
        </Form.List>

        <Form.Item
          label="생성될 용어"
          extra={issues.map((issue) => (
            <div key={issue}>
              <Typography.Text type="warning">{issue}</Typography.Text>
            </div>
          ))}
        >
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="용어명">
              {termName || <Typography.Text type="secondary">단어를 모두 고르면 표시됩니다</Typography.Text>}
            </Descriptions.Item>
            <Descriptions.Item label="영문약어">
              {termAbbreviation ? (
                <span className="mono">{termAbbreviation}</span>
              ) : (
                <Typography.Text type="secondary">-</Typography.Text>
              )}
            </Descriptions.Item>
          </Descriptions>
        </Form.Item>

        <Form.Item
          name="domainId"
          label="도메인"
          extra={
            lastClassifier
              ? `분류어 '${lastClassifier.name}'의 도메인만 보여줍니다.`
              : '마지막 단어로 분류어를 고르면 그 분류어의 도메인을 고를 수 있습니다.'
          }
          rules={[{ required: true, message: '도메인을 선택하세요.' }]}
        >
          <DomainSelect classifierWordId={lastClassifier?.id} disabled={!lastClassifier} />
        </Form.Item>

        <Form.Item name="description" label="설명" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
          <Input.TextArea rows={2} maxLength={1000} showCount />
        </Form.Item>

        {!isAdmin && (
          <Form.Item name="reason" label="신청 사유" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
            <Input.TextArea rows={2} maxLength={1000} showCount placeholder="관리자가 검토할 때 참고할 내용 (선택)" />
          </Form.Item>
        )}
      </Form>
    </Modal>
  )
}
