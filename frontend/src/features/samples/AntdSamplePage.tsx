import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import { useMemo, useState } from 'react'
import { PageHeader } from '@/components/PageHeader'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { SampleSection } from './SampleSection'

/**
 * Ant Design 6 샘플 화면.
 *
 * 이 프로젝트 화면이 쓰는 컴포넌트와, Ant Design 6에서 이름이 바뀌어 자료대로 하면 안 되는 부분을 모았다.
 * 모든 데모가 실제로 동작하므로 눌러 보고 아래 코드를 그대로 가져다 쓰면 된다.
 */
export function AntdSamplePage() {
  return (
    <>
      <PageHeader title="Ant Design 6 샘플" subTitle="폼 · 표 · 모달 · 알림 · 서버 검색 선택" />

      <Space orientation="vertical" size="large" className="full-width">
        <Alert
          type="warning"
          showIcon
          title="Ant Design 6에서 이름이 바뀐 것들"
          description={
            <>
              인터넷 자료는 대부분 5.x 기준이다. <code>Alert</code>은 <code>message</code>가 아니라 <code>title</code>,{' '}
              <code>Space</code>는 <code>direction</code>이 아니라 <code>orientation</code>, <code>Modal</code>은{' '}
              <code>destroyOnClose</code>가 아니라 <code>destroyOnHidden</code>, <code>Select</code>의 검색 설정은{' '}
              <code>showSearch</code> 객체로만 받는다. 자세한 표는 아래 8번에 있다.
            </>
          }
        />

        <SampleSection
          title="1. 알림 — App.useApp()으로 꺼낸다"
          summary={
            <>
              <code>message</code>, <code>modal</code>, <code>notification</code>은 정적 import(<code>message.success</code>)
              하지 않고 <b><code>App.useApp()</code></b>에서 꺼낸다. 그래야 테마와 한국어 설정이 적용된다.
            </>
          }
          points={[
            <>
              <code>src/main.tsx</code>가 <code>ConfigProvider</code> → <code>AntdApp</code> 순으로 감싸 두었다.
            </>,
            <>
              삭제처럼 되돌릴 수 없는 동작은 <code>modal.confirm</code>으로 한 번 더 묻고, <code>okButtonProps: {'{ danger: true }'}</code>를 준다.
            </>,
            <>
              <code>onOk</code>가 Promise를 돌려주면 버튼이 알아서 로딩 상태가 된다.
            </>,
          ]}
          demo={<FeedbackDemo />}
          code={`const { message, modal, notification } = App.useApp()

message.success('등록했습니다.')
notification.info({ title: '알림', description: '오른쪽 위에 남는다' })

modal.confirm({
  title: '삭제할까요?',
  content: '삭제한 단어는 되돌릴 수 없습니다.',
  okText: '삭제',
  okButtonProps: { danger: true },
  cancelText: '닫기',
  // Promise를 돌려주면 확인 버튼이 로딩 상태가 된다
  onOk: () => deleteWord.mutateAsync({ params: { path: { id } } }),
})`}
        />

        <SampleSection
          title="2. Form — 폼 상태는 Form이 들고 있다"
          summary={
            <>
              입력마다 <code>useState</code>를 만들지 않는다. <code>Form.useForm()</code>으로 인스턴스를 얻고{' '}
              <code>rules</code>로 검증하며, 제출은 <code>onFinish</code>에서 받는다.
            </>
          }
          points={[
            <>
              모달 안의 폼은 <code>forceRender</code>를 줘야 <code>form</code> 인스턴스가 미리 연결된다. 안 그러면 처음 열 때{' '}
              <code>setFieldsValue</code>가 먹지 않는다.
            </>,
            <>
              서버가 준 필드 오류는 <code>applyFieldErrors(form, error)</code>로 해당 입력 아래에 보여 준다. 업무 오류 코드는{' '}
              <code>applyCodeError(form, error, {'{ DUPLICATE_WORD_NAME: "name" }'})</code>.
            </>,
            <>
              줄을 늘렸다 줄이는 입력(용어의 구성 단어)은 <code>Form.List</code>를 쓴다.
            </>,
          ]}
          demo={<FormDemo />}
          code={`const [form] = Form.useForm<WordRequest>()
const createWord = $api.useMutation('post', '/api/words')

const submit = (values: WordRequest) =>
  createWord.mutate({ body: values }, {
    onSuccess: () => { message.success('등록했습니다.'); form.resetFields() },
    onError: (error: unknown) => {
      // 서버가 준 필드 오류 → 각 입력 아래에 표시
      if (!applyFieldErrors(form, error) && !applyCodeError(form, error, { DUPLICATE_WORD_NAME: 'name' })) {
        message.error(errorMessage(error))
      }
    },
  })

<Form form={form} layout="vertical" onFinish={submit}>
  <Form.Item name="name" label="단어명" rules={[{ required: true, message: '단어명을 입력하세요.' }]}>
    <Input maxLength={50} showCount />
  </Form.Item>
  <Form.Item
    name="abbreviation"
    label="영문약어"
    normalize={value => value?.toUpperCase()}  // 백엔드와 같게 대문자로 정규화
    rules={[{ pattern: /^[A-Z][A-Z0-9_]*$/, message: '영문 대문자로 시작하고 영문·숫자·_ 만 씁니다.' }]}
  >
    <Input />
  </Form.Item>
  <Button type="primary" htmlType="submit" loading={createWord.isPending}>등록</Button>
</Form>`}
        />

        <SampleSection
          title="3. Table — 목록 화면의 본체"
          summary={
            <>
              <code>rowKey</code>는 반드시 준다(보통 <code>"id"</code>). 서버 페이징이므로{' '}
              <code>pagination</code>에 <code>total</code>과 <code>current</code>를 직접 넣고, 페이지를 바꾸면 URL을 바꾼다.
            </>
          }
          points={[
            <>
              <code>columns</code>는 컴포넌트 밖에 두거나 <code>useMemo</code>로 감싼다. 매 렌더 새 배열이면 표가 통째로 다시
              그려진다.
            </>,
            <>
              셀 꾸미기는 <code>render</code>에서 한다. 여러 화면이 같은 셀을 쓰면 <code>columnCells</code>처럼 따로 뺀다.
            </>,
            <>
              행 클릭으로 상세를 여는 화면은 <code>onRow</code>와 <code>rowClassName</code>(<code>clickable-row</code>,{' '}
              <code>selected-row</code>)을 쓴다.
            </>,
            <>
              <code>scroll={'{{ x: 900 }}'}</code>를 주면 좁은 화면에서 가로 스크롤이 생긴다.
            </>,
          ]}
          demo={<TableDemo />}
          code={`const columns: ColumnsType<WordResponse> = [
  { title: '단어명', dataIndex: 'name', width: 160 },
  { title: '영문약어', dataIndex: 'abbreviation', width: 140, render: value => <code>{value}</code> },
  {
    title: '사용 여부',
    dataIndex: 'used',
    width: 100,
    render: (used: boolean) => (used ? <Tag color="green">사용</Tag> : <Tag>미사용</Tag>),
  },
  {
    title: '',
    key: 'actions',
    width: 120,
    render: (_, row) => <Button size="small" onClick={() => setEditing(row)}>수정</Button>,
  },
]

<Table
  rowKey="id"
  columns={columns}
  dataSource={listQuery.data?.content}
  loading={listQuery.isPending}
  pagination={{
    current: page,                       // URL에서 온 값 (1부터)
    pageSize: size,
    total: listQuery.data?.totalElements ?? 0,
    showSizeChanger: true,
    onChange: (nextPage, nextSize) => setPage(nextPage, nextSize),
  }}
  onRow={row => ({ onClick: () => setSelectedId(row.id) })}
  rowClassName={row => (row.id === selectedId ? 'clickable-row selected-row' : 'clickable-row')}
/>`}
        />

        <SampleSection
          title="4. Modal과 Drawer"
          summary={
            <>
              등록·수정 폼은 <code>Modal</code>, 읽기 위주의 상세는 <code>Drawer</code>를 쓴다. 하위 목록이 주 내용인
              상세(테이블의 컬럼)는 별도 라우트로 만든다.
            </>
          }
          points={[
            <>
              <code>destroyOnHidden</code>을 주면 닫을 때 내용이 사라져 다음에 열 때 이전 입력이 남지 않는다.
            </>,
            <>
              <code>confirmLoading</code>으로 저장 중 버튼을 잠근다. 저장은 <code>onOk</code>에서 <code>form.submit()</code>을
              부르는 방식이 흔하다.
            </>,
            <>
              <code>Drawer</code>는 <code>size="large"</code>와 <code>extra</code>(오른쪽 위 버튼)를 자주 쓴다.
            </>,
          ]}
          demo={<ModalDemo />}
          code={`<Modal
  open={open}
  title={editing ? '표준 단어 수정' : '표준 단어 등록'}
  okText="저장"
  cancelText="취소"
  confirmLoading={saving}
  destroyOnHidden          // 6에서 destroyOnClose → destroyOnHidden
  forceRender              // 모달 안 폼의 form 인스턴스를 미리 연결
  onOk={() => form.submit()}
  onCancel={onClose}
>
  <Form form={form} layout="vertical" onFinish={submit}>...</Form>
</Modal>

<Drawer open={open} onClose={onClose} size="large" title="신청 상세" extra={<Button>승인</Button>}>
  <Descriptions column={1} bordered size="small">...</Descriptions>
</Drawer>`}
        />

        <SampleSection
          title="5. Select — 서버에서 검색하는 선택 박스"
          summary={
            <>
              후보가 많은 참조(단어·도메인·용어·테이블)는 전부 내려받지 않고 <b>입력할 때마다 서버에서 검색</b>한다. Ant
              Design 6은 검색 설정을 <code>showSearch</code> <b>객체</b>로만 받는다.
            </>
          }
          points={[
            <>
              <code>filterOption: false</code>로 로컬 필터를 끈다. 켜 두면 서버가 준 결과를 또 걸러 버린다.
            </>,
            <>
              입력마다 부르지 않도록 <code>useDebouncedValue</code>를 거친다.
            </>,
            <>
              수정 화면·URL 복원처럼 <b>선택된 값이 검색 결과에 없을 때</b>는 단건 조회로 라벨을 채운다(
              <code>ClassifierWordSelect</code> 참고).
            </>,
          ]}
          demo={<SelectDemo />}
          code={`export function WordSelect({ value, onChange }: WordSelectProps) {
  const [keyword, setKeyword] = useState('')
  const debounced = useDebouncedValue(keyword, 300)

  const listQuery = $api.useQuery('get', '/api/words', { params: { query: { name: debounced, size: 20 } } })
  // 선택된 값이 검색 결과에 없을 수 있으니 단건 조회로 라벨을 채운다
  const selectedQuery = $api.useQuery('get', '/api/words/{id}', { params: { path: { id: value! } } }, { enabled: !!value })

  const options = useMemo(() => {
    const rows = listQuery.data?.content ?? []
    const selected = selectedQuery.data
    const merged = selected && !rows.some(row => row.id === selected.id) ? [selected, ...rows] : rows
    return merged.map(row => ({ value: row.id, label: row.name + ' (' + row.abbreviation + ')' }))
  }, [listQuery.data, selectedQuery.data])

  return (
    <Select
      value={value}
      onChange={onChange}
      options={options}
      loading={listQuery.isFetching}
      allowClear
      placeholder="단어 검색"
      // Ant Design 6: 최상위 onSearch·filterOption이 없다. showSearch 객체로 준다
      showSearch={{ filterOption: false, onSearch: setKeyword }}
    />
  )
}`}
          codeTitle="src/components/WordSelect.tsx (요약)"
        />

        <SampleSection
          title="6. 정보 표시 — Descriptions · Tag · Typography"
          summary={
            <>
              상세는 <code>Descriptions</code>로 라벨과 값을 나란히 보여 주고, 상태·분류는 <code>Tag</code>로, 보조 설명은{' '}
              <code>Typography.Text type="secondary"</code>로 쓴다.
            </>
          }
          points={[
            <>
              <code>column={'{{ xs: 1, md: 2 }}'}</code>처럼 반응형으로 주면 좁은 화면에서 한 줄씩 나온다.
            </>,
            <>
              일시는 직접 포맷하지 않고 <code>formatDateTime</code>(<code>src/utils/format.ts</code>)을 쓴다. 값이 없으면{' '}
              <code>-</code>를 보여 준다.
            </>,
            <>
              물리명·코드처럼 고정폭이 어울리는 값은 <code>className="mono"</code>를 준다.
            </>,
          ]}
          demo={<DescriptionsDemo />}
          code={`<Descriptions column={{ xs: 1, md: 2 }} bordered size="small">
  <Descriptions.Item label="단어명">{word.name}</Descriptions.Item>
  <Descriptions.Item label="영문약어"><span className="mono">{word.abbreviation}</span></Descriptions.Item>
  <Descriptions.Item label="사용 여부">
    {word.used ? <Tag color="green">사용</Tag> : <Tag>미사용</Tag>}
  </Descriptions.Item>
  <Descriptions.Item label="등록 일시">{formatDateTime(word.createdAt)}</Descriptions.Item>
</Descriptions>`}
        />

        <SampleSection
          title="7. 테마와 로케일"
          summary={
            <>
              색·모서리 같은 전역 값은 컴포넌트마다 고치지 않고 <code>ConfigProvider</code>의 토큰으로 바꾼다. 날짜 선택기,
              페이지네이션의 한국어도 여기서 온다.
            </>
          }
          points={[
            <>
              감싸는 순서: <code>ConfigProvider</code> → <code>AntdApp</code> → <code>QueryClientProvider</code> →{' '}
              <code>AuthProvider</code> → <code>RouterProvider</code>.
            </>,
            <>
              화면 고유의 여백·색은 <code>src/index.css</code>의 클래스로 둔다. 인라인 <code>style</code>을 흩뿌리지 않는다.
            </>,
            <>
              이 프로젝트는 <code>dayjs</code>를 쓰지 않아 날짜 입력에 <code>{'<Input type="date">'}</code>를 쓴다(
              <code>date-input</code> 클래스).
            </>,
          ]}
          code={`// src/main.tsx
<ConfigProvider locale={koKR} theme={{ token: { colorPrimary: '#1f6feb', borderRadius: 6 } }}>
  <AntdApp>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  </AntdApp>
</ConfigProvider>`}
          codeTitle="src/main.tsx"
        />

        <SampleSection
          title="8. Ant Design 5 자료를 볼 때 바꿔 읽을 것"
          summary={<>검색해서 나온 코드가 동작하지 않으면 대부분 아래 중 하나다.</>}
          demo={
            <Table
              size="small"
              pagination={false}
              rowKey="topic"
              dataSource={RENAMES}
              columns={[
                { title: '컴포넌트', dataIndex: 'topic', width: 140 },
                { title: '5.x 자료', dataIndex: 'before', width: 260, render: (value: string) => <code>{value}</code> },
                { title: '이 프로젝트(6.x)', dataIndex: 'after', render: (value: string) => <code>{value}</code> },
              ]}
            />
          }
        />

        <SampleSection
          title="9. 이 프로젝트의 화면 골격"
          summary={
            <>
              새 목록 화면은 아래 순서를 그대로 따른다. <code>src/features/words/WordListPage.tsx</code>가 기준 예시다.
            </>
          }
          points={[
            <>
              제목 줄은 <code>PageHeader</code>(<code>title</code>, <code>extra</code>에 버튼). 상세 화면은{' '}
              <code>onBack</code>으로 목록에 돌아간다.
            </>,
            <>
              검색 줄은 <code>list-toolbar</code> 클래스, 입력은 <code>search-input</code>·<code>filter-select</code>.
            </>,
            <>
              버튼 노출은 권한으로 정한다: <code>const {'{ can }'} = useMenus()</code> →{' '}
              <code>can('/words', 'create')</code>. 서버도 같은 권한을 검사한다.
            </>,
            <>
              마지막에 <code>src/layouts/screens.tsx</code>의 <code>PAGES</code>에 메뉴 경로로 등록한다.
            </>,
          ]}
          code={`export function WordListPage() {
  const { can } = useMenus()
  const [editing, setEditing] = useState<WordResponse | null>(null)
  const [open, setOpen] = useState(false)

  return (
    <>
      <PageHeader
        title="표준 단어"
        extra={
          <Space>
            <ExcelActions downloadPath="/api/excel/words" uploadPath="/api/excel/words" />
            {can('/words', 'create') && (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditing(null); setOpen(true) }}>
                등록
              </Button>
            )}
          </Space>
        }
      />

      <div className="list-toolbar">
        <Input.Search className="search-input" placeholder="단어명" allowClear onSearch={value => setFilters({ name: value })} />
        <Select className="filter-select" allowClear placeholder="사용 여부" options={USED_OPTIONS} onChange={...} />
      </div>

      <Table rowKey="id" columns={columns} dataSource={listQuery.data?.content} loading={listQuery.isPending} pagination={...} />

      <WordFormModal open={open} word={editing} onClose={() => setOpen(false)} />
    </>
  )
}`}
          codeTitle="src/features/words/WordListPage.tsx (골격)"
        />
      </Space>
    </>
  )
}

