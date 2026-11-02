import { Alert, App, Form, Input, Modal, Typography } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyFieldErrors, errorMessage } from '@/api/errors'
import { PASSWORD_RULES, type UserResponse } from './userFields'

interface PasswordResetModalProps {
  /** 초기화할 계정. null이면 닫힌다 */
  user: UserResponse | null
  onClose: () => void
}

interface PasswordResetValues {
  password: string
  confirmPassword: string
}

/**
 * 관리자의 비밀번호 초기화 모달.
 * 현재 비밀번호 확인 없이 새 비밀번호로 바꾼다. 사용자가 직접 바꿀 때는 내 정보 화면을 쓴다.
 */
export function PasswordResetModal({ user, onClose }: PasswordResetModalProps) {
  const open = user !== null
  const { message } = App.useApp()
  const [form] = Form.useForm<PasswordResetValues>()

  const resetPassword = $api.useMutation('put', '/api/users/{id}/password')

  useEffect(() => {
    if (open) {
      form.resetFields()
    }
  }, [open, form])

  const handleFinish = ({ password }: PasswordResetValues) => {
    if (user?.id === undefined) {
      return
    }
    resetPassword.mutate(
      { params: { path: { id: user.id } }, body: { password } },
      {
        onSuccess: () => {
          message.success('비밀번호를 초기화했습니다. 사용자에게 새 비밀번호를 알려주세요.')
          onClose()
        },
        onError: (error: unknown) => {
          if (!applyFieldErrors(form, error)) {
            message.error(errorMessage(error))
          }
        },
      },
    )
  }

  return (
    <Modal
      open={open}
      title="비밀번호 초기화"
      okText="초기화"
      confirmLoading={resetPassword.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={480}
    >
      <Typography.Paragraph>
        <Typography.Text strong>
          {user?.name} ({user?.username})
        </Typography.Text>{' '}
        계정의 비밀번호를 새로 정합니다.
      </Typography.Paragraph>
      <Alert
        type="info"
        showIcon
        className="form-notice"
        title="이미 발급된 토큰은 만료 전까지 유효합니다. 사용자는 새 비밀번호로 다시 로그인하면 됩니다."
      />
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item name="password" label="새 비밀번호" rules={PASSWORD_RULES}>
          <Input.Password maxLength={64} autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="새 비밀번호 확인"
          dependencies={['password']}
          rules={[
            { required: true, message: '새 비밀번호를 한 번 더 입력하세요.' },
            ({ getFieldValue }) => ({
              validator: (_, value?: string) =>
                !value || getFieldValue('password') === value
                  ? Promise.resolve()
                  : Promise.reject(new Error('새 비밀번호와 같지 않습니다.')),
            }),
          ]}
        >
          <Input.Password maxLength={64} autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
