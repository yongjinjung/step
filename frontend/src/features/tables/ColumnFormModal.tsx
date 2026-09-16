import { Alert, App, Button, Col, Form, Input, InputNumber, Modal, Row, Switch } from 'antd'
import { useEffect } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { CodeGroupSelect } from '@/components/CodeGroupSelect'
import { DataTypeSelect } from '@/components/DataTypeSelect'
import { TermSelect } from '@/components/TermSelect'
import type { TermResponse } from '@/features/terms/termFields'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { DATA_TYPE_SPECS, formatDataType, MAX_DECIMAL_SCALE, usesLength, usesScale, type DataType } from '@/utils/dataType'
import {
  COLUMN_ERROR_FIELDS,
  inspectColumn,
  PHYSICAL_NAME_MAX_LENGTH,
  PHYSICAL_NAME_PATTERN,
  type ColumnIssue,
  type MetaColumnRequest,
  type MetaColumnResponse,
} from './tableFields'

type ColumnFormValues = Partial<MetaColumnRequest>

interface ColumnFormModalProps {
  open: boolean
  tableId: number
  /** 수정할 컬럼. null이면 등록 */
  column: MetaColumnResponse | null
  /** 등록할 때 채울 순서 (현재 마지막 순서 + 1) */
  nextSeq: number
  onClose: () => void
}

const INITIAL_VALUES: ColumnFormValues = { nullable: true, primaryKey: false }

/** 저장 전 미리보기에 보여줄 사유별 설명. 용어의 표준 값을 함께 알려준다. */
function describeIssue(issue: ColumnIssue, term: TermResponse | undefined): string {
  switch (issue) {
    case 'NO_TERM':
      return '매핑한 용어가 없어 비표준(용어 미매핑)으로 검출됩니다.'
    case 'NAME_MISMATCH':
      return `물리명이 용어 영문약어 ${term?.abbreviation}와(과) 다릅니다.`
    case 'TYPE_MISMATCH':
      return `데이터 타입이 도메인 ${term?.domainName}의 ${term?.dataType}와(과) 다릅니다.`
    case 'LENGTH_MISMATCH':
      return `길이·소수점이 도메인 ${term?.domainName}의 ${formatDataType(term?.dataType, term?.dataLength, term?.dataScale)}와(과) 다릅니다.`
  }
}

/**
 * 컬럼 정의 등록·수정 모달.
 *
 * - 매핑 용어를 고르면 표준 값(물리명=영문약어, 논리명=용어명, 도메인 타입·길이)을 채울 수 있다.
 *   등록 중에는 고르는 즉시 채우고, 수정 중에는 입력한 값을 덮어쓰지 않도록 버튼으로만 채운다.
 * - 입력한 값으로 비표준 검출 결과를 미리 보여준다(inspectColumn). 비표준이어도 저장은 된다.
 * - 기본키를 켜면 NULL 허용을 끈다 (서버 규칙 PRIMARY_KEY_NULLABLE).
 * - 코드성 컬럼은 공통 코드 그룹을 연결한다(선택). 검출 결과와는 무관하다.
 */