/* ------------------------------------------------------------------ */
/* 데모 컴포넌트                                                        */
/* ------------------------------------------------------------------ */

interface RenameRow {
  topic: string
  before: string
  after: string
}

const RENAMES: RenameRow[] = [
  { topic: 'Alert', before: 'message="..."', after: 'title="..."' },
  { topic: 'Space', before: 'direction="vertical"', after: 'orientation="vertical"' },
  { topic: 'Modal', before: 'destroyOnClose', after: 'destroyOnHidden' },
  { topic: 'Select', before: 'showSearch onSearch={fn} filterOption={false}', after: 'showSearch={{ filterOption: false, onSearch: fn }}' },
  { topic: 'Timeline', before: 'items=[{ children: ... }]', after: 'items=[{ content: ... }]' },
  { topic: 'notification', before: "notification.info({ message, description })", after: 'notification.info({ title, description })' },
  { topic: 'message', before: "import { message } from 'antd'", after: 'const { message } = App.useApp()' },
]

/** 1번 데모: message · notification · modal.confirm */
function FeedbackDemo() {
  const { message, modal, notification } = App.useApp()

  const confirmDelete = () =>
    modal.confirm({
      title: '삭제할까요?',
      content: '삭제한 단어는 되돌릴 수 없습니다.',
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '닫기',
      // Promise를 돌려주면 확인 버튼이 로딩 상태가 된다 (여기서는 서버 호출 흉내).
      onOk: () =>
        new Promise<void>((resolve) => setTimeout(resolve, 800)).then(() => {
          message.success('삭제했습니다.')
        }),
    })

  return (
    <Space wrap>
      <Button onClick={() => message.success('등록했습니다.')}>message.success</Button>
      <Button onClick={() => message.error('요청을 처리하지 못했습니다.')}>message.error</Button>
      <Button onClick={() => notification.info({ title: '알림', description: '오른쪽 위에 남는 알림입니다.' })}>
        notification
      </Button>
      <Button danger onClick={confirmDelete}>
        modal.confirm
      </Button>
    </Space>
  )
}

