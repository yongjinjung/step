import { ArrowLeftOutlined } from '@ant-design/icons'
import { Button, Typography } from 'antd'
import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  /** 제목 옆의 보조 설명 (상세 화면의 논리명 등) */
  subTitle?: string
  /** 있으면 제목 앞에 뒤로 가기 버튼을 보여준다 (상세 화면 → 목록) */
  onBack?: () => void
  /** 제목 오른쪽 버튼 영역 (등록, 엑셀 등) */
  extra?: ReactNode
}

/** 화면 제목 줄. 모든 화면이 같은 모양의 제목과 버튼 위치를 쓰도록 한다. */
export function PageHeader({ title, subTitle, onBack, extra }: PageHeaderProps) {
  return (
    <div className="page-header">
      <div className="page-title-group">
        {onBack && <Button type="text" icon={<ArrowLeftOutlined />} aria-label="목록으로" onClick={onBack} />}
        <Typography.Title level={4} className="page-title">
          {title}
        </Typography.Title>
        {subTitle && <Typography.Text type="secondary">{subTitle}</Typography.Text>}
      </div>
      {extra && <div className="page-extra">{extra}</div>}
    </div>
  )
}
