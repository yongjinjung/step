import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { $api } from '@/api/client'
import { applyFieldErrors, errorMessage, isApiError } from '@/api/errors'
import type { components } from '@/api/schema'
import { useAuth } from '@/auth/AuthContext'

type LoginRequest = components['schemas']['LoginRequest']

/** 로그인 화면. 성공하면 원래 가려던 화면(없으면 첫 화면)으로 이동한다. */
export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form] = Form.useForm<LoginRequest>()
  const loginMutation = $api.useMutation('post', '/api/auth/login')

  const from = (location.state as { from?: string } | null)?.from ?? '/'

  if (user) {
    return <Navigate to={from} replace />
  }

  const handleFinish = (values: LoginRequest) => {
    loginMutation.mutate(
      { body: values },
      {
        onSuccess: (token) => {
          if (!token.accessToken) {
            return
          }
          login(token.accessToken)
          navigate(from, { replace: true })
        },
        // 입력 형식 오류는 필드 아래에, 아이디/비밀번호 오류·비활성 계정은 폼 위 알림으로 보여준다.
        onError: (error) => applyFieldErrors(form, error),
      },
    )
  }

  // 필드 오류로 표시하지 않은 오류(INVALID_CREDENTIALS, USER_DISABLED, 네트워크 오류 등)만 알림으로 보여준다.
  // 오류 응답은 API 문서에 선언돼 있지 않아 생성 타입이 never이므로 unknown으로 받아 모양을 확인한다.
  const error: unknown = loginMutation.error
  const showAlert = error != null && !(isApiError(error) && error.fieldErrors?.length > 0)

  return (
    <div className="login-page">
      <Card className="login-card">
        <Typography.Title level={3} className="login-title">
          STEP
        </Typography.Title>
        <Typography.Paragraph type="secondary" className="login-subtitle">
          로그인이 필요합니다
        </Typography.Paragraph>

        {showAlert && <Alert type="error" showIcon title={errorMessage(error)} className="login-alert" />}

        <Form form={form} layout="vertical" onFinish={handleFinish} requiredMark={false} autoComplete="on">
          <Form.Item name="username" label="아이디" rules={[{ required: true, message: '아이디를 입력하세요.' }]}>
            <Input prefix={<UserOutlined />} autoComplete="username" autoFocus size="large" />
          </Form.Item>
          <Form.Item name="password" label="비밀번호" rules={[{ required: true, message: '비밀번호를 입력하세요.' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" size="large" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block size="large" loading={loginMutation.isPending}>
            로그인
          </Button>
        </Form>
      </Card>
    </div>
  )
}