interface DemoWordForm {
  name: string
  abbreviation: string
  description?: string
}

/** 2번 데모: 검증 규칙과 서버 오류 표시 */
function FormDemo() {
  const { message } = App.useApp()
  const [form] = Form.useForm<DemoWordForm>()
  const [saved, setSaved] = useState<DemoWordForm | null>(null)

  const submit = (values: DemoWordForm) => {
    setSaved(values)
    message.success('검증을 통과했습니다. (서버 호출은 하지 않는 데모)')
  }

  /** 백엔드가 fieldErrors를 돌려준 상황을 흉내 낸다. 실제로는 applyFieldErrors(form, error)가 이 일을 한다. */
  const showServerError = () =>
    form.setFields([{ name: 'name', errors: ['이미 등록된 단어명입니다.'] }])

  return (
    <Form form={form} layout="vertical" onFinish={submit} className="narrow-form">
      <Form.Item name="name" label="단어명" rules={[{ required: true, message: '단어명을 입력하세요.' }]}>
        <Input maxLength={50} showCount placeholder="예: 고객" />
      </Form.Item>
      <Form.Item
        name="abbreviation"
        label="영문약어"
        // 백엔드가 대문자로 정규화하므로 화면에서도 같게 맞춘다.
        normalize={(value?: string) => value?.toUpperCase()}
        rules={[
          { required: true, message: '영문약어를 입력하세요.' },
          { pattern: /^[A-Z][A-Z0-9_]*$/, message: '영문 대문자로 시작하고 영문·숫자·_ 만 씁니다.' },
        ]}
      >
        <Input placeholder="예: CUST" />
      </Form.Item>
      <Form.Item name="description" label="설명">
        <Input.TextArea rows={2} maxLength={200} showCount />
      </Form.Item>
      <Space wrap>
        <Button type="primary" htmlType="submit">
          저장
        </Button>
        <Button onClick={() => form.resetFields()}>초기화</Button>
        <Button onClick={showServerError}>서버 오류 표시 흉내</Button>
      </Space>
      {saved && (
        <Typography.Paragraph type="secondary" className="sample-form-result">
          제출된 값: {JSON.stringify(saved)}
        </Typography.Paragraph>
      )}
    </Form>
  )
}

