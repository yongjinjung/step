import { WordSelect } from './WordSelect'

interface ClassifierWordSelectProps {
  /** 선택한 분류어 단어 ID */
  value?: number
  /**
   * 선택이 바뀌면 호출한다. 두 번째 인자로 단어명을 넘겨 도메인명 추천 등에 쓸 수 있게 한다.
   * Form.Item 안에서 쓰면 Form이 첫 번째 인자(ID)를 값으로 쓴다.
   */
  onChange?: (value: number | undefined, wordName: string | undefined) => void
  placeholder?: string
  allowClear?: boolean
  className?: string
}

/**
 * 분류어 단어 검색 선택. 분류어만 서버에서 검색하는 {@link WordSelect}다.
 * 선택된 값이 검색 결과에 없을 때 단건 조회로 이름을 채우는 동작도 WordSelect와 같다.
 */
export function ClassifierWordSelect({ onChange, ...props }: ClassifierWordSelectProps) {
  return <WordSelect {...props} classifierOnly onChange={(id, word) => onChange?.(id, word?.name)} />
}
