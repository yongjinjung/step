import { CheckOutlined, CloseOutlined, RollbackOutlined } from '@ant-design/icons'
import { Alert, App, Button, Descriptions, Drawer, Skeleton, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useAuth } from '@/auth/AuthContext'
import { useMenus } from '@/layouts/MenuContext'
import { ReasonModal } from '@/components/ReasonModal'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import { formatDateTime } from '@/utils/format'
import { RequestPayloadCompare } from './RequestPayloadCompare'
import {
  REQUEST_STATUSES,
  REQUEST_TARGET_TYPES,
  REQUEST_TYPES,
  summarizeTarget,
  TARGET_QUERY_PATHS,
  type ChangeRequestResponse,
} from './requestFields'

interface RequestDetailDrawerProps {
  /** 보여줄 신청 ID. null이면 닫힌다 */
  requestId: number | null
  onClose: () => void
}

/**
 * 신청 상세와 처리.
 *
 * - ADMIN: 대기 중인 신청을 승인(의견 선택)하거나 반려(의견 필수)한다. 승인하면 서버가 기존 서비스로 대상에 반영한다.
 * - 신청자 본인: 대기 중인 신청을 취소한다. 관리자라도 남의 신청은 취소할 수 없다(REQUEST_NOT_OWNED).
 * - 승인이 업무 규칙(중복 등)이나 대상 변경(REQUEST_TARGET_CHANGED)으로 실패하면 신청은 대기로 남으므로 사유를 창으로 보여준다.
 */
