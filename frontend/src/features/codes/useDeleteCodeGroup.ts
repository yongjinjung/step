import { App } from 'antd'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import type { CodeGroupResponse } from './codeFields'

interface DeleteCodeGroupOptions {
  /** 알고 있으면 확인 창에 함께 삭제될 코드값 수를 보여준다 */
  valueCount?: number
  /** 삭제에 성공한 뒤 호출한다 (상세 화면이면 목록으로 이동) */
  onDeleted?: () => void
}

/**
 * 코드 그룹 삭제 확인 창. 목록과 상세 화면이 같이 쓴다.
 * 서버가 코드값도 함께 지우고, 컬럼에 매핑된 그룹이면 CODE_GROUP_IN_USE로 거부하므로 확인 창에서 알려준다.
 */
export function useDeleteCodeGroup() {
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const deleteGroup = $api.useMutation('delete', '/api/code-groups/{id}')

  return (group: CodeGroupResponse, { valueCount, onDeleted }: DeleteCodeGroupOptions = {}) => {
    if (group.id === undefined) {
      return
    }
    const id = group.id
    modal.confirm({
      title: '코드 그룹을 삭제할까요?',
      content: `${group.codeId} (${group.codeName}) — 코드값${valueCount === undefined ? '' : ` ${valueCount}개`}도 함께 삭제됩니다. 컬럼에 매핑돼 있으면 삭제할 수 없습니다.`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteGroup
          .mutateAsync({ params: { path: { id } } })
          .then(() => {
            message.success('코드 그룹을 삭제했습니다.')
            onDeleted?.()
            // 삭제된 그룹의 단건 조회는 무효화하지 않는다. 상세 화면에 머무는 동안 404로 다시 불러오게 된다.
            invalidate('/api/code-groups', '/api/histories')
          })
          .catch((error: unknown) => {
            message.error(errorMessage(error))
          }),
    })
  }
}
