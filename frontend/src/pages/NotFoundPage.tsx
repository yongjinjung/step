import { Button, Result } from 'antd'
import { useNavigate } from 'react-router'

export function NotFoundPage() {
  const navigate = useNavigate()
  return (
    <Result
      status="404"
      title="페이지를 찾을 수 없습니다"
      extra={
        <Button type="primary" onClick={() => navigate('/')}>
          첫 화면으로
        </Button>
      }
    />
  )
}
