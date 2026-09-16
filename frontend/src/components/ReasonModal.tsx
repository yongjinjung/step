import { Form, Input, Modal } from 'antd'
import type { ReactNode } from 'react'

interface ReasonModalProps {
  open: boolean
  title: string
  /** 입력란 위에 보여줄 안내 (대상 이름 등) */
  description?: ReactNode
  okText?: string
  /** 입력란 이름. 기본은 신청 사유 */
  label?: string
  placeholder?: string
  /** true면 비워 둘 수 없다 (반려 의견 등) */
  required?: boolean
  /** 확인 버튼을 위험 동작(빨강)으로 보여준다 */
  danger?: boolean
  confirmLoading?: boolean
  /** 내용을 입력하고 확인을 누르면 호출한다. 비워 두면 undefined */
  onSubmit: (reason: string | undefined) => void
  onCancel: () => void
}

/**
 * 사유·의견 한 줄만 받는 모달.
 * USER의 삭제 신청(신청 사유, 선택), 관리자의 승인(승인 의견, 선택)·반려(반려 의견, 필수)에 쓴다.
 */
export function ReasonModal({
  open,
  title,
  description,
  okText = '신청',
  label = '신청 사유',
  placeholder = '관리자가 검토할 때 참고할 내용 (선택)',
  required = false,
  danger = false,
  confirmLoading,
  onSubmit,
  onCancel,
}: ReasonModalProps) {
  const [form] = Form.useForm<{ reason?: string }>()

  return (
    <Modal
      open={open}
      title={title}
      okText={okText}
      okButtonProps={{ danger }}
      confirmLoading={confirmLoading}
      onOk={() => form.submit()}
      onCancel={onCancel}
      // 폼을 미리 렌더링해 두어 열기 전에도 form 인스턴스가 연결되게 하고, 닫힌 뒤 입력값을 지운다.
      forceRender
      afterClose={() => form.resetFields()}
    >
      {description}
      <Form form={form} layout="vertical" onFinish={({ reason }) => onSubmit(reason?.trim() || undefined)}>
        <Form.Item
          name="reason"
          label={label}
          rules={[
            { required, whitespace: true, message: `${label}을(를) 입력하세요.` },
            { max: 1000, message: '1000자 이내로 입력하세요.' },
          ]}
        >
          <Input.TextArea rows={3} maxLength={1000} showCount placeholder={placeholder} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
