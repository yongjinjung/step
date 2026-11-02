import { Alert, App, Form, Input, Modal, Switch } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { ROLE_CODE_PATTERN, ROLE_ERROR_FIELDS, type RoleRequest, type RoleResponse } from './roleFields'

interface RoleFormModalProps {
  open: boolean
  /** 수정할 역할. null이면 등록 */
  role: RoleResponse | null
  onClose: () => void
  /** 저장에 성공하면 저장된 역할로 호출한다 (등록 후 바로 선택) */
  onSaved?: (role: RoleResponse, created: boolean) => void
}

/**
 * 역할 등록·수정 모달.
 *
 * 기본 제공 역할(ADMIN, USER)은 코드와 전권 여부를 바꿀 수 없어 그 항목을 잠근다(서버도 막는다).
 * 전권 역할은 메뉴 권한과 관계없이 모든 기능을 쓸 수 있으므로 안내를 함께 보여준다.
 */
export function RoleFormModal({ open, role, onClose, onSaved }: RoleFormModalProps) {
  const isEdit = role?.id !== undefined
  const builtIn = role?.builtIn ?? false
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<RoleRequest>()
  const superAdmin = Form.useWatch('superAdmin', form)

  const createRole = $api.useMutation('post', '/api/roles')
  const updateRole = $api.useMutation('put', '/api/roles/{id}')

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (role) {
      form.setFieldsValue({
        code: role.code,
        name: role.name,
        description: role.description,
        superAdmin: role.superAdmin,
        used: role.used,
      })
    }
  }, [open, role, form])

  const showError = (error: unknown) => {
    if (applyCodeError(form, error, ROLE_ERROR_FIELDS) || applyFieldErrors(form, error)) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = (values: RoleRequest) => {
    const body: RoleRequest = {
      code: values.code.trim(),
      name: values.name.trim(),
      description: values.description?.trim() || undefined,
      superAdmin: values.superAdmin ?? false,
      used: values.used ?? true,
    }
    const onSuccess = (saved: RoleResponse) => {
      message.success(isEdit ? '역할을 수정했습니다.' : '역할을 등록했습니다.')
      // 역할이 바뀌면 내 권한과 메뉴도 달라질 수 있어 함께 새로 불러온다.
      invalidate('/api/roles', '/api/roles/{id}', '/api/menus')
      onSaved?.(saved, !isEdit)
      onClose()
    }
    if (isEdit && role?.id !== undefined) {
      updateRole.mutate({ params: { path: { id: role.id } }, body }, { onSuccess, onError: showError })
    } else {
      createRole.mutate({ body }, { onSuccess, onError: showError })
    }
  }

  return (
    <Modal
      open={open}
      title={isEdit ? '역할 수정' : '역할 등록'}
      okText="저장"
      confirmLoading={createRole.isPending || updateRole.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={520}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ superAdmin: false, used: true }}>
        <Form.Item
          name="code"
          label="역할 코드"
          extra={builtIn ? '기본 제공 역할의 코드는 바꿀 수 없습니다.' : '영문자로 시작하는 영문/숫자/_이며 대문자로 저장됩니다.'}
          normalize={(value?: string) => value?.toUpperCase()}
          rules={[
            { required: true, message: '역할 코드를 입력하세요.' },
            { max: 50, message: '50자 이내로 입력하세요.' },
            { pattern: ROLE_CODE_PATTERN, message: '영문자로 시작하는 영문/숫자/_만 사용할 수 있습니다.' },
          ]}
        >
          <Input className="mono" maxLength={50} disabled={builtIn} placeholder="예: STD_MANAGER" />
        </Form.Item>

        <Form.Item
          name="name"
          label="역할명"
          rules={[
            { required: true, whitespace: true, message: '역할명을 입력하세요.' },
            { max: 100, message: '100자 이내로 입력하세요.' },
          ]}
        >
          <Input maxLength={100} placeholder="예: 표준 데이터 관리자" />
        </Form.Item>

        <Form.Item name="description" label="설명" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
          <Input.TextArea rows={2} maxLength={1000} showCount />
        </Form.Item>

        <Form.Item
          name="superAdmin"
          label="전권"
          valuePropName="checked"
          extra={builtIn ? '기본 제공 역할의 전권 여부는 바꿀 수 없습니다.' : '켜면 메뉴 권한과 관계없이 모든 기능을 쓸 수 있습니다.'}
        >
          <Switch checkedChildren="전권" unCheckedChildren="일반" disabled={builtIn} />
        </Form.Item>

        <Form.Item name="used" label="사용 여부" valuePropName="checked" extra="미사용 역할의 권한은 무시됩니다.">
          <Switch checkedChildren="사용" unCheckedChildren="미사용" />
        </Form.Item>

        {superAdmin && (
          <Alert
            type="warning"
            showIcon
            title="전권 역할은 메뉴 권한 설정과 관계없이 모든 화면과 기능을 쓸 수 있습니다."
          />
        )}
      </Form>
    </Modal>
  )
}
