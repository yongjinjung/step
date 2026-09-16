import { Card, Result } from 'antd'
import { PageHeader } from '@/components/PageHeader'

interface PlaceholderPageProps {
  /** 메뉴 이름 */
  title: string
  description?: string
}

/**
 * 메뉴에는 있지만 아직 만들지 않은 화면.
 * 메뉴를 DB에서 관리하므로, 등록된 화면이 없는 경로의 메뉴를 누르면 이 화면이 나온다(`layouts/screens.tsx`).
 */
export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <>
      <PageHeader title={title} />
      <Card>
        <Result
          status="info"
          title="준비 중인 화면입니다"
          subTitle={description ?? '메뉴에는 등록됐지만 아직 만들지 않은 화면입니다.'}
        />
      </Card>
    </>
  )
}
