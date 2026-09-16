import { App, Col, Form, Input, InputNumber, Modal, Row, Switch } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { CODE_VALUE_ERROR_FIELDS, CODE_VALUE_PATTERN, type CodeValueRequest, type CodeValueResponse } from './codeFields'

type CodeValueFormValues = Partial<CodeValueRequest>

interface CodeValueFormModalProps {
  open: boolean
  groupId: number
  /** 수정할 코드값. null이면 등록 */
  value: CodeValueResponse | null
  /** 등록할 때 채울 정렬 순서 (현재 마지막 순서 + 1) */
  nextSortOrder: number
  onClose: () => void
}

/** 코드값 등록·수정 모달. 코드값은 서버가 대문자로 저장하므로 입력할 때부터 대문자로 보여준다. */
export function CodeValueFormModal({ open, groupId, value, nextSortOrder, onClose }: CodeValueFormModalProps) {
  const isEdit = value?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<CodeValueFormValues>()

  const createValue = $api.useMutation('post', '/api/code-groups/{groupId}/values')
  const updateValue = $api.useMutation('put', '/api/code-groups/{groupId}/values/{valueId}')

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (value) {
      form.setFieldsValue({
        codeValue: value.codeValue,
        valueName: value.valueName,
        description: value.description,
        sortOrder: value.sortOrder,
        used: value.used,
      })
    } else {
      form.setFieldsValue({ sortOrder: nextSortOrder })
    }
    // nextSortOrder는 의존성에서 뺀다. 열 때의 값만 쓰고, 열린 동안 목록이 바뀌어도 입력 중인 순서를 바꾸지 않는다.
  }, [open, value, form])

  const showError = (error: unknown) => {
    if (applyCodeError(form, error, CODE_VALUE_ERROR_FIELDS) || applyFieldErrors(form, error)) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = (values: CodeValueFormValues) => {
    const body: CodeValueRequest = {
      codeValue: values.codeValue?.trim() ?? '',
      valueName: values.valueName?.trim() ?? '',
      description: values.description?.trim() || undefined,
      sortOrder: values.sortOrder as number,
      used: values.used ?? true,
    }
    const onSuccess = () => {
      message.success(isEdit ? '코드값을 수정했습니다.' : '코드값을 등록했습니다.')
      invalidate('/api/code-groups/{groupId}/values', '/api/code-groups/{groupId}/values/{valueId}', '/api/histories')
      onClose()
    }
    if (isEdit && value?.id !== undefined) {
      updateValue.mutate({ params: { path: { groupId, valueId: value.id } }, body }, { onSuccess, onError: showError })
    } else {
      createValue.mutate({ params: { path: { groupId } }, body }, { onSuccess, onError: showError })
    }
  }

  return (
    <Modal
      open={open}
      title={isEdit ? '코드값 수정' : '코드값 추가'}
      okText="저장"
      confirmLoading={createValue.isPending || updateValue.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={560}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ used: true }}>
        <Row gutter={12}>
          <Col xs={24} sm={10}>
            <Form.Item
              name="codeValue"
              label="코드값"
              normalize={(input?: string) => input?.toUpperCase()}
              rules={[
                { required: true, message: '코드값을 입력하세요.' },
                { max: 50, message: '50자 이내로 입력하세요.' },
                { pattern: CODE_VALUE_PATTERN, message: '영문/숫자/_/-만 사용할 수 있습니다.' },
              ]}
            >
              <Input placeholder="예: 01" maxLength={50} className="mono" />
            </Form.Item>
          </Col>
          <Col xs={24} sm={14}>
            <Form.Item
              name="valueName"
              label="코드값명"
              rules={[
                { required: true, whitespace: true, message: '코드값명을 입력하세요.' },
                { max: 200, message: '200자 이내로 입력하세요.' },
              ]}
            >
              <Input placeholder="예: 접수" maxLength={200} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={12}>
          <Col xs={12} sm={10}>
            <Form.Item name="sortOrder" label="정렬 순서" rules={[{ required: true, message: '정렬 순서를 입력하세요.' }]}>
              <InputNumber className="full-width" min={1} precision={0} />
            </Form.Item>
          </Col>
          <Col xs={12} sm={14}>
            <Form.Item name="used" label="사용 여부" valuePropName="checked">
              <Switch checkedChildren="사용" unCheckedChildren="미사용" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="description" label="설명" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
          <Input.TextArea rows={2} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </Modal>
  )
}