interface DemoRow {
  id: number
  name: string
  abbreviation: string
  used: boolean
}

const DEMO_ROWS: DemoRow[] = [
  { id: 1, name: '고객', abbreviation: 'CUST', used: true },
  { id: 2, name: '계좌', abbreviation: 'ACCT', used: true },
  { id: 3, name: '거래', abbreviation: 'TRAN', used: false },
  { id: 4, name: '금액', abbreviation: 'AMT', used: true },
  { id: 5, name: '일자', abbreviation: 'DT', used: true },
]

/** 3번 데모: 행 클릭 선택과 셀 꾸미기 */
function TableDemo() {
  const { message } = App.useApp()
  const [selectedId, setSelectedId] = useState<number | null>(null)

  // columns를 매 렌더 새로 만들면 표가 통째로 다시 그려진다.
  const columns = useMemo(
    () => [
      { title: '단어명', dataIndex: 'name', width: 140 },
      {
        title: '영문약어',
        dataIndex: 'abbreviation',
        width: 140,
        render: (value: string) => <span className="mono">{value}</span>,
      },
      {
        title: '사용 여부',
        dataIndex: 'used',
        width: 110,
        render: (used: boolean) => (used ? <Tag color="green">사용</Tag> : <Tag>미사용</Tag>),
      },
      {
        title: '',
        key: 'actions',
        width: 100,
        render: (_: unknown, row: DemoRow) => (
          <Button
            size="small"
            onClick={(event) => {
              event.stopPropagation() // 행 클릭까지 번지지 않게 막는다
              message.info(`${row.name} 수정 (데모)`)
            }}
          >
            수정
          </Button>
        ),
      },
    ],
    [message],
  )

  return (
    <Space orientation="vertical" className="full-width">
      <Table
        size="small"
        rowKey="id"
        columns={columns}
        dataSource={DEMO_ROWS}
        pagination={{ pageSize: 3, showSizeChanger: false }}
        onRow={(row) => ({ onClick: () => setSelectedId(row.id) })}
        rowClassName={(row) => (row.id === selectedId ? 'clickable-row selected-row' : 'clickable-row')}
      />
      <Typography.Text type="secondary">
        {selectedId ? `선택한 행 id: ${selectedId}` : '행을 클릭하면 선택된다(상세 드로어를 여는 자리).'}
      </Typography.Text>
    </Space>
  )
}

