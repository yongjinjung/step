import { App } from 'antd'
import { $api } from '@/api/client'
import { errorMessage } from '@/api/errors'
import { useInvalidateApi } from '@/hooks/useInvalidateApi'
import type { MetaTableResponse } from './tableFields'

interface DeleteTableOptions {
  /** 알고 있으면 확인 창에 함께 삭제될 컬럼 수를 보여준다 */
  columnCount?: number
  /** 삭제에 성공한 뒤 호출한다 (상세 화면이면 목록으로 이동) */
  onDeleted?: () => void
}

/**
 * 테이블 삭제 확인 창. 목록과 상세 화면이 같이 쓴다.
 * 테이블을 지우면 서버가 컬럼 정의도 함께 지우므로 확인 창에서 알려준다.
 */
export function useDeleteTable() {
  const { message, modal } = App.useApp()
  const invalidate = useInvalidateApi()
  const deleteTable = $api.useMutation('delete', '/api/tables/{id}')

  return (table: MetaTableResponse, { columnCount, onDeleted }: DeleteTableOptions = {}) => {
    if (table.id === undefined) {
      return
    }
    const id = table.id
    modal.confirm({
      title: '테이블을 삭제할까요?',
      content: `${table.physicalName} — 컬럼 정의${columnCount === undefined ? '' : ` ${columnCount}개`}도 함께 삭제됩니다.`,
      okText: '삭제',
      okButtonProps: { danger: true },
      cancelText: '취소',
      onOk: () =>
        deleteTable
          .mutateAsync({ params: { path: { id } } })
          .then(() => {
            message.success('테이블을 삭제했습니다.')
            onDeleted?.()
            // 삭제된 테이블의 단건 조회는 무효화하지 않는다. 상세 화면에 머무는 동안 404로 다시 불러오게 된다.
            invalidate('/api/tables', '/api/histories', '/api/columns/non-standard')
          })
          .catch((error: unknown) => {
            message.error(errorMessage(error))
          }),
    })
  }
}
