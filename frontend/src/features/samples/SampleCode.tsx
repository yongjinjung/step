import { CopyOutlined } from '@ant-design/icons'
import { App, Button, Typography } from 'antd'

interface SampleCodeProps {
  /** 보여줄 코드 (템플릿 문자열 그대로 출력한다) */
  code: string
  /** 코드 위에 적을 파일 경로나 설명. 없으면 "예제 코드" */
  title?: string
}

/**
 * 복사할 수 있는 코드 블록.
 *
 * 샘플 화면은 "읽고 그대로 따라 쓰는" 용도라 강조(하이라이팅)보다 복사가 중요하다.
 * 별도 라이브러리를 넣지 않고 `<pre>`와 클립보드 API만 쓴다.
 */
export function SampleCode({ code, title }: SampleCodeProps) {
  const { message } = App.useApp()

  const copy = () => {
    // 클립보드 API는 보안 컨텍스트(https, localhost)에서만 동작한다. 실패하면 직접 선택해 복사하도록 안내한다.
    navigator.clipboard.writeText(code).then(
      () => message.success('코드를 복사했습니다.'),
      () => message.error('복사하지 못했습니다. 코드를 직접 선택해 복사하세요.'),
    )
  }

  return (
    <div className="sample-code">
      <div className="sample-code-bar">
        <Typography.Text className="sample-code-title">{title ?? '예제 코드'}</Typography.Text>
        <Button size="small" type="text" icon={<CopyOutlined />} onClick={copy}>
          복사
        </Button>
      </div>
      <pre className="sample-code-body">
        <code>{code}</code>
      </pre>
    </div>
  )
}
