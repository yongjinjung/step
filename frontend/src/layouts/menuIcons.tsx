import {
  ApartmentOutlined,
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  BookOutlined,
  ClusterOutlined,
  DatabaseOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  FolderOutlined,
  FormOutlined,
  HistoryOutlined,
  LockOutlined,
  MenuOutlined,
  SettingOutlined,
  TableOutlined,
  TagsOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'

/**
 * 메뉴 아이콘 이름 → 아이콘.
 *
 * 메뉴는 DB에 아이콘 "이름"만 저장하고(백엔드 `app_menu.icon`), 실제 아이콘은 여기서 고른다.
 * 관리 화면의 아이콘 선택 목록도 이 표를 쓰므로, 아이콘을 늘리려면 여기에만 추가하면 된다.
 * 모르는 이름이거나 비어 있으면 아이콘 없이 이름만 보여준다.
 */
export const MENU_ICONS: Record<string, ReactNode> = {
  book: <BookOutlined />,
  cluster: <ClusterOutlined />,
  apartment: <ApartmentOutlined />,
  tags: <TagsOutlined />,
  table: <TableOutlined />,
  'file-search': <FileSearchOutlined />,
  audit: <AuditOutlined />,
  history: <HistoryOutlined />,
  team: <TeamOutlined />,
  menu: <MenuOutlined />,
  lock: <LockOutlined />,
  setting: <SettingOutlined />,
  appstore: <AppstoreOutlined />,
  folder: <FolderOutlined />,
  file: <FileTextOutlined />,
  form: <FormOutlined />,
  database: <DatabaseOutlined />,
  chart: <BarChartOutlined />,
}

/** 관리 화면의 아이콘 선택 목록 (이름 순). */
export const MENU_ICON_NAMES = Object.keys(MENU_ICONS)

/** 아이콘 이름으로 아이콘을 찾는다. 이름이 없거나 모르는 이름이면 undefined. */
export function menuIcon(name: string | null | undefined): ReactNode {
  return name ? MENU_ICONS[name] : undefined
}
