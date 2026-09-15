import { Typography } from 'antd'
import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  /** 제목 오른쪽 버튼 영역 (등록, 엑셀 등) */
  extra?: ReactNode
}

/** 화면 제목 줄. 모든 화면이 같은 모양의 제목과 버튼 위치를 쓰도록 한다. */
export function PageHeader({ title, extra }: PageHeaderProps) {
  return (
    <div className="page-header">
      <Typography.Title level={4} className="page-title">
        {title}
      </Typography.Title>
      {extra && <div className="page-extra">{extra}</div>}
    </div>
  )
}
