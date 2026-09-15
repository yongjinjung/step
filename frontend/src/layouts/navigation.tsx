import {
  ApartmentOutlined,
  AuditOutlined,
  BookOutlined,
  ClusterOutlined,
  FileSearchOutlined,
  HistoryOutlined,
  TableOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'

/** 메뉴 항목. 경로가 곧 메뉴 키다. */
export interface NavItem {
  path: string
  label: string
  icon: ReactNode
  /** 화면 설명 (준비 중 화면에 표시) */
  description: string
  /** ADMIN에게만 메뉴를 보여준다 */
  adminOnly?: boolean
}

export interface NavGroup {
  label: string
  items: NavItem[]
}

/** 왼쪽 메뉴 구성. 라우트(router.tsx)도 이 목록으로 화면을 만든다. */
export const NAV_GROUPS: NavGroup[] = [
  {
    label: '표준 사전',
    items: [
      { path: '/words', label: '표준 단어', icon: <BookOutlined />, description: '단어 검색·등록·수정·삭제와 엑셀 업로드/다운로드. USER는 신청으로 등록합니다.' },
      { path: '/domains', label: '표준 도메인', icon: <ClusterOutlined />, description: '분류어별 데이터 타입·길이 규칙 관리' },
      { path: '/terms', label: '표준 용어', icon: <ApartmentOutlined />, description: '단어 조합으로 용어명·영문약어를 만들고 도메인을 연결합니다.' },
    ],
  },
  {
    label: '메타 관리',
    items: [
      { path: '/tables', label: '테이블 정의', icon: <TableOutlined />, description: '테이블과 컬럼을 등록하고 컬럼별 표준 검출 결과를 확인합니다.' },
      { path: '/non-standard-columns', label: '비표준 컬럼', icon: <FileSearchOutlined />, description: '사유(용어 미매핑, 이름·타입·길이 불일치)별 비표준 컬럼 검색' },
    ],
  },
  {
    label: '업무',
    items: [
      { path: '/requests', label: '신청/승인', icon: <AuditOutlined />, description: '내 신청 목록과 관리자 승인·반려' },
      { path: '/histories', label: '변경 이력', icon: <HistoryOutlined />, description: '표준 데이터와 테이블 정의의 변경 전후 값 조회' },
    ],
  },
  {
    label: '관리',
    items: [
      { path: '/users', label: '사용자 관리', icon: <TeamOutlined />, description: '계정 생성, 역할·활성 변경, 비밀번호 초기화', adminOnly: true },
    ],
  },
]

export const NAV_ITEMS: NavItem[] = NAV_GROUPS.flatMap((group) => group.items)
