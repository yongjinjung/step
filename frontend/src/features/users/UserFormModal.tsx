import { Alert, App, Form, Input, Modal, Select, Switch } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyFieldErrors, errorMessage, isApiError } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { PASSWORD_RULES, USERNAME_PATTERN, type UserCreateRequest, type UserResponse, type UserUpdateRequest } from './userFields'

type UserFormValues = {
  username?: string
  password?: string
  name?: string
  roleIds?: number[]
  enabled?: boolean
}

interface UserFormModalProps {
  open: boolean
  /** 수정할 계정. null이면 등록 */
  user: UserResponse | null
  onClose: () => void
}

/**
 * 계정 등록·수정 모달.
 *
 * - 등록: 아이디·초기 비밀번호·이름·역할. 아이디는 소문자로 저장되고 나중에 바꿀 수 없다(토큰 subject).
 * - 수정: 이름·역할·활성 여부만 바꾼다. 비밀번호는 따로 초기화한다(PasswordResetModal).
 * - 역할은 여러 개 줄 수 있고 권한은 역할들의 합집합이다. 역할이 하나도 없으면 아무 화면도 쓸 수 없어 필수다.
 * - 전권 역할을 가진 활성 사용자가 한 명도 남지 않는 변경은 서버가 막는다(LAST_ADMIN_REQUIRED).
 */
export function UserFormModal({ open, user, onClose }: UserFormModalProps) {
  const isEdit = user?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<UserFormValues>()

  const rolesQuery = $api.useQuery('get', '/api/roles', {}, { enabled: open })
  const createUser = $api.useMutation('post', '/api/users')
  const updateUser = $api.useMutation('put', '/api/users/{id}')

  // 미사용 역할은 권한이 무시되므로 새로 고르지 못하게 하고, 이미 부여된 역할만 남겨 둔다.
  const assignedIds = (user?.roles ?? []).map((role) => role.id)
  const roleOptions = (rolesQuery.data ?? [])
    .filter((role) => role.used || assignedIds.includes(role.id))
    .map((role) => ({
      value: role.id as number,
      label: `${role.name} (${role.code})${role.used ? '' : ' · 미사용'}`,
    }))

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (user) {
      form.setFieldsValue({
        username: user.username,
        name: user.name,
        roleIds: (user.roles ?? []).map((role) => role.id as number),
        enabled: user.enabled,
      })
    }
  }, [open, user, form])

  const showError = (error: unknown) => {
    if (isApiError(error)) {
      if (error.code === 'DUPLICATE_USERNAME') {
        form.setFields([{ name: 'username', errors: [error.message] }])
        return
      }
      // 마지막 전권 사용자 보호: 역할을 뺐는지 비활성화했는지에 따라 오류를 보여줄 자리가 다르다.
      if (error.code === 'LAST_ADMIN_REQUIRED') {
        const enabled = form.getFieldValue('enabled')
        form.setFields([{ name: enabled === false ? 'enabled' : 'roleIds', errors: [error.message] }])
        return
      }
      if (error.code === 'USER_ROLE_REQUIRED' || error.code === 'ROLE_NOT_FOUND') {
        form.setFields([{ name: 'roleIds', errors: [error.message] }])
        return
      }
    }
    if (!applyFieldErrors(form, error)) {
      message.error(errorMessage(error))
    }
  }

  const handleFinish = (values: UserFormValues) => {
    const onSuccess = () => {
      message.success(isEdit ? '계정을 수정했습니다.' : '계정을 등록했습니다.')
      // 내 역할을 바꿨을 수도 있으므로 화면 메뉴도 다시 불러온다.
      invalidate('/api/users', '/api/users/{id}', '/api/menus')
      onClose()
    }
    if (isEdit && user?.id !== undefined) {
      const body: UserUpdateRequest = {
        name: values.name?.trim() ?? '',
        roleIds: values.roleIds ?? [],
        enabled: values.enabled ?? true,
      }
      updateUser.mutate({ params: { path: { id: user.id } }, body }, { onSuccess, onError: showError })
      return
    }
    const body: UserCreateRequest = {
      username: values.username?.trim() ?? '',
      password: values.password ?? '',
      name: values.name?.trim() ?? '',
      roleIds: values.roleIds ?? [],
    }
    createUser.mutate({ body }, { onSuccess, onError: showError })
  }

  return (
    <Modal
      open={open}
      title={isEdit ? '계정 수정' : '계정 등록'}
      okText="저장"
      confirmLoading={createUser.isPending || updateUser.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={520}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ enabled: true }}>
        <Form.Item
          name="username"
          label="아이디"
          extra={isEdit ? '아이디는 바꿀 수 없습니다.' : '소문자로 저장됩니다. 등록 후에는 바꿀 수 없습니다.'}
          rules={
            isEdit
              ? []
              : [
                  { required: true, message: '아이디를 입력하세요.' },
                  { min: 4, max: 50, message: '4~50자로 입력하세요.' },
                  { pattern: USERNAME_PATTERN, message: "영문/숫자로 시작하고 영문, 숫자, '.', '_', '-'만 쓸 수 있습니다." },
                ]
          }
        >
          <Input className="mono" maxLength={50} disabled={isEdit} autoComplete="off" placeholder="예: hong" />
        </Form.Item>

        {!isEdit && (
          <Form.Item name="password" label="초기 비밀번호" extra="사용자가 로그인한 뒤 내 정보에서 바꿀 수 있습니다." rules={PASSWORD_RULES}>
            <Input.Password maxLength={64} autoComplete="new-password" />
          </Form.Item>
        )}

        <Form.Item
          name="name"
          label="이름"
          rules={[
            { required: true, whitespace: true, message: '이름을 입력하세요.' },
            { max: 100, message: '100자 이내로 입력하세요.' },
          ]}
        >
          <Input maxLength={100} placeholder="예: 홍길동" />
        </Form.Item>

        <Form.Item
          name="roleIds"
          label="역할"
          extra="여러 역할을 줄 수 있고 권한은 역할들의 합집합입니다. 역할별 권한은 역할 관리 화면에서 정합니다."
          rules={[{ required: true, message: '역할을 하나 이상 고르세요.' }]}
        >
          <Select mode="multiple" options={roleOptions} loading={rolesQuery.isFetching} placeholder="역할 선택" />
        </Form.Item>

        {isEdit && (
          <Form.Item
            name="enabled"
            label="활성 여부"
            valuePropName="checked"
            extra="비활성 계정은 로그인할 수 없습니다. 계정은 삭제하지 않고 비활성으로 둡니다."
          >
            <Switch checkedChildren="활성" unCheckedChildren="비활성" />
          </Form.Item>
        )}

        {isEdit && user?.superAdmin && (
          <Alert type="info" showIcon title="전권 역할을 가진 활성 사용자가 한 명도 남지 않는 변경은 저장되지 않습니다." />
        )}
      </Form>
    </Modal>
  )
}
