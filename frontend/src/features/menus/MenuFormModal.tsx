import { App, AutoComplete, Col, Form, Input, InputNumber, Modal, Row, Select, Space, Switch } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { menuIcon, MENU_ICON_NAMES } from '@/layouts/menuIcons'
import { SCREEN_PATHS } from '@/layouts/screens'
import { MENU_ERROR_FIELDS, MENU_PATH_PATTERN, type MenuRequest, type MenuResponse } from './menuFields'

type MenuFormValues = Partial<MenuRequest>

interface MenuFormModalProps {
  open: boolean
  /** 수정할 메뉴. null이면 등록 */
  menu: MenuResponse | null
  /** 등록할 때 기본으로 고를 상위 메뉴 (하위 추가). 비우면 상위 메뉴로 등록 */
  defaultParentId?: number
  /** 등록할 때 채울 정렬 순서 */
  defaultSortOrder: number
  /** 상위 메뉴 선택 목록 */
  topMenus: MenuResponse[]
  onClose: () => void
}

/**
 * 메뉴 등록·수정 모달 (관리자 전용 화면에서만 연다).
 *
 * - 상위 메뉴를 비우면 위쪽(TOP) 메뉴, 고르면 그 아래 왼쪽(LEFT) 메뉴가 된다.
 * - 화면 경로는 하위 메뉴만 입력한다. 등록된 화면 목록에서 고를 수 있고, 목록에 없는 경로를 적으면
 *   메뉴는 만들어지되 "준비 중" 화면이 나온다.
 */
export function MenuFormModal({ open, menu, defaultParentId, defaultSortOrder, topMenus, onClose }: MenuFormModalProps) {
  const isEdit = menu?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<MenuFormValues>()
  const parentId = Form.useWatch('parentId', form)

  const createMenu = $api.useMutation('post', '/api/menus')
  const updateMenu = $api.useMutation('put', '/api/menus/{id}')

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (menu) {
      form.setFieldsValue({
        parentId: menu.parentId,
        name: menu.name,
        path: menu.path,
        icon: menu.icon,
        sortOrder: menu.sortOrder,
        used: menu.used,
        adminOnly: menu.adminOnly,
      })
    } else {
      form.setFieldsValue({ parentId: defaultParentId, sortOrder: defaultSortOrder })
    }
    // 열 때의 기본값만 쓴다. 열린 동안 목록이 바뀌어도 입력 중인 값을 건드리지 않는다.
  }, [open, menu, form])

  const showError = (error: unknown) => {
    if (applyCodeError(form, error, MENU_ERROR_FIELDS) || applyFieldErrors(form, error)) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = (values: MenuFormValues) => {
    const isTopMenu = values.parentId === undefined || values.parentId === null
    const body: MenuRequest = {
      parentId: isTopMenu ? undefined : values.parentId,
      name: values.name?.trim() ?? '',
      // 상위 메뉴는 화면이 아니라 묶음이라 경로를 보내지 않는다 (서버 규칙 INVALID_MENU_PATH).
      path: isTopMenu ? undefined : values.path?.trim() || undefined,
      icon: values.icon || undefined,
      sortOrder: values.sortOrder as number,
      used: values.used ?? true,
      adminOnly: values.adminOnly ?? false,
    }
    const onSuccess = () => {
      message.success(isEdit ? '메뉴를 수정했습니다.' : '메뉴를 등록했습니다.')
      // 관리 목록과 실제 화면 메뉴(위/왼쪽 막대)를 함께 새로 불러온다.
      invalidate('/api/menus/all', '/api/menus', '/api/menus/{id}')
      onClose()
    }
    if (isEdit && menu?.id !== undefined) {
      updateMenu.mutate({ params: { path: { id: menu.id } }, body }, { onSuccess, onError: showError })
    } else {
      createMenu.mutate({ body }, { onSuccess, onError: showError })
    }
  }

  // 자기 자신은 상위 메뉴로 고를 수 없다.
  const parentOptions = topMenus
    .filter((top) => top.id !== undefined && top.id !== menu?.id)
    .map((top) => ({ value: top.id as number, label: top.name as string }))

  return (
    <Modal
      open={open}
      title={isEdit ? '메뉴 수정' : '메뉴 등록'}
      okText="저장"
      confirmLoading={createMenu.isPending || updateMenu.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={560}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={{ used: true, adminOnly: false }}>
        <Form.Item
          name="parentId"
          label="상위 메뉴"
          extra="비우면 위쪽(TOP) 메뉴가 되고, 고르면 그 아래 왼쪽(LEFT) 메뉴가 됩니다."
        >
          <Select allowClear placeholder="상위 메뉴 없음 (TOP 메뉴로 등록)" options={parentOptions} />
        </Form.Item>

        <Form.Item
          name="name"
          label="메뉴명"
          rules={[
            { required: true, whitespace: true, message: '메뉴명을 입력하세요.' },
            { max: 100, message: '100자 이내로 입력하세요.' },
          ]}
        >
          <Input placeholder="예: 표준 단어" maxLength={100} />
        </Form.Item>

        <Form.Item
          name="path"
          label="화면 경로"
          dependencies={['parentId']}
          extra={
            parentId
              ? '등록된 화면을 고르면 그 화면이 열립니다. 목록에 없는 경로는 "준비 중" 화면이 나옵니다.'
              : '위쪽(TOP) 메뉴는 하위 메뉴를 묶는 이름이라 화면 경로가 없습니다.'
          }
          rules={[
            { required: Boolean(parentId), message: '화면 경로를 입력하세요.' },
            { max: 200, message: '200자 이내로 입력하세요.' },
            { pattern: MENU_PATH_PATTERN, message: '영문/숫자/_/-와 /만 사용할 수 있습니다.' },
          ]}
        >
          <AutoComplete
            disabled={!parentId}
            placeholder={parentId ? '예: /words' : '상위 메뉴에는 경로가 없습니다'}
            options={SCREEN_PATHS.map((path) => ({ value: path }))}
            filterOption={(input, option) => (option?.value as string).includes(input.trim())}
          >
            <Input className="mono" maxLength={200} />
          </AutoComplete>
        </Form.Item>

        <Row gutter={12}>
          <Col xs={24} sm={12}>
            <Form.Item name="icon" label="아이콘">
              <Select
                allowClear
                placeholder="아이콘 없음"
                options={MENU_ICON_NAMES.map((name) => ({ value: name, label: name }))}
                optionRender={(option) => (
                  <Space size={8}>
                    {menuIcon(option.value as string)}
                    {option.label}
                  </Space>
                )}
                labelRender={(option) => (
                  <Space size={8}>
                    {menuIcon(option.value as string)}
                    {option.label}
                  </Space>
                )}
              />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12}>
            <Form.Item
              name="sortOrder"
              label="정렬 순서"
              extra="같은 단계 안에서 작은 값이 앞에 옵니다."
              rules={[{ required: true, message: '정렬 순서를 입력하세요.' }]}
            >
              <InputNumber className="full-width" min={0} precision={0} />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={12}>
          <Col xs={12}>
            <Form.Item name="used" label="사용 여부" valuePropName="checked" extra="미사용 메뉴는 화면에 나오지 않습니다.">
              <Switch checkedChildren="사용" unCheckedChildren="미사용" />
            </Form.Item>
          </Col>
          <Col xs={12}>
            <Form.Item
              name="adminOnly"
              label="관리자 전용"
              valuePropName="checked"
              extra="관리자에게만 메뉴가 보입니다. 권한 검사는 서버가 합니다."
            >
              <Switch checkedChildren="관리자" unCheckedChildren="전체" />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  )
}