/** 4번 데모: 폼을 담은 모달과 읽기용 드로어 */
function ModalDemo() {
  const { message } = App.useApp()
  const [modalOpen, setModalOpen] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<{ name: string }>()

  const submit = ({ name }: { name: string }) => {
    setSaving(true)
    // 서버 호출 흉내. 실제로는 mutation의 isPending을 confirmLoading에 준다.
    window.setTimeout(() => {
      setSaving(false)
      setModalOpen(false)
      message.success(`${name} 저장 (데모)`)
    }, 700)
  }

  return (
    <>
      <Space wrap>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          모달 열기
        </Button>
        <Button onClick={() => setDrawerOpen(true)}>드로어 열기</Button>
      </Space>

      <Modal
        open={modalOpen}
        title="표준 단어 등록"
        okText="저장"
        cancelText="취소"
        confirmLoading={saving}
        destroyOnHidden
        forceRender
        onOk={() => form.submit()}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="name" label="단어명" rules={[{ required: true, message: '단어명을 입력하세요.' }]}>
            <Input placeholder="예: 고객" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        size="large"
        title="표준 단어 상세"
        extra={<Button onClick={() => message.info('수정 (데모)')}>수정</Button>}
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="단어명">고객</Descriptions.Item>
          <Descriptions.Item label="영문약어">
            <span className="mono">CUST</span>
          </Descriptions.Item>
          <Descriptions.Item label="사용 여부">
            <Tag color="green">사용</Tag>
          </Descriptions.Item>
        </Descriptions>
      </Drawer>
    </>
  )
}

