import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, App, Button, Form, Input, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { PageHeader } from '@/components/PageHeader'
import { SampleSection } from './SampleSection'

/**
 * 폼과 검증 샘플 화면.
 *
 * Ant Design 샘플의 2번(Form 기본)을 이미 봤다는 전제로, 이 프로젝트의 폼이 실제로 겪는 상황
 * (동적 목록, 다른 필드를 보고 미리보기 만들기, 서버가 돌려준 필드 오류 표시, 권한별 분기)을 다룬다.
 */
export function FormsSamplePage() {
  return (
    <>
      <PageHeader title="폼과 검증 샘플" subTitle="Form.List · 서버 오류 매핑 · 신청/승인 분기" />

      <Space orientation="vertical" size="large" className="full-width">
        <Alert
          type="info"
          showIcon
          title="먼저 볼 것"
          description={
            <>
              <code>Form.useForm()</code>, <code>rules</code>, <code>onFinish</code> 같은 기본기는 "Ant Design 6" 샘플
              2번에 있다. 여기서는 이 프로젝트의 실제 폼(<code>WordFormModal</code>, <code>TermFormModal</code>,{' '}
              <code>ColumnFormModal</code>)이 겪는 더 까다로운 상황만 다룬다.
            </>
          }
        />

        <SampleSection
          title="1. 정규화 — normalize로 백엔드와 규칙 맞추기"
          summary={
            <>
              사용자가 입력한 값을 검증하기 <b>전에</b> 서버가 저장할 형태로 바꿔야 하는 필드가 있다(영문약어 대문자화,
              앞뒤 공백 제거). <code>Form.Item</code>의 <code>normalize</code>가 그 자리다.
            </>
          }
          points={[
            <>
              <code>normalize</code>는 값이 <b>폼 상태에 들어가기 전</b>에 실행된다. 그래서 <code>rules</code>의 정규식
              검사는 이미 정규화된 값을 본다.
            </>,
            <>
              백엔드가 저장 전에 정규화하는 규칙(예: 영문약어 대문자화)과 <b>같은 규칙</b>을 화면에도 둬야 사용자가
              저장 후에 값이 바뀌어 놀라지 않는다.
            </>,
          ]}
          demo={<NormalizeDemo />}
          code={`<Form.Item
  name="abbreviation"
  label="영문약어"
  normalize={(value?: string) => value?.toUpperCase()}   // 백엔드와 같게 대문자로 정규화
  rules={[{ pattern: /^[A-Z][A-Z0-9_]*$/, message: '영문 대문자로 시작하고 영문·숫자·_ 만 씁니다.' }]}
>
  <Input />
</Form.Item>`}
        />

        <SampleSection
          title="2. Form.List — 늘렸다 줄이는 목록"
          summary={
            <>
              용어의 구성 단어처럼 <b>몇 개가 될지 모르는 입력 줄</b>은 <code>useState</code> 배열이 아니라{' '}
              <code>Form.List</code>로 관리한다. 폼 검증·제출 값에 자동으로 포함된다.
            </>
          }
          points={[
            <>
              <code>{'{(fields, { add, remove, move }) => ...}'}</code> 형태로 쓴다. <code>fields</code>의 각 항목이 줄
              하나이고, <code>field.name</code>으로 <code>{"Form.Item name={[field.name, 'value']}"}</code>처럼 중첩
              이름을 만든다.
            </>,
            <>
              줄 순서가 의미 있으면(용어의 구성 단어 순서) <code>move(from, to)</code>로 바꾼다. 실제 화면은 위/아래
              버튼으로 <code>move(index, index - 1)</code>를 부른다.
            </>,
            <>
              실제 예: <code>TermFormModal</code>의 <code>wordIds</code> — 각 줄이 <code>WordSelect</code> 하나다.
            </>,
          ]}
          demo={<FormListDemo />}
          code={`<Form.List name="links" initialValue={['']}>
  {(fields, { add, remove, move }) => (
    <>
      {fields.map((field, index) => (
        <Space key={field.key} className="full-width" align="baseline">
          <Form.Item name={field.name} rules={[{ required: true, message: '값을 입력하세요.' }]}>
            <Input placeholder="참고 링크" />
          </Form.Item>
          <Button icon={<ArrowUpOutlined />} disabled={index === 0} onClick={() => move(index, index - 1)} />
          <Button icon={<DeleteOutlined />} danger onClick={() => remove(field.name)} />
        </Space>
      ))}
      <Button icon={<PlusOutlined />} onClick={() => add('')}>줄 추가</Button>
    </>
  )}
</Form.List>`}
        />

        <SampleSection
          title="3. Form.useWatch — 다른 필드를 보고 미리보기 만들기"
          summary={
            <>
              사용자가 직접 입력하지 않고 <b>다른 필드로 계산해서 보여주는</b> 값이 있다(용어명·영문약어는 구성 단어로
              조합한다). <code>Form.useWatch</code>로 그 필드들을 구독하고 화면 밖에서 계산한다.
            </>
          }
          points={[
            <>
              계산 결과는 <code>Form.Item</code>으로 감싸지 않는다(사용자가 고칠 수 없어야 한다). 그냥 렌더링만 한다.
            </>,
            <>
              실제 예: <code>TermFormModal</code>의 <code>composeTermName(words)</code> — 구성 단어를 조합해 용어명을
              미리 보여주고, 이 값 그대로 저장 요청에 담는다(백엔드 <code>Term.composeName</code>과 같은 규칙).
            </>,
          ]}
          demo={<WatchDemo />}
          code={`const wordNames = Form.useWatch('wordNames', form) ?? []
const domain = Form.useWatch('domain', form)

// 폼 바깥에서 계산한다 — Form.Item으로 감싸지 않는다 (사용자가 못 고치게)
const termName = wordNames.filter(Boolean).join('') + (domain ? \`(\${domain})\` : '')

<Typography.Text type="secondary">미리보기: {termName || '-'}</Typography.Text>`}
        />

        <SampleSection
          title="4. 서버가 돌려준 필드 오류 표시"
          summary={
            <>
              중복 이름, 참조 무결성 같은 검증은 <b>서버만</b> 안다. 저장 요청이 실패하면 응답의{' '}
              <code>fieldErrors</code>를 그 입력 아래에 그대로 보여준다.
            </>
          }
          points={[
            <>
              <code>applyFieldErrors(form, error)</code>가 <code>fieldErrors</code> 배열을{' '}
              <code>form.setFields(...)</code>로 바꿔 준다. 필드 경로 <code>wordIds[0]</code>는 폼 이름 경로{' '}
              <code>['wordIds', 0]</code>로 자동 변환된다.
            </>,
            <>
              변경 <b>신청</b> API는 내용이 <code>payload</code>로 한 번 감싸여서 오류 경로가{' '}
              <code>payload.name</code>이 된다. 이때는{' '}
              <code>{"applyFieldErrors(form, error, { stripPrefix: 'payload.' })"}</code>로 접두어를 뗀다.
            </>,
            <>
              업무 오류 코드(문자열)만 오고 필드 경로가 없는 경우는{' '}
              <code>{"applyCodeError(form, error, { DUPLICATE_WORD_NAME: 'name' })"}</code>로 코드→필드를 직접
              연결한다.
            </>,
          ]}
          demo={<ServerErrorDemo />}
          code={`const createWord = $api.useMutation('post', '/api/words')

createWord.mutate({ body: values }, {
  onError: (error: unknown) => {
    // 1) 필드 경로가 있는 오류(fieldErrors) 먼저 시도
    // 2) 없으면 업무 오류 코드 → 필드 매핑 시도
    // 3) 그래도 없으면 전체 메시지
    if (!applyFieldErrors(form, error) && !applyCodeError(form, error, { DUPLICATE_WORD_NAME: 'name' })) {
      message.error(errorMessage(error))
    }
  },
})`}
        />

        <SampleSection
          title="5. 필드 간 비교 검증"
          summary={
            <>
              "새 비밀번호 확인"처럼 <b>다른 필드 값과 비교</b>해야 하는 검증은 <code>rules</code>의 함수형 규칙과{' '}
              <code>dependencies</code>를 같이 쓴다.
            </>
          }
          points={[
            <>
              <code>dependencies={['password']}</code>가 없으면, <code>password</code>가 바뀌어도 이 필드의 검증이
              다시 돌지 않아 이미 표시된 오류가 낡은 채로 남는다.
            </>,
            <>
              실제 예: <code>MyPage</code>의 "새 비밀번호 확인".
            </>,
          ]}
          demo={<CrossFieldDemo />}
          code={`<Form.Item
  name="confirmPassword"
  label="새 비밀번호 확인"
  dependencies={['newPassword']}   // newPassword가 바뀌면 이 필드도 다시 검증한다
  rules={[
    { required: true, message: '새 비밀번호를 한 번 더 입력하세요.' },
    ({ getFieldValue }) => ({
      validator: (_, value) =>
        !value || getFieldValue('newPassword') === value
          ? Promise.resolve()
          : Promise.reject(new Error('새 비밀번호와 같지 않습니다.')),
    }),
  ]}
>
  <Input.Password />
</Form.Item>`}
        />

        <SampleSection
          title="6. 신청/승인 분기 — 같은 폼, 다른 저장 방식"
          summary={
            <>
              표준 데이터(단어·도메인·용어) 화면은 <b>같은 폼과 같은 버튼</b>이 권한에 따라 동작이 다르다: 권한이 있으면
              바로 저장, 없으면 변경 신청으로 접수된다. 폼을 두 개 만들지 않는다.
            </>
          }
          points={[
            <>
              <code>useMenus().can(path, action)</code> 하나로 어느 API를 부를지, 버튼 문구를 무엇으로 할지(
              <code>"등록"</code> vs <code>"등록 신청"</code>)를 함께 정한다.
            </>,
            <>
              신청 쪽은 응답 오류 경로가 <code>payload.xxx</code>라 4번의 <code>stripPrefix</code>가 필요하다.
            </>,
          ]}
          demo={<PermissionBranchDemo />}
          code={`const canCreate = can('/words', 'create')
const createWord = $api.useMutation('post', '/api/words')
const requestWord = $api.useMutation('post', '/api/requests/words')

const submit = (values: WordRequest) => {
  if (canCreate) {
    createWord.mutate({ body: values }, {
      onSuccess: () => message.success('등록했습니다.'),
      onError: (error: unknown) => applyFieldErrors(form, error) || message.error(errorMessage(error)),
    })
  } else {
    requestWord.mutate({ body: { requestType: 'CREATE', payload: values, reason } }, {
      onSuccess: () => message.success('등록을 신청했습니다. 관리자가 승인하면 반영됩니다.'),
      onError: (error: unknown) =>
        applyFieldErrors(form, error, { stripPrefix: 'payload.' }) || message.error(errorMessage(error)),
    })
  }
}

<Button type="primary" htmlType="submit">{canCreate ? '등록' : '등록 신청'}</Button>`}
        />

        <SampleSection
          title="7. 이 프로젝트의 폼 골격"
          summary={<>등록·수정 모달은 아래 구조를 그대로 따른다(`WordFormModal` 기준).</>}
          points={[
            <>
              등록/수정을 폼 하나로 처리한다: <code>editing</code>이 <code>null</code>이면 등록,{' '}
              <code>editing</code>이 있으면 <code>form.setFieldsValue(editing)</code>로 채운 뒤 수정.
            </>,
            <>
              모달이 열릴 때마다 이전 값이 남지 않도록 <code>destroyOnHidden</code>, 열기 전에 form 인스턴스를 미리
              연결하도록 <code>forceRender</code>를 함께 쓴다.
            </>,
            <>
              저장 버튼은 폼 밖(모달의 <code>onOk</code>)에서 <code>form.submit()</code>을 부르는 방식과, 폼 안에{' '}
              <code>htmlType="submit"</code> 버튼을 두는 방식 둘 다 쓴다 — 모달은 전자, 화면에 바로 있는 폼은 후자.
            </>,
          ]}
          code={`export function WordFormModal({ open, word, onClose }: WordFormModalProps) {
  const isEdit = word !== null
  const [form] = Form.useForm<WordFormValues>()

  useEffect(() => {
    if (open) form.setFieldsValue(word ?? DEFAULT_VALUES)
  }, [open, word, form])

  const submit = (values: WordFormValues) => { /* 4·6번 패턴 */ }

  return (
    <Modal
      open={open}
      title={isEdit ? '단어 수정' : '단어 등록'}
      okText="저장"
      confirmLoading={createWord.isPending || updateWord.isPending}
      destroyOnHidden
      forceRender
      onOk={() => form.submit()}
      onCancel={onClose}
    >
      <Form form={form} layout="vertical" onFinish={submit}>...</Form>
    </Modal>
  )
}`}
          codeTitle="src/features/words/WordFormModal.tsx (골격)"
        />
      </Space>
    </>
  )
}