export function RequestDetailDrawer({ requestId, onClose }: RequestDetailDrawerProps) {
  const { user } = useAuth()
  const { can } = useMenus()
  // 신청 메뉴의 수정 권한이 곧 검토(승인·반려) 권한이다. 취소는 신청자 본인만 할 수 있어 아이디도 본다.
  const canReview = can('/requests', 'update')
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const open = requestId !== null

  const requestQuery = $api.useQuery('get', '/api/requests/{id}', { params: { path: { id: requestId ?? 0 } } }, { enabled: open })
  const request = requestQuery.data

  const approveRequest = $api.useMutation('post', '/api/requests/{id}/approve')
  const rejectRequest = $api.useMutation('post', '/api/requests/{id}/reject')
  const cancelRequest = $api.useMutation('post', '/api/requests/{id}/cancel')
  const [dialog, setDialog] = useState<'approve' | 'reject' | null>(null)

  const isPending = request?.status === 'REQUESTED'
  const isMine = request?.requesterUsername === user?.username

  /** 처리 후 신청 목록·단건과, 승인이면 대상 화면의 조회도 다시 불러온다. */
  const refresh = (target: ChangeRequestResponse, applied: boolean) => {
    invalidate('/api/requests', '/api/requests/{id}')
    if (applied && target.targetType) {
      invalidate('/api/histories', ...TARGET_QUERY_PATHS[target.targetType])
    }
  }

  const showFailure = (title: string, error: unknown) => {
    modal.error({ title, content: errorMessage(error) })
    // 실패해도 신청 상태나 대상이 바뀌었을 수 있으니 최신 값으로 다시 본다.
    invalidate('/api/requests', '/api/requests/{id}')
  }

  const submitApprove = (comment: string | undefined) => {
    if (!request?.id) {
      return
    }
    approveRequest.mutate(
      { params: { path: { id: request.id } }, body: { comment } },
      {
        onSuccess: () => {
          message.success('승인해 대상에 반영했습니다.')
          setDialog(null)
          refresh(request, true)
        },
        onError: (error: unknown) => {
          setDialog(null)
          showFailure('승인하지 못했습니다', error)
        },
      },
    )
  }

  const submitReject = (comment: string | undefined) => {
    if (!request?.id || !comment) {
      return
    }
    rejectRequest.mutate(
      { params: { path: { id: request.id } }, body: { comment } },
      {
        onSuccess: () => {
          message.success('반려했습니다.')
          setDialog(null)
          refresh(request, false)
        },
        onError: (error: unknown) => {
          setDialog(null)
          showFailure('반려하지 못했습니다', error)
        },
      },
    )
  }

  const confirmCancel = () => {
    if (!request?.id) {
      return
    }
    const id = request.id
    modal.confirm({
      title: '신청을 취소할까요?',
      content: '취소한 신청은 되돌릴 수 없습니다. 필요하면 다시 신청하세요.',
      okText: '신청 취소',
      okButtonProps: { danger: true },
      cancelText: '닫기',
      onOk: () =>
        cancelRequest
          .mutateAsync({ params: { path: { id } } })
          .then(() => {
            message.success('신청을 취소했습니다.')
            refresh(request, false)
          })
          .catch((error: unknown) => showFailure('취소하지 못했습니다', error)),
    })
  }

  const targetLabel = request?.targetType ? REQUEST_TARGET_TYPES[request.targetType].label : ''
  const typeLabel = request?.requestType ? REQUEST_TYPES[request.requestType].label : ''

  return (
    <Drawer
      open={open}
      onClose={onClose}
      size="large"
      title={request ? `신청 #${request.id} · ${targetLabel} ${typeLabel}` : '신청 상세'}
      extra={
        request &&
        isPending && (
          <Space>
            {isMine && (
              <Button icon={<RollbackOutlined />} onClick={confirmCancel}>
                신청 취소
              </Button>
            )}
            {canReview && (
              <>
                <Button danger icon={<CloseOutlined />} onClick={() => setDialog('reject')}>
                  반려
                </Button>
                <Button type="primary" icon={<CheckOutlined />} onClick={() => setDialog('approve')}>
                  승인
                </Button>
              </>
            )}
          </Space>
        )
      }
    >
      {requestQuery.isPending && open ? (
        <Skeleton active />
      ) : requestQuery.isError ? (
        <Alert type="error" showIcon title={errorMessage(requestQuery.error)} />
      ) : (
        request && (
          <>
            {canReview && isPending && request.requestType !== 'CREATE' && (
              <Alert
                type="info"
                showIcon
                className="form-notice"
                title="신청 이후 대상이 수정·삭제됐으면 승인할 수 없습니다. 이때는 반려하고 최신 값으로 다시 신청하게 하세요."
              />
            )}

            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="상태">
                {request.status && <Tag color={REQUEST_STATUSES[request.status].color}>{REQUEST_STATUSES[request.status].label}</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="대상">
                <Space size={4} wrap>
                  {request.targetType && <Tag color={REQUEST_TARGET_TYPES[request.targetType].color}>{targetLabel}</Tag>}
                  {request.requestType && <Tag color={REQUEST_TYPES[request.requestType].color}>{typeLabel}</Tag>}
                  {summarizeTarget(request)}
                  {request.requestType === 'CREATE' && isPending && (
                    <Typography.Text type="secondary">(승인하면 등록됩니다)</Typography.Text>
                  )}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="신청자">
                {request.requesterName} <Typography.Text type="secondary">({request.requesterUsername})</Typography.Text>
              </Descriptions.Item>
              <Descriptions.Item label="신청 일시">{formatDateTime(request.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="신청 사유">{request.reason || '-'}</Descriptions.Item>
              {!isPending && (
                <>
                  <Descriptions.Item label={request.status === 'CANCELED' ? '취소 일시' : '처리 일시'}>
                    {formatDateTime(request.reviewedAt)}
                  </Descriptions.Item>
                  {request.reviewerUsername && (
                    <Descriptions.Item label="처리자">
                      {request.reviewerName} <Typography.Text type="secondary">({request.reviewerUsername})</Typography.Text>
                    </Descriptions.Item>
                  )}
                  {request.status !== 'CANCELED' && (
                    <Descriptions.Item label={request.status === 'REJECTED' ? '반려 의견' : '승인 의견'}>
                      {request.reviewComment || '-'}
                    </Descriptions.Item>
                  )}
                </>
              )}
            </Descriptions>

            <Typography.Title level={5} className="section-title">
              신청 내용
            </Typography.Title>
            <RequestPayloadCompare request={request} />

            {request.status === 'APPROVED' && (
              <div className="section-link">
                <Link to={`/histories?requestId=${request.id}`}>이 신청으로 반영된 변경 이력 보기</Link>
              </div>
            )}
          </>
        )
      )}

      <ReasonModal
        open={dialog === 'approve'}
        title="신청 승인"
        description={<Typography.Paragraph>승인하면 신청 내용이 바로 대상에 반영됩니다.</Typography.Paragraph>}
        label="승인 의견"
        placeholder="신청자에게 남길 내용 (선택)"
        okText="승인"
        confirmLoading={approveRequest.isPending}
        onSubmit={submitApprove}
        onCancel={() => setDialog(null)}
      />
      <ReasonModal
        open={dialog === 'reject'}
        title="신청 반려"
        description={<Typography.Paragraph>신청자가 무엇을 고쳐 다시 신청할지 알 수 있게 의견을 남기세요.</Typography.Paragraph>}
        label="반려 의견"
        placeholder="예: 같은 이름의 단어가 이미 있습니다."
        required
        danger
        okText="반려"
        confirmLoading={rejectRequest.isPending}
        onSubmit={submitReject}
        onCancel={() => setDialog(null)}
      />
    </Drawer>
  )
}