/** 5번 데모: 서버 검색을 흉내 낸 Select. 실제로는 $api.useQuery가 목록을 준다 */
function SelectDemo() {
  const [keyword, setKeyword] = useState('')
  const [value, setValue] = useState<number>()
  const debounced = useDebouncedValue(keyword, 300)

  // 실제 화면에서는 이 자리에 $api.useQuery('get', '/api/words', ...)가 들어간다.
  const options = useMemo(
    () =>
      DEMO_ROWS.filter((row) => row.name.includes(debounced.trim()) || row.abbreviation.includes(debounced.trim().toUpperCase())).map(
        (row) => ({ value: row.id, label: `${row.name} (${row.abbreviation})` }),
      ),
    [debounced],
  )

  return (
    <Space orientation="vertical" className="full-width">
      <Select
        className="filter-select"
        value={value}
        onChange={setValue}
        options={options}
        allowClear
        placeholder="단어 검색"
        suffixIcon={<SearchOutlined />}
        notFoundContent="검색 결과가 없습니다."
        // Ant Design 6: 최상위 onSearch·filterOption이 없다. showSearch 객체로 준다.
        showSearch={{ filterOption: false, onSearch: setKeyword }}
      />
      <Typography.Text type="secondary">
        입력한 검색어(0.3초 디바운스): <code>{debounced || '(없음)'}</code> · 선택한 id: <code>{value ?? '-'}</code>
      </Typography.Text>
    </Space>
  )
}

/** 6번 데모: 상세 화면의 기본 모양 */
function DescriptionsDemo() {
  return (
    <Card size="small">
      <Descriptions column={{ xs: 1, md: 2 }} bordered size="small">
        <Descriptions.Item label="단어명">고객</Descriptions.Item>
        <Descriptions.Item label="영문약어">
          <span className="mono">CUST</span>
        </Descriptions.Item>
        <Descriptions.Item label="사용 여부">
          <Tag color="green">사용</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="등록 일시">2026-09-16 10:24:11</Descriptions.Item>
        <Descriptions.Item label="설명" span={2}>
          <Typography.Text type="secondary">서비스를 이용하는 개인 또는 법인</Typography.Text>
        </Descriptions.Item>
      </Descriptions>
    </Card>
  )
}