/* ------------------------------------------------------------------ */
/* 데모 컴포넌트                                                        */
/* ------------------------------------------------------------------ */

/** 1번 데모: normalize로 입력을 대문자로 바꾼 뒤 검증한다 */
function NormalizeDemo() {
  const [form] = Form.useForm<{ abbreviation?: string }>()
  const value = Form.useWatch('abbreviation', form)

  return (
    <Form form={form} layout="vertical" className="narrow-form">
      <Form.Item
        name="abbreviation"
        label="영문약어"
        normalize={(input?: string) => input?.toUpperCase()}
        rules={[{ pattern: /^[A-Z][A-Z0-9_]*$/, message: '영문 대문자로 시작하고 영문·숫자·_ 만 씁니다.' }]}
      >
        <Input placeholder="소문자로 입력해도 대문자로 바뀐다 (예: cust)" />
      </Form.Item>
      <Typography.Text type="secondary">폼에 저장된 실제 값: {value ? <code>{value}</code> : '(없음)'}</Typography.Text>
    </Form>
  )
}

/** 2번 데모: Form.List로 늘렸다 줄이는 목록. move로 순서도 바꾼다 */
function FormListDemo() {
  const [form] = Form.useForm<{ links: string[] }>()

  return (
    <Form form={form} layout="vertical" initialValues={{ links: ['https://example.com/spec'] }} className="narrow-form">
      <Form.List name="links">
        {(fields, { add, remove, move }) => (
          <>
            {fields.map((field, index) => (
              <Space key={field.key} className="full-width sample-formlist-row" align="baseline">
                <Form.Item
                  name={field.name}
                  rules={[{ required: true, message: '값을 입력하세요.' }]}
                  className="sample-formlist-item"
                >
                  <Input placeholder="참고 링크" />
                </Form.Item>
                <Button size="small" disabled={index === 0} onClick={() => move(index, index - 1)}>
                  위로
                </Button>
                <Button size="small" icon={<DeleteOutlined />} danger onClick={() => remove(field.name)} />
              </Space>
            ))}
            <Button icon={<PlusOutlined />} onClick={() => add('')}>
              줄 추가
            </Button>
          </>
        )}
      </Form.List>
    </Form>
  )
}

