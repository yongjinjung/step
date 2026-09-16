import { Alert, App, Form, Input, Modal, Switch } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { ABBREVIATION_PATTERN, WORD_ERROR_FIELDS, type WordRequest, type WordResponse } from './wordFields'

type WordFormValues = WordRequest & { reason?: string }

interface WordFormModalProps {
  open: boolean
  /** 수정할 단어. null이면 등록 */
  word: WordResponse | null
  onClose: () => void
}

/**
 * 단어 등록·수정 모달.
 *
 * - ADMIN: 저장하면 바로 반영한다 (POST/PUT /api/words).
 * - USER: 같은 내용을 변경 신청으로 보낸다 (POST /api/requests/words). 신청 사유를 함께 받는다.
 *
 * 서버 오류는 가능한 한 해당 필드 아래에 보여준다: 형식 오류(fieldErrors)와 중복 같은 업무 오류(코드 → 필드).
 */
export function WordFormModal({ open, word, onClose }: WordFormModalProps) {
  const { user } = useAuth()
  const isAdmin = user?.isAdmin ?? false
  const isEdit = word?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<WordFormValues>()

  const createWord = $api.useMutation('post', '/api/words')
  const updateWord = $api.useMutation('put', '/api/words/{id}')
  const requestWord = $api.useMutation('post', '/api/requests/words')
  const saving = createWord.isPending || updateWord.isPending || requestWord.isPending

  // 열 때마다 수정 대상 값(또는 등록 기본값)으로 채운다.
  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (word) {
      form.setFieldsValue({
        name: word.name,
        abbreviation: word.abbreviation,
        englishName: word.englishName,
        description: word.description,
        classifier: word.classifier ?? false,
      })
    }
  }, [open, word, form])

  const showError = (error: unknown, fromRequest: boolean) => {
    if (applyCodeError(form, error, WORD_ERROR_FIELDS)) {
      return
    }
    if (applyFieldErrors(form, error, fromRequest ? { stripPrefix: 'payload.' } : {})) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = ({ reason, ...values }: WordFormValues) => {
    const body: WordRequest = { ...values, description: values.description?.trim() || undefined }

    if (!isAdmin) {
      requestWord.mutate(
        {
          body: {
            requestType: isEdit ? 'UPDATE' : 'CREATE',
            targetId: isEdit ? word?.id : undefined,
            payload: body,
            reason: reason?.trim() || undefined,
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
      message.success(isEdit ? '단어를 수정했습니다.' : '단어를 등록했습니다.')
      invalidate('/api/words', '/api/words/{id}', '/api/histories')
      onClose()
    }
    if (isEdit && word?.id !== undefined) {
      updateWord.mutate({ params: { path: { id: word.id } }, body }, { onSuccess, onError: (error: unknown) => showError(error, false) })
    } else {
      createWord.mutate({ body }, { onSuccess, onError: (error: unknown) => showError(error, false) })
    }
  }

  const action = isEdit ? '수정' : '등록'

  return (
    <Modal
      open={open}
      title={isAdmin ? `단어 ${action}` : `단어 ${action} 신청`}
      okText={isAdmin ? '저장' : '신청'}
      confirmLoading={saving}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={560}
    >
      {!isAdmin && (
        <Alert
          type="info"
          showIcon
          className="form-notice"
          title="변경 신청으로 접수되며, 관리자가 승인하면 반영됩니다."
        />
      )}
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ classifier: false }}>
        <Form.Item
          name="name"
          label="단어명"
          rules={[
            { required: true, whitespace: true, message: '단어명을 입력하세요.' },
            { max: 100, message: '100자 이내로 입력하세요.' },
          ]}
        >
          <Input placeholder="예: 고객" maxLength={100} />
        </Form.Item>
        <Form.Item
          name="abbreviation"
          label="영문약어"
          extra="영문자로 시작하는 영문/숫자이며 대문자로 저장됩니다. 용어 영문약어를 만들 때 쓰입니다."
          // 서버가 대문자로 저장하므로 입력할 때부터 대문자로 보여준다.
          normalize={(value?: string) => value?.toUpperCase()}
          rules={[
            { required: true, message: '영문약어를 입력하세요.' },
            { max: 50, message: '50자 이내로 입력하세요.' },
            { pattern: ABBREVIATION_PATTERN, message: '영문자로 시작하는 영문/숫자만 사용할 수 있습니다.' },
          ]}
        >
          <Input placeholder="예: CUST" maxLength={50} className="mono" />
        </Form.Item>
        <Form.Item
          name="englishName"
          label="영문명"
          rules={[
            { required: true, whitespace: true, message: '영문명을 입력하세요.' },
            { max: 200, message: '200자 이내로 입력하세요.' },
          ]}
        >
          <Input placeholder="예: Customer" maxLength={200} />
        </Form.Item>
        <Form.Item
          name="classifier"
          label="분류어 여부"
          valuePropName="checked"
          extra="분류어는 용어 끝에 붙어 데이터 성격을 나타냅니다 (번호, 명, 일자, 금액 등). 도메인은 분류어에만 연결됩니다."
        >
          <Switch checkedChildren="분류어" unCheckedChildren="일반" />
        </Form.Item>
        <Form.Item name="description" label="설명" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
          <Input.TextArea rows={3} maxLength={1000} showCount />
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
