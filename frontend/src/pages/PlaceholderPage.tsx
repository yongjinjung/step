import { Card, Result } from 'antd'
import { PageHeader } from '@/components/PageHeader'
import type { NavItem } from '@/layouts/navigation'

/** 아직 만들지 않은 화면. 메뉴와 라우트가 연결됐는지 확인하는 용도다. */
export function PlaceholderPage({ item }: { item: NavItem }) {
  return (
    <>
      <PageHeader title={item.label} />
      <Card>
        <Result status="info" title="준비 중인 화면입니다" subTitle={item.description} />
      </Card>
    </>
  )
}