const SAMPLE_WORD_OPTIONS = ['고객', '계좌', '거래', '일자', '금액']

/** 3번 데모: 두 필드(단어 선택 여러 개 + 도메인)를 보고 미리보기를 계산한다 */
function WatchDemo() {
  const [form] = Form.useForm<{ words: string[]; domain?: string }>()
  const words: string[] = Form.useWatch('words', form) ?? []
  const domain: string | undefined = Form.useWatch('domain', form)

  // 폼 필드가 아니라 계산 결과다 — 사용자가 직접 고치지 않는다.
  const composed = words.filter(Boolean).join('') + (domain ? `(${domain})` : '')

  return (
    <Form form={form} layout="vertical" initialValues={{ words: [] }} className="narrow-form">
      <Space wrap className="sample-word-picker">
        {SAMPLE_WORD_OPTIONS.map((word) => (
          <Tag.CheckableTag
            key={word}
            checked={words.includes(word)}
            onChange={(checked) => {
              const next = checked ? [...words, word] : words.filter((w) => w !== word)
              form.setFieldValue('words', next)
            }}
          >
            {word}
          </Tag.CheckableTag>
        ))}
      </Space>
      <Form.Item name="domain" label="도메인 (선택)" className="sample-form-gap-top">
        <Input placeholder="예: 문자" />
      </Form.Item>
      <Typography.Text type="secondary">조합 미리보기: {composed || '단어를 선택하세요'}</Typography.Text>
    </Form>
  )
}