export function ColumnFormModal({ open, tableId, column, nextSeq, onClose }: ColumnFormModalProps) {
  const isEdit = column?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<ColumnFormValues>()

  const termId = Form.useWatch('termId', form)
  const physicalName = Form.useWatch('physicalName', form)
  const dataType = Form.useWatch('dataType', form)
  const dataLength = Form.useWatch('dataLength', form)
  const dataScale = Form.useWatch('dataScale', form)
  const primaryKey = Form.useWatch('primaryKey', form)

  // TermSelect의 단건 조회와 조회 키가 같아 추가 요청 없이 캐시를 쓰는 경우가 많다.
  const termQuery = $api.useQuery(
    'get',
    '/api/terms/{id}',
    { params: { path: { id: termId ?? 0 } } },
    { enabled: open && termId !== undefined },
  )
  const term = termId !== undefined ? termQuery.data : undefined
  // 용어를 골랐는데 아직 불러오는 중이면 미리보기를 보여주지 않는다 (NO_TERM으로 잘못 보이지 않게).
  const issues = termId !== undefined && !term ? undefined : inspectColumn({ physicalName, dataType, dataLength, dataScale }, term)

  const createColumn = $api.useMutation('post', '/api/tables/{tableId}/columns')
  const updateColumn = $api.useMutation('put', '/api/tables/{tableId}/columns/{columnId}')

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    if (column) {
      form.setFieldsValue({
        seq: column.seq,
        physicalName: column.physicalName,
        logicalName: column.logicalName,
        termId: column.termId,
        codeGroupId: column.codeGroupId,
        dataType: column.dataType,
        dataLength: column.dataLength,
        dataScale: column.dataScale,
        nullable: column.nullable,
        primaryKey: column.primaryKey,
      })
    } else {
      form.setFieldsValue({ seq: nextSeq })
    }
    // nextSeq는 의존성에서 뺀다. 열 때의 값만 쓰고, 열린 동안 목록이 바뀌어도 입력 중인 순서를 바꾸지 않는다.
  }, [open, column, form])

  /** 용어의 표준 값으로 물리명·논리명·타입·길이·소수점을 채운다. */
  const fillFromTerm = (source: TermResponse) => {
    const type = source.dataType
    form.setFields([
      { name: 'physicalName', value: source.abbreviation, errors: [] },
      { name: 'logicalName', value: source.name, errors: [] },
      { name: 'dataType', value: type, errors: [] },
      { name: 'dataLength', value: usesLength(type) ? source.dataLength : undefined, errors: [] },
      { name: 'dataScale', value: usesScale(type) ? source.dataScale : undefined, errors: [] },
    ])
  }

  /** 타입을 바꾸면 그 타입에서 쓸 수 없는 길이·소수점을 비운다. */
  const handleTypeChange = (type: DataType | undefined) => {
    form.setFieldsValue({
      dataLength: usesLength(type) ? form.getFieldValue('dataLength') : undefined,
      dataScale: usesScale(type) ? form.getFieldValue('dataScale') : undefined,
    })
  }

  const showError = (error: unknown) => {
    if (applyCodeError(form, error, COLUMN_ERROR_FIELDS) || applyFieldErrors(form, error)) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = (values: ColumnFormValues) => {
    const type = values.dataType as DataType
    const body: MetaColumnRequest = {
      physicalName: values.physicalName?.trim() ?? '',
      logicalName: values.logicalName?.trim() ?? '',
      // 비우면 본문에서 빠져 매핑이 해제된다.
      termId: values.termId ?? undefined,
      // 용어와 같이, 비우면 본문에서 빠져 코드 그룹 매핑이 해제된다.
      codeGroupId: values.codeGroupId ?? undefined,
      dataType: type,
      dataLength: usesLength(type) ? values.dataLength : undefined,
      dataScale: usesScale(type) ? values.dataScale : undefined,
      nullable: values.nullable ?? false,
      primaryKey: values.primaryKey ?? false,
      seq: values.seq as number,
    }
    const onSuccess = () => {
      message.success(isEdit ? '컬럼을 수정했습니다.' : '컬럼을 등록했습니다.')
      invalidate(
        '/api/tables/{tableId}/columns',
        '/api/tables/{tableId}/columns/{columnId}',
        '/api/columns/non-standard',
        '/api/code-groups/{id}/columns',
        '/api/histories',
      )
      onClose()
    }
    if (isEdit && column?.id !== undefined) {
      updateColumn.mutate(
        { params: { path: { tableId, columnId: column.id } }, body },
        { onSuccess, onError: showError },
      )
    } else {
      createColumn.mutate({ params: { path: { tableId } }, body }, { onSuccess, onError: showError })
    }
  }

  const lengthSpec = dataType ? DATA_TYPE_SPECS[dataType as DataType] : undefined

  return (
    <Modal
      open={open}
      title={isEdit ? '컬럼 수정' : '컬럼 추가'}
      okText="저장"
      confirmLoading={createColumn.isPending || updateColumn.isPending}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={640}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} initialValues={INITIAL_VALUES}>
        <Form.Item
          name="termId"
          label="매핑 용어"
          extra={
            <Button
              type="link"
              size="small"
              className="inline-link"
              disabled={!term}
              onClick={() => term && fillFromTerm(term)}
            >
              {term ? `용어 기준 값으로 채우기: ${term.abbreviation} · ${formatDataType(term.dataType, term.dataLength, term.dataScale)}` : '용어를 고르면 표준 값으로 채울 수 있습니다 (선택)'}
            </Button>
          }
        >
          <TermSelect allowClear onChange={(_, selected) => !isEdit && selected && fillFromTerm(selected)} />
        </Form.Item>

        <Form.Item
          name="codeGroupId"
          label="공통 코드"
          extra="상태·구분 같은 코드성 컬럼이면 값 목록인 코드 그룹을 연결합니다 (선택). 사용 중인 그룹만 새로 고를 수 있습니다."
        >
          <CodeGroupSelect allowClear />
        </Form.Item>

        <Row gutter={12}>
          <Col xs={24} sm={12}>
            <Form.Item
              name="physicalName"
              label="물리명"
              normalize={(value?: string) => value?.toUpperCase()}
              rules={[
                { required: true, message: '물리명을 입력하세요.' },
                { max: PHYSICAL_NAME_MAX_LENGTH, message: `${PHYSICAL_NAME_MAX_LENGTH}자 이내로 입력하세요.` },
                { pattern: PHYSICAL_NAME_PATTERN, message: '영문자로 시작하는 영문/숫자/_만 사용할 수 있습니다.' },
              ]}
            >
              <Input placeholder="예: CUST_NO" maxLength={PHYSICAL_NAME_MAX_LENGTH} className="mono" />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12}>
            <Form.Item
              name="logicalName"
              label="논리명"
              rules={[
                { required: true, whitespace: true, message: '논리명을 입력하세요.' },
                { max: 200, message: '200자 이내로 입력하세요.' },
              ]}
            >
              <Input placeholder="예: 고객번호" maxLength={200} />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={12}>
          <Col xs={24} sm={10}>
            <Form.Item name="dataType" label="데이터 타입" rules={[{ required: true, message: '데이터 타입을 선택하세요.' }]}>
              <DataTypeSelect onChange={handleTypeChange} />
            </Form.Item>
          </Col>
          <Col xs={12} sm={7}>
            <Form.Item
              name="dataLength"
              label="길이"
              dependencies={['dataType']}
              rules={[{ required: usesLength(dataType as DataType | undefined), message: '길이를 입력하세요.' }]}
            >
              <InputNumber
                className="full-width"
                min={1}
                max={lengthSpec?.maxLength || undefined}
                precision={0}
                disabled={!usesLength(dataType as DataType | undefined)}
                placeholder={usesLength(dataType as DataType | undefined) ? `1~${lengthSpec?.maxLength}` : '없음'}
              />
            </Form.Item>
          </Col>
          <Col xs={12} sm={7}>
            <Form.Item
              name="dataScale"
              label="소수점"
              dependencies={['dataType', 'dataLength']}
              rules={[
                ({ getFieldValue }) => ({
                  validator: (_, value?: number) =>
                    value === undefined || value === null || value <= (getFieldValue('dataLength') ?? 0)
                      ? Promise.resolve()
                      : Promise.reject(new Error('길이보다 클 수 없습니다.')),
                }),
              ]}
            >
              <InputNumber
                className="full-width"
                min={0}
                max={MAX_DECIMAL_SCALE}
                precision={0}
                disabled={!usesScale(dataType as DataType | undefined)}
                placeholder={usesScale(dataType as DataType | undefined) ? '비우면 0' : '없음'}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={12}>
          <Col xs={24} sm={8}>
            <Form.Item name="seq" label="순서" rules={[{ required: true, message: '순서를 입력하세요.' }]}>
              <InputNumber className="full-width" min={1} precision={0} />
            </Form.Item>
          </Col>
          <Col xs={12} sm={8}>
            <Form.Item name="primaryKey" label="기본키" valuePropName="checked">
              <Switch
                checkedChildren="PK"
                unCheckedChildren="아님"
                onChange={(checked) => checked && form.setFields([{ name: 'nullable', value: false, errors: [] }])}
              />
            </Form.Item>
          </Col>
          <Col xs={12} sm={8}>
            <Form.Item
              name="nullable"
              label="NULL 허용"
              valuePropName="checked"
              extra={primaryKey ? '기본키는 NULL을 허용할 수 없습니다.' : undefined}
            >
              <Switch checkedChildren="Y" unCheckedChildren="N" disabled={primaryKey} />
            </Form.Item>
          </Col>
        </Row>

        {issues &&
          (issues.length === 0 ? (
            <Alert type="success" showIcon title="매핑 용어 기준으로 표준 컬럼입니다." />
          ) : (
            <Alert
              type={issues[0] === 'NO_TERM' ? 'info' : 'warning'}
              showIcon
              title="저장하면 비표준 컬럼으로 검출됩니다. (저장은 할 수 있습니다)"
              description={
                <ul className="plain-list">
                  {issues.map((issue) => (
                    <li key={issue}>{describeIssue(issue, term)}</li>
                  ))}
                </ul>
              }
            />
          ))}
      </Form>
    </Modal>
  )
}
