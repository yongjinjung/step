import { DownloadOutlined, FileExcelOutlined, UploadOutlined } from '@ant-design/icons'
import { App, Button, Space, Table, Upload } from 'antd'
import { useState } from 'react'
import { apiClient } from '@/api/client'
import { downloadFile } from '@/api/download'
import { errorMessage, isApiError } from '@/api/errors'
import type { components } from '@/api/schema'

export type ExcelTarget = 'words' | 'domains' | 'terms' | 'tables'
type ExcelUploadResponse = components['schemas']['ExcelUploadResponse']

interface ExcelActionsProps {
  target: ExcelTarget
  /** 현재 검색 조건. 다운로드에 그대로 넘겨 화면과 같은 목록을 받는다 */
  downloadParams?: Record<string, string | number | boolean | undefined>
  /** 업로드가 성공한 뒤 호출한다 (목록 다시 불러오기 등) */
  onUploaded?: (result: ExcelUploadResponse) => void
}

/**
 * 엑셀 다운로드 · 업로드 양식 · 업로드 버튼 묶음. 단어/도메인/용어/테이블 화면에서 같이 쓴다.
 *
 * 업로드는 서버가 전부 반영하거나 전혀 반영하지 않는다. 오류 행이 있으면 행 번호별 사유를 표로 보여준다.
 * 표준 데이터를 USER가 올리면 서버가 행마다 등록 신청을 만들고 결과의 mode가 REQUESTED다.
 */
export function ExcelActions({ target, downloadParams, onUploaded }: ExcelActionsProps) {
  const { message, modal } = App.useApp()
  const [downloading, setDownloading] = useState(false)
  const [uploading, setUploading] = useState(false)

  const download = async (template: boolean) => {
    setDownloading(true)
    try {
      await (template
        ? downloadFile(`/api/excel/${target}/template`)
        : downloadFile(`/api/excel/${target}`, downloadParams))
    } catch (error) {
      message.error(errorMessage(error))
    } finally {
      setDownloading(false)
    }
  }

  const showUploadError = (error: unknown) => {
    if (isApiError(error) && error.fieldErrors?.length) {
      modal.error({
        title: error.message,
        width: 680,
        content: (
          <Table
            size="small"
            pagination={false}
            scroll={{ y: 320 }}
            dataSource={error.fieldErrors.map((detail, index) => ({ ...detail, key: index }))}
            columns={[
              { title: '위치', dataIndex: 'field', width: 200 },
              { title: '사유', dataIndex: 'reason' },
            ]}
          />
        ),
      })
      return
    }
    message.error(errorMessage(error))
  }

  const upload = async (file: File) => {
    setUploading(true)
    try {
      // 네 업로드 API의 요청·응답 타입이 같으므로 경로 하나의 타입으로 호출한다.
      // 생성 타입은 파일을 string(format: binary)으로 표현해 File을 캐스팅하고, 본문은 FormData로 직접 만든다.
      const { data, error, response } = await apiClient.POST(`/api/excel/${target}` as '/api/excel/words', {
        body: { file: file as unknown as string },
        bodySerializer: () => {
          const formData = new FormData()
          formData.append('file', file)
          return formData
        },
      })
      if (!response.ok || !data) {
        showUploadError(error as unknown)
        return
      }
      modal.success({
        title: data.mode === 'REQUESTED' ? '등록 신청을 만들었습니다' : '업로드했습니다',
        content:
          data.mode === 'REQUESTED'
            ? `${data.rowCount}행을 등록 신청했습니다. 관리자가 승인하면 반영됩니다.`
            : `${data.rowCount}행을 반영했습니다.`,
      })
      onUploaded?.(data)
    } catch (error) {
      showUploadError(error)
    } finally {
      setUploading(false)
    }
  }

  return (
    <Space wrap>
      <Button icon={<DownloadOutlined />} loading={downloading} onClick={() => void download(false)}>
        엑셀 다운로드
      </Button>
      <Button icon={<FileExcelOutlined />} onClick={() => void download(true)}>
        업로드 양식
      </Button>
      <Upload
        accept=".xlsx"
        showUploadList={false}
        beforeUpload={(file) => {
          void upload(file)
          // false를 돌려 Upload 컴포넌트의 자동 전송을 막고 직접 올린다.
          return false
        }}
      >
        <Button icon={<UploadOutlined />} loading={uploading}>
          엑셀 업로드
        </Button>
      </Upload>
    </Space>
  )
}
