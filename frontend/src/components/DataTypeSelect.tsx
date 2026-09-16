import { Select, Typography } from 'antd'
import { DATA_TYPE_OPTIONS, type DataType } from '@/utils/dataType'

interface DataTypeSelectProps {
  value?: DataType
  onChange?: (value: DataType | undefined) => void
  placeholder?: string
  allowClear?: boolean
  className?: string
}

/** 데이터 타입 선택. 목록에는 타입별 길이 규칙을 함께 보여준다. 도메인과 컬럼 정의 화면에서 쓴다. */
export function DataTypeSelect({ value, onChange, placeholder = '데이터 타입', allowClear, className }: DataTypeSelectProps) {
  return (
    <Select<DataType>
      value={value}
      onChange={(next) => onChange?.(next)}
      placeholder={placeholder}
      allowClear={allowClear}
      className={className}
      options={DATA_TYPE_OPTIONS}
      optionRender={(option) => (
        <span>
          <span className="mono">{option.value}</span>{' '}
          <Typography.Text type="secondary" className="option-hint">
            {option.data.description}
          </Typography.Text>
        </span>
      )}
    />
  )
}
