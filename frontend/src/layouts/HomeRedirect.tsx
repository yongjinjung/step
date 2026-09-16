import { Result, Spin } from 'antd'
import { Navigate } from 'react-router'
import { useMenus } from './MenuContext'

/**
 * 첫 주소(`/`)에서 보여줄 화면을 정한다.
 *
 * 메뉴를 DB에서 관리하므로 첫 화면도 메뉴에서 온다(첫 상위 메뉴의 첫 하위 메뉴).
 * 메뉴를 불러오는 동안에는 잠깐 기다리고, 볼 수 있는 메뉴가 없으면 안내를 보여준다.
 */
export function HomeRedirect() {
  const { homePath, loading, failed } = useMenus()

  if (loading) {
    return (
      <div className="page-loading">
        <Spin size="large" tip="메뉴를 불러오는 중입니다…">
          <div className="page-loading-content" />
        </Spin>
      </div>
    )
  }
  if (homePath) {
    return <Navigate to={homePath} replace />
  }
  return (
    <Result
      status={failed ? 'error' : 'warning'}
      title={failed ? '메뉴를 불러오지 못했습니다' : '볼 수 있는 메뉴가 없습니다'}
      subTitle={failed ? '잠시 후 새로고침해 주세요.' : '관리자에게 메뉴 등록을 요청하세요.'}
    />
  )
}
