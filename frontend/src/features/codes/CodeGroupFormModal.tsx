import { App, Form, Input, Modal, Switch } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { CODE_GROUP_ERROR_FIELDS, CODE_ID_PATTERN, type CodeGroupRequest, type CodeGroupResponse } from './codeFields'

interface CodeGroupFormModalProps {
  open: boolean
  /** 수정할 코드 그룹. null이면 등록 */
  group: CodeGroupResponse | null
  onClose: () => void
  /** 저장에 성공하면 저장된 그룹으로 호출한다 (등록 후 상세 화면으로 이동 등) */
  onSaved?: (group: CodeGroupResponse, created: boolean) => void
}

/**
 * 코드 그룹 등록·수정 모달 (ADMIN 전용 화면에서만 연다).
 * 코드ID는 서버가 대문자로 저장하므로 입력할 때부터 대문자로 보여준다.
 */
export function CodeGroupFormModal({ open, group, onClose, onSaved }: CodeGroupFormModalProps) {
  const isEdit = group?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<CodeGroupRequest>()

  const createGroup = $api.useMutation('post', '/api/code-groups')
  const updateGroup = $api.useMutation('put', '/api/code-groups/{id}')

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (group) {
      form.setFieldsValue({ codeId: group.codeId, codeName: group.codeName, description: group.description, used: group.used })
    }
  }, [open, group, form])

  const showError = (error: unknown) => {
    if (applyCodeError(form, error, CODE_GROUP_ERROR_FIELDS) || applyFieldErrors(form, error)) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = (values: CodeGroupRequest) => {
    const body: CodeGroupRequest = {
      codeId: values.codeId.trim(),
      codeName: values.codeName.trim(),
      description: values.description?.trim() || undefined,
      used: values.used ?? true,
    }
    const onSuccess = (saved: CodeGroupResponse) => {
      message.success(isEdit ? '코드 그룹을 수정했습니다.' : '코드 그룹을 등록했습니다.')
      // 컬럼 응답에도 코드ID·코드명이 들어가므로 테이블 컬럼 목록도 새로 불러온다.
      invalidate('/api/code-groups', '/api/code-groups/{id}', '/api/histories', '/api/tables/{tableId}/columns')
      onSaved?.(saved, !isEdit)
      onClose()
    }
    if (isEdit && group?.id !== undefined) {
      updateGroup.mutate({ params: { path: { id: group.id } }, body }, { onSuccess, onError: showError })
    } else {
      createGroup.mutate({ body }, { onSuccess, onError: showError })
    }
  }

  return (
    <Modal
      open={open}
      title={isEdit ? '코드 그룹 수정' : '코드 그룹 등록'}
      okText="저장"
      confirmLoading={createGroup.isPending || updateGroup.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={560}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ used: true }}>
        <Form.Item
          name="codeId"
          label="코드ID"
          extra="영문자로 시작하는 영문/숫자/_이며 대문자로 저장됩니다. 컬럼은 ID로 연결되므로 바꿔도 매핑이 유지됩니다."
          normalize={(value?: string) => value?.toUpperCase()}
          rules={[
            { required: true, message: '코드ID를 입력하세요.' },
            { max: 50, message: '50자 이내로 입력하세요.' },
            { pattern: CODE_ID_PATTERN, message: '영문자로 시작하는 영문/숫자/_만 사용할 수 있습니다.' },
          ]}
        >
          <Input placeholder="예: ORDER_STAT" maxLength={50} className="mono" />
        </Form.Item>
        <Form.Item
          name="codeName"
          label="코드명"
          rules={[
            { required: true, whitespace: true, message: '코드명을 입력하세요.' },
            { max: 200, message: '200자 이내로 입력하세요.' },
          ]}
        >
          <Input placeholder="예: 주문상태" maxLength={200} />
        </Form.Item>
        <Form.Item
          name="used"
          label="사용 여부"
          valuePropName="checked"
          extra="미사용 그룹은 컬럼에 새로 매핑할 수 없습니다. 이미 매핑된 컬럼은 그대로 유지됩니다."
        >
          <Switch checkedChildren="사용" unCheckedChildren="미사용" />
        </Form.Item>
        <Form.Item name="description" label="설명" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
          <Input.TextArea rows={3} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </Modal>
  )
}
