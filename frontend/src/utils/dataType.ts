import type { components } from '@/api/schema'

/** 데이터 타입. 도메인과 컬럼 정의가 같은 값을 쓴다 (백엔드 DataType enum). */
export type DataType = components['schemas']['DomainRequest']['dataType']

interface DataTypeSpec {
  /** 최대 길이. 0이면 길이를 쓰지 않는 타입 */
  maxLength: number
  /** 소수점 자리수를 쓸 수 있는지 (DECIMAL만) */
  scalable: boolean
  /** 선택 목록에 보여줄 설명 */
  description: string
}

/** 소수점 자리수 최대값 (MySQL DECIMAL). */
export const MAX_DECIMAL_SCALE = 30

/**
 * 타입별 길이·소수점 규칙. 백엔드 `DataType.isValidSpec`과 같아야 한다.
 * 화면에서 먼저 막아 주고, 최종 검증은 서버가 한다(INVALID_DOMAIN_SPEC / INVALID_COLUMN_SPEC).
 */
export const DATA_TYPE_SPECS: Record<DataType, DataTypeSpec> = {
  CHAR: { maxLength: 255, scalable: false, description: '고정 길이 문자 (길이 1~255)' },
  VARCHAR: { maxLength: 65535, scalable: false, description: '가변 길이 문자 (길이 1~65535)' },
  DECIMAL: { maxLength: 65, scalable: true, description: '고정 소수점 숫자 (길이 1~65, 소수점 0~30)' },
  INTEGER: { maxLength: 0, scalable: false, description: '32비트 정수' },
  BIGINT: { maxLength: 0, scalable: false, description: '64비트 정수' },
  DATE: { maxLength: 0, scalable: false, description: '날짜' },
  DATETIME: { maxLength: 0, scalable: false, description: '날짜와 시각' },
  TEXT: { maxLength: 0, scalable: false, description: '긴 문자열' },
}

export const DATA_TYPES = Object.keys(DATA_TYPE_SPECS) as DataType[]

/** Select 옵션. 라벨 옆에 규칙 설명을 붙인다. */
export const DATA_TYPE_OPTIONS = DATA_TYPES.map((type) => ({
  value: type,
  label: type,
  description: DATA_TYPE_SPECS[type].description,
}))

/** 길이를 쓰는 타입인지 (CHAR, VARCHAR, DECIMAL). */
export function usesLength(type: DataType | undefined): boolean {
  return type !== undefined && DATA_TYPE_SPECS[type].maxLength > 0
}

/** 소수점을 쓰는 타입인지 (DECIMAL). */
export function usesScale(type: DataType | undefined): boolean {
  return type !== undefined && DATA_TYPE_SPECS[type].scalable
}

/**
 * 저장되는 소수점 값. DECIMAL은 비우면 0으로 저장한다 (백엔드 `DataType.normalizeScale`).
 * DECIMAL(15)와 DECIMAL(15,0)을 같다고 비교할 때 쓴다.
 */
export function normalizeScale(type: DataType | undefined, scale?: number | null): number | null {
  if (usesScale(type)) {
    return scale ?? 0
  }
  return scale ?? null
}

/**
 * 타입 표기. 예: `VARCHAR(20)`, `DECIMAL(18,2)`, `DECIMAL(15)`, `DATE`.
 * DECIMAL의 소수점 0은 MySQL `DECIMAL(15)`와 같으므로 생략한다.
 */
export function formatDataType(type?: DataType, length?: number | null, scale?: number | null): string {
  if (!type) {
    return '-'
  }
  if (!usesLength(type) || length === undefined || length === null) {
    return type
  }
  return scale ? `${type}(${length},${scale})` : `${type}(${length})`
}

const NAME_SUFFIXES: Record<DataType, string> = {
  CHAR: 'C',
  VARCHAR: 'V',
  DECIMAL: 'N',
  INTEGER: 'I',
  BIGINT: 'BI',
  DATE: 'DT',
  DATETIME: 'DTM',
  TEXT: 'TX',
}

/**
 * 도메인명 추천: 분류어 단어명 + 타입 약자 + 길이(.소수점). 예: 번호V20, 금액N18.2, 일자DT.
 * 등록된 샘플 도메인과 같은 규칙이다. 필요한 값이 없으면 undefined.
 */
export function suggestDomainName(
  classifierWordName: string | undefined,
  type: DataType | undefined,
  length?: number | null,
  scale?: number | null,
): string | undefined {
  if (!classifierWordName || !type) {
    return undefined
  }
  if (!usesLength(type)) {
    return `${classifierWordName}${NAME_SUFFIXES[type]}`
  }
  if (!length) {
    return undefined
  }
  return `${classifierWordName}${NAME_SUFFIXES[type]}${length}${scale ? `.${scale}` : ''}`
}
