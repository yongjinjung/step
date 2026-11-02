import { Alert, App, Button, Col, Form, Input, InputNumber, Modal, Row } from 'antd'
import { useEffect, useState } from 'react'
import { $api } from '@/api/client'
import { applyCodeError, applyFieldErrors, errorMessage } from '@/api/errors'
import { useMenus } from '@/layouts/MenuContext'
import { ClassifierWordSelect } from '@/components/ClassifierWordSelect'
import { DataTypeSelect } from '@/components/DataTypeSelect'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import {
  DATA_TYPE_SPECS,
  MAX_DECIMAL_SCALE,
  suggestDomainName,
  usesLength,
  usesScale,
  type DataType,
} from '@/utils/dataType'
import { DOMAIN_ERROR_FIELDS, type DomainRequest, type DomainResponse } from './domainFields'

type DomainFormValues = Partial<DomainRequest> & { reason?: string }

interface DomainFormModalProps {
  open: boolean
  /** 수정할 도메인. null이면 등록 */
  domain: DomainResponse | null
  onClose: () => void
}

/**
 * 도메인 등록·수정 모달.
 *
 * - 입력 순서: 분류어 → 데이터 타입 → 길이/소수점 → 도메인명. 앞의 값으로 도메인명을 추천한다(번호V20, 금액N18.2).
 * - 길이·소수점 입력은 타입 규칙(utils/dataType)에 따라 켜지고 꺼진다. 최종 검증은 서버가 한다.
 * - ADMIN은 바로 저장, USER는 변경 신청으로 보낸다.
 */
