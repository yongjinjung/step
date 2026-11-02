import { Card, Typography } from 'antd'
import type { ReactNode } from 'react'
import { SampleCode } from './SampleCode'

interface SampleSectionProps {
  /** 섹션 제목 (예: "2. useState — 화면이 기억하는 값") */
  title: string
  /** 무엇을 언제 쓰는지 한두 문장 */
  summary: ReactNode
  /** 실수하기 쉬운 점, 이 프로젝트의 규칙 등 */
  points?: ReactNode[]
  /** 실제로 동작하는 예제. 설명만 있는 섹션은 비워 둔다 */
  demo?: ReactNode
  /** 데모를 만든 코드 */
  code?: string
  /** 코드 블록 제목 (보통 파일 경로) */
  codeTitle?: string
}

/**
 * 샘플 화면의 섹션 하나.
 *
 * "설명 → 실행 결과 → 코드" 순서를 모든 샘플 화면이 똑같이 지키도록 모아 둔다.
 * 개발자가 화면에서 동작을 보고, 바로 아래 코드를 복사해 쓰는 흐름이다.
 */
export function SampleSection({ title, summary, points, demo, code, codeTitle }: SampleSectionProps) {
  return (
    <Card title={title} className="sample-section">
      <Typography.Paragraph className="sample-summary">{summary}</Typography.Paragraph>

      {points && points.length > 0 && (
        <ul className="sample-points">
          {points.map((point, index) => (
            // 항목이 고정된 설명 목록이라 순서가 바뀌지 않는다. 이럴 때만 인덱스를 key로 쓴다.
            <li key={index}>{point}</li>
          ))}
        </ul>
      )}

      {demo && (
        <div className="sample-demo">
          <div className="sample-demo-label">실행 결과</div>
          <div className="sample-demo-body">{demo}</div>
        </div>
      )}

      {code && <SampleCode code={code} title={codeTitle} />}
    </Card>
  )
}
