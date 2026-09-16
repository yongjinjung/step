import { App, Form, Input, Modal } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import {
  PHYSICAL_NAME_MAX_LENGTH,
  PHYSICAL_NAME_PATTERN,
  TABLE_ERROR_FIELDS,
  type MetaTableRequest,
  type MetaTableResponse,
} from './tableFields'

interface TableFormModalProps {
  open: boolean
  /** 수정할 테이블. null이면 등록 */
  table: MetaTableResponse | null
  onClose: () => void
  /** 저장에 성공하면 저장된 테이블로 호출한다 (등록 후 상세 화면으로 이동 등) */
  onSaved?: (table: MetaTableResponse, created: boolean) => void
}

/**
 * 테이블 정의 등록·수정 모달. 테이블 정의는 역할과 관계없이 바로 반영한다(신청 없음).
 * 물리명은 서버가 대문자로 저장하므로 입력할 때부터 대문자로 보여준다.
 */
export function TableFormModal({ open, table, onClose, onSaved }: TableFormModalProps) {
  const isEdit = table?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<MetaTableRequest>()

  const createTable = $api.useMutation('post', '/api/tables')
  const updateTable = $api.useMutation('put', '/api/tables/{id}')

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (table) {
      form.setFieldsValue({ physicalName: table.physicalName, logicalName: table.logicalName, description: table.description })
    }
  }, [open, table, form])

  const showError = (error: unknown) => {
    if (applyCodeError(form, error, TABLE_ERROR_FIELDS) || applyFieldErrors(form, error)) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = (values: MetaTableRequest) => {
    const body: MetaTableRequest = {
      physicalName: values.physicalName.trim(),
      logicalName: values.logicalName.trim(),
      description: values.description?.trim() || undefined,
    }
    const onSuccess = (saved: MetaTableResponse) => {
      message.success(isEdit ? '테이블을 수정했습니다.' : '테이블을 등록했습니다.')
      // 비표준 컬럼 목록도 테이블 물리명을 보여주므로 함께 새로 불러온다.
      invalidate('/api/tables', '/api/tables/{id}', '/api/histories', '/api/columns/non-standard')
      onSaved?.(saved, !isEdit)
      onClose()
    }
    if (isEdit && table?.id !== undefined) {
      updateTable.mutate({ params: { path: { id: table.id } }, body }, { onSuccess, onError: showError })
    } else {
      createTable.mutate({ body }, { onSuccess, onError: showError })
    }
  }

  return (
    <Modal
      open={open}
      title={isEdit ? '테이블 수정' : '테이블 등록'}
      okText="저장"
      confirmLoading={createTable.isPending || updateTable.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={560}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="physicalName"
          label="물리명"
          extra="영문자로 시작하는 영문/숫자/_이며 대문자로 저장됩니다."
          normalize={(value?: string) => value?.toUpperCase()}
          rules={[
            { required: true, message: '물리명을 입력하세요.' },
            { max: PHYSICAL_NAME_MAX_LENGTH, message: `${PHYSICAL_NAME_MAX_LENGTH}자 이내로 입력하세요.` },
            { pattern: PHYSICAL_NAME_PATTERN, message: '영문자로 시작하는 영문/숫자/_만 사용할 수 있습니다.' },
          ]}
        >
          <Input placeholder="예: TB_CUSTOMER" maxLength={PHYSICAL_NAME_MAX_LENGTH} className="mono" />
        </Form.Item>
        <Form.Item
          name="logicalName"
          label="논리명"
          rules={[
            { required: true, whitespace: true, message: '논리명을 입력하세요.' },
            { max: 200, message: '200자 이내로 입력하세요.' },
          ]}
        >
          <Input placeholder="예: 고객" maxLength={200} />
        </Form.Item>
        <Form.Item name="description" label="설명" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
          <Input.TextArea rows={3} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </Modal>
  )
}