export function DomainFormModal({ open, domain, onClose }: DomainFormModalProps) {
  const { can } = useMenus()
  // 등록·수정 권한이 있으면 바로 반영하고, 없으면 변경 신청으로 접수된다.
  const canSave = can('/domains', domain?.id === undefined ? 'create' : 'update')
  const isEdit = domain?.id !== undefined
  const { message } = App.useApp()
  const invalidate = useInvalidateApi()
  const [form] = Form.useForm<DomainFormValues>()
  /** 선택한 분류어 단어명. 도메인명 추천에 쓴다 (폼 값에는 ID만 있다) */
  const [classifierName, setClassifierName] = useState<string>()

  const dataType = Form.useWatch('dataType', form)
  const dataLength = Form.useWatch('dataLength', form)
  const dataScale = Form.useWatch('dataScale', form)

  const createDomain = $api.useMutation('post', '/api/domains')
  const updateDomain = $api.useMutation('put', '/api/domains/{id}')
  const requestDomain = $api.useMutation('post', '/api/requests/domains')
  const saving = createDomain.isPending || updateDomain.isPending || requestDomain.isPending

  useEffect(() => {
    if (!open) {
      return
    }
    form.resetFields()
    setClassifierName(domain?.classifierWordName)
    if (domain) {
      form.setFieldsValue({
        name: domain.name,
        classifierWordId: domain.classifierWordId,
        dataType: domain.dataType,
        dataLength: domain.dataLength,
        // DECIMAL이 아니면 서버 값이 null이다.
        dataScale: domain.dataScale,
        description: domain.description,
      })
    }
  }, [open, domain, form])

  /** 타입을 바꾸면 그 타입에서 쓸 수 없는 길이·소수점을 비운다. */
  const handleTypeChange = (type: DataType | undefined) => {
    form.setFieldsValue({
      dataLength: usesLength(type) ? form.getFieldValue('dataLength') : undefined,
      dataScale: usesScale(type) ? form.getFieldValue('dataScale') : undefined,
    })
  }

  const suggestion = suggestDomainName(classifierName, dataType, dataLength, dataScale)

  const showError = (error: unknown, fromRequest: boolean) => {
    if (applyCodeError(form, error, DOMAIN_ERROR_FIELDS)) {
      return
    }
    if (applyFieldErrors(form, error, fromRequest ? { stripPrefix: 'payload.' } : {})) {
      return
    }
    message.error(errorMessage(error))
  }

  const handleFinish = (values: DomainFormValues) => {
    const type = values.dataType as DataType
    const body: DomainRequest = {
      name: values.name?.trim() ?? '',
      classifierWordId: values.classifierWordId as number,
      dataType: type,
      dataLength: usesLength(type) ? values.dataLength : undefined,
      dataScale: usesScale(type) ? values.dataScale : undefined,
      description: values.description?.trim() || undefined,
    }

    if (!canSave) {
      requestDomain.mutate(
        {
          body: {
            requestType: isEdit ? 'UPDATE' : 'CREATE',
            targetId: isEdit ? domain?.id : undefined,
            payload: body,
            reason: values.reason?.trim() || undefined,
          },
        },
        {
          onSuccess: () => {
            message.success('신청했습니다. 관리자가 승인하면 반영됩니다.')
            invalidate('/api/requests')
            onClose()
          },
          onError: (error: unknown) => showError(error, true),
        },
      )
      return
    }

    const onSuccess = () => {
      message.success(isEdit ? '도메인을 수정했습니다.' : '도메인을 등록했습니다.')
      // 용어 목록도 도메인의 타입·길이를 보여주고, 컬럼의 비표준 검출도 도메인 기준이라 함께 새로 불러온다.
      invalidate('/api/domains', '/api/domains/{id}', '/api/histories', '/api/terms', '/api/tables/{tableId}/columns', '/api/columns/non-standard')
      onClose()
    }
    if (isEdit && domain?.id !== undefined) {
      updateDomain.mutate({ params: { path: { id: domain.id } }, body }, { onSuccess, onError: (error: unknown) => showError(error, false) })
    } else {
      createDomain.mutate({ body }, { onSuccess, onError: (error: unknown) => showError(error, false) })
    }
  }

  const action = isEdit ? '수정' : '등록'
  const lengthSpec = dataType ? DATA_TYPE_SPECS[dataType as DataType] : undefined

  return (
    <Modal
      open={open}
      title={canSave ? `도메인 ${action}` : `도메인 ${action} 신청`}
      okText={canSave ? '저장' : '신청'}
      confirmLoading={saving}
      onOk={() => form.submit()}
      onCancel={onClose}
      forceRender
      width={600}
    >
      {!canSave && (
        <Alert type="info" showIcon className="form-notice" title="변경 신청으로 접수되며, 관리자가 승인하면 반영됩니다." />
      )}
      {isEdit && (
        <Alert
          type="warning"
          showIcon
          className="form-notice"
          title="타입·길이를 바꾸면 이 도메인을 쓰는 용어와 컬럼의 비표준 검출 결과에 바로 반영됩니다. 용어에서 쓰는 도메인은 분류어를 바꿀 수 없습니다."
        />
      )}
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          name="classifierWordId"
          label="분류어"
          extra="도메인은 분류어 단어에만 연결됩니다."
          rules={[{ required: true, message: '분류어 단어를 선택하세요.' }]}
        >
          <ClassifierWordSelect onChange={(_, name) => setClassifierName(name)} />
        </Form.Item>

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
              rules={[
                {
                  required: usesLength(dataType as DataType | undefined),
                  message: '길이를 입력하세요.',
                },
              ]}
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

        <Form.Item
          name="name"
          label="도메인명"
          extra={
            <Button
              type="link"
              size="small"
              className="inline-link"
              disabled={!suggestion}
              onClick={() => suggestion && form.setFields([{ name: 'name', value: suggestion, errors: [] }])}
            >
              {suggestion ? `추천 이름 사용: ${suggestion}` : '분류어·타입·길이를 고르면 이름을 추천합니다'}
            </Button>
          }
          rules={[
            { required: true, whitespace: true, message: '도메인명을 입력하세요.' },
            { max: 100, message: '100자 이내로 입력하세요.' },
          ]}
        >
          <Input placeholder="예: 번호V20" maxLength={100} />
        </Form.Item>

        <Form.Item name="description" label="설명" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
          <Input.TextArea rows={2} maxLength={1000} showCount />
        </Form.Item>

        {!canSave && (
          <Form.Item name="reason" label="신청 사유" rules={[{ max: 1000, message: '1000자 이내로 입력하세요.' }]}>
            <Input.TextArea rows={2} maxLength={1000} showCount placeholder="관리자가 검토할 때 참고할 내용 (선택)" />
          </Form.Item>
        )}
      </Form>
    </Modal>
  )
}
