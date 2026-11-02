import { App, Button, Card, Descriptions, Form, Input, Skeleton, Space, Tag } from 'antd'
import { $api } from '@/api/client'
import { applyFieldErrors, errorMessage, isApiError } from '@/api/errors'
import type { components } from '@/api/schema'
import { formatDateTime } from '@/utils/format'
import { PageHeader } from '@/components/PageHeader'

type PasswordChangeRequest = components['schemas']['PasswordChangeRequest']
type PasswordForm = PasswordChangeRequest & { confirmPassword: string }

/** 백엔드 PasswordPolicy와 같은 규칙: 8~64자, 공백 없는 ASCII */
const PASSWORD_RULES = [
  { required: true, message: '새 비밀번호를 입력하세요.' },
  { min: 8, max: 64, message: '8~64자로 입력하세요.' },
  { pattern: /^[!-~]+$/, message: '영문, 숫자, 특수문자만 사용할 수 있습니다 (공백 불가).' },
]

/** 내 정보와 비밀번호 변경. 생성된 API 훅($api) 사용 예시이기도 하다. */
export function MyPage() {
  const { message } = App.useApp()
  const [form] = Form.useForm<PasswordForm>()
  const meQuery = $api.useQuery('get', '/api/auth/me')
  const changePassword = $api.useMutation('put', '/api/auth/me/password')

  const handleFinish = ({ currentPassword, newPassword }: PasswordForm) => {
    changePassword.mutate(
      { body: { currentPassword, newPassword } },
      {
        onSuccess: () => {
          message.success('비밀번호를 변경했습니다. 이미 발급된 토큰은 만료 전까지 유효합니다.')
          form.resetFields()
        },
        // 오류 응답은 API 문서에 선언돼 있지 않아 생성 타입이 never다. unknown으로 받고 isApiError로 모양을 확인한다.
        onError: (error: unknown) => {
          if (isApiError(error) && error.code === 'INVALID_CURRENT_PASSWORD') {
            form.setFields([{ name: 'currentPassword', errors: [error.message] }])
          } else if (!applyFieldErrors(form, error)) {
            message.error(errorMessage(error))
          }
        },
      },
    )
  }

  const me = meQuery.data

  return (
    <>
      <PageHeader title="내 정보" />
      <Space orientation="vertical" size="large" className="full-width">
        <Card title="계정">
          {meQuery.isPending ? (
            <Skeleton active />
          ) : meQuery.isError ? (
            errorMessage(meQuery.error)
          ) : (
            <Descriptions column={{ xs: 1, md: 2 }} bordered>
              <Descriptions.Item label="아이디">{me?.username}</Descriptions.Item>
              <Descriptions.Item label="이름">{me?.name}</Descriptions.Item>
              <Descriptions.Item label="역할">
                <Space size={[0, 4]} wrap>
                  {(me?.roles ?? []).map((role) => (
                    <Tag key={role.id} color={me?.superAdmin && role.code === 'ADMIN' ? 'volcano' : 'blue'}>
                      {role.name}
                    </Tag>
                  ))}
                  {(me?.roles ?? []).length === 0 && <Tag>역할 없음</Tag>}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="상태">
                {me?.enabled ? <Tag color="green">활성</Tag> : <Tag>비활성</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="등록 일시">{formatDateTime(me?.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="수정 일시">{formatDateTime(me?.updatedAt)}</Descriptions.Item>
            </Descriptions>
          )}
        </Card>

        <Card title="비밀번호 변경">
          <Form form={form} layout="vertical" onFinish={handleFinish} className="narrow-form">
            <Form.Item name="currentPassword" label="현재 비밀번호" rules={[{ required: true, message: '현재 비밀번호를 입력하세요.' }]}>
              <Input.Password autoComplete="current-password" />
            </Form.Item>
            <Form.Item name="newPassword" label="새 비밀번호" rules={PASSWORD_RULES}>
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            <Form.Item
              name="confirmPassword"
              label="새 비밀번호 확인"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: '새 비밀번호를 한 번 더 입력하세요.' },
                ({ getFieldValue }) => ({
                  validator: (_, value) =>
                    !value || getFieldValue('newPassword') === value
                      ? Promise.resolve()
                      : Promise.reject(new Error('새 비밀번호와 같지 않습니다.')),
                }),
              ]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={changePassword.isPending}>
              변경
            </Button>
          </Form>
        </Card>
      </Space>
    </>
  )
}