/** 4번 데모: 서버가 돌려준 fieldErrors를 흉내 내 실제로 입력 아래에 표시한다 */
function ServerErrorDemo() {
  const { message } = App.useApp()
  const [form] = Form.useForm<{ name: string }>()

  const submit = ({ name }: { name: string }) => {
    // 실제로는 createWord.mutate(...)의 onError에서 applyFieldErrors(form, error)를 부른다.
    // 여기서는 "이미 등록된 이름"이라는 서버 응답을 흉내 낸다.
    if (name.trim() === '고객') {
      form.setFields([{ name: 'name', errors: ['이미 등록된 단어명입니다. (DUPLICATE_WORD_NAME)'] }])
      return
    }
    message.success('저장했습니다. ("고객"을 입력하면 서버 오류를 흉내 낸다)')
  }

  return (
    <Form form={form} layout="vertical" onFinish={submit} className="narrow-form">
      <Form.Item name="name" label="단어명" rules={[{ required: true, message: '단어명을 입력하세요.' }]}>
        <Input placeholder="'고객'을 입력해 서버 오류를 확인해 보세요" />
      </Form.Item>
      <Button type="primary" htmlType="submit">
        저장
      </Button>
    </Form>
  )
}

/** 5번 데모: dependencies로 다른 필드와 비교 검증 */
function CrossFieldDemo() {
  const [form] = Form.useForm<{ password: string; confirm: string }>()

  return (
    <Form form={form} layout="vertical" className="narrow-form">
      <Form.Item name="password" label="새 비밀번호" rules={[{ required: true, message: '비밀번호를 입력하세요.' }]}>
        <Input.Password />
      </Form.Item>
      <Form.Item
        name="confirm"
        label="새 비밀번호 확인"
        dependencies={['password']}
        rules={[
          { required: true, message: '한 번 더 입력하세요.' },
          ({ getFieldValue }) => ({
            validator: (_, value) =>
              !value || getFieldValue('password') === value ? Promise.resolve() : Promise.reject(new Error('비밀번호와 같지 않습니다.')),
          }),
        ]}
      >
        <Input.Password />
      </Form.Item>
    </Form>
  )
}

/** 6번 데모: 권한 스위치 하나로 버튼 문구·동작이 바뀌는 것을 흉내 낸다 */
function PermissionBranchDemo() {
  const { message } = App.useApp()
  const [canCreate, setCanCreate] = useState(true)

  const submit = () => {
    message.success(canCreate ? '바로 등록했습니다. (POST /api/words)' : '등록을 신청했습니다. (POST /api/requests/words)')
  }

  return (
    <Space orientation="vertical" className="full-width">
      <Space align="center">
        <Typography.Text>내 권한:</Typography.Text>
        <Button size="small" type={canCreate ? 'primary' : 'default'} onClick={() => setCanCreate(true)}>
          등록 권한 있음
        </Button>
        <Button size="small" type={!canCreate ? 'primary' : 'default'} onClick={() => setCanCreate(false)}>
          등록 권한 없음
        </Button>
      </Space>
      <Button type="primary" onClick={submit}>
        {canCreate ? '등록' : '등록 신청'}
      </Button>
    </Space>
  )
}
