import { keepPreviousData } from '@tanstack/react-query'
import { Select } from 'antd'
import { useState } from 'react'
import { $api } from '@/api/client'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { formatDateTime } from '@/utils/format'
import type { DispensingStatus } from './pharmacyFields'

/**
 * 약제 업무 등록 모달에서 "앞 단계 기록"을 고르는 서버 검색 선택 박스.
 *
 * - 조제 등록 → 처방 선택 (PrescriptionSelect, 조제 전 처방만)
 * - 검수 등록 → 조제 선택 (DispensingSelect status=DISPENSED)
 * - 불출 등록 → 조제 선택 (DispensingSelect status=INSPECTED)
 * - 반납 등록 → 불출 선택 (IssueSelect)
 *
 * 검색어는 처방전번호·환자번호·환자명 앞부분 일치다(서버 규칙). 입력이 300ms 멈추면 검색한다.
 * 선택된 값이 검색 결과에 없으면(드로어에서 대상을 정해 열었을 때) 단건 조회로 라벨을 채운다.
 * Ant Design Form.Item 안에 두면 value/onChange가 자동으로 연결된다.
 */

interface SourceSelectProps {
    value?: number
    onChange?: (value: number | undefined) => void
    disabled?: boolean
}

interface Option {
    value: number
    label: string
}

/** 검색 결과 목록 앞에 (목록에 없는) 선택된 항목을 붙여 선택 박스 항목을 만든다. */
function mergeOptions(listed: Option[], selected: Option | undefined): Option[] {
    return selected && !listed.some((option) => option.value === selected.value) ? [selected, ...listed] : listed
}

/** 세 선택 박스가 같이 쓰는 모양. 검색어 상태와 디바운스는 각 선택 박스가 들고 있다. */
function SearchSelect({
    value,
    onChange,
    disabled,
    options,
    loading,
    placeholder,
    onSearch,
}: SourceSelectProps & { options: Option[]; loading: boolean; placeholder: string; onSearch: (text: string) => void }) {
    return (
        <Select
            value={value}
            placeholder={placeholder}
            disabled={disabled}
            options={options}
            loading={loading}
            // 서버가 검색하므로 화면에서 다시 거르지 않는다.
            showSearch={{ filterOption: false, onSearch }}
            notFoundContent={loading ? '검색 중…' : '대상이 없습니다'}
            onChange={(next: number | undefined) => {
                onSearch('')
                onChange?.(next)
            }}
        />
    )
}

/** 조제할 처방 선택. 조제 전(ORDERED) 처방만 검색한다. */
export function PrescriptionSelect({ value, onChange, disabled }: SourceSelectProps) {
    const [search, setSearch] = useState('')
    const keyword = useDebouncedValue(search.trim(), 300)

    const listQuery = $api.useQuery(
        'get',
        '/api/prescriptions',
        { params: { query: { keyword: keyword || undefined, status: 'ORDERED', size: 20 } } },
        { placeholderData: keepPreviousData, enabled: !disabled },
    )
    const selectedQuery = $api.useQuery(
        'get',
        '/api/prescriptions/{id}',
        { params: { path: { id: value ?? 0 } } },
        { enabled: value !== undefined },
    )

    const toOption = (p: { id?: number; prescriptionNo?: string; patientName?: string; patientNo?: string }): Option => ({
        value: p.id as number,
        label: `${p.prescriptionNo} · ${p.patientName} (${p.patientNo})`,
    })
    const listed = (listQuery.data?.content ?? []).filter((p) => p.id !== undefined).map(toOption)
    const selected = selectedQuery.data?.id !== undefined ? toOption(selectedQuery.data) : undefined

    return (
        <SearchSelect
            value={value}
            onChange={onChange}
            disabled={disabled}
            options={mergeOptions(listed, selected)}
            loading={listQuery.isFetching}
            placeholder="처방전번호 · 환자번호 · 환자명 (앞부분)"
            onSearch={setSearch}
        />
    )
}

/** 조제 선택. status로 검수 대기(DISPENSED)·검수 합격(INSPECTED) 조제만 고른다. */
export function DispensingSelect({ value, onChange, disabled, status }: SourceSelectProps & { status: DispensingStatus }) {
    const [search, setSearch] = useState('')
    const keyword = useDebouncedValue(search.trim(), 300)

    const listQuery = $api.useQuery(
        'get',
        '/api/dispensings',
        { params: { query: { keyword: keyword || undefined, status, size: 20 } } },
        { placeholderData: keepPreviousData, enabled: !disabled },
    )
    const selectedQuery = $api.useQuery(
        'get',
        '/api/dispensings/{id}',
        { params: { path: { id: value ?? 0 } } },
        { enabled: value !== undefined },
    )

    const toOption = (d: { id?: number; prescriptionNo?: string; patientName?: string; dispensedAt?: string }): Option => ({
        value: d.id as number,
        label: `${d.prescriptionNo} · ${d.patientName} (조제 ${formatDateTime(d.dispensedAt).slice(0, 16)})`,
    })
    const listed = (listQuery.data?.content ?? []).filter((d) => d.id !== undefined).map(toOption)
    const selected = selectedQuery.data?.id !== undefined ? toOption(selectedQuery.data) : undefined

    return (
        <SearchSelect
            value={value}
            onChange={onChange}
            disabled={disabled}
            options={mergeOptions(listed, selected)}
            loading={listQuery.isFetching}
            placeholder="처방전번호 · 환자번호 · 환자명 (앞부분)"
            onSearch={setSearch}
        />
    )
}

/** 반납할 불출 선택. */
export function IssueSelect({ value, onChange, disabled }: SourceSelectProps) {
    const [search, setSearch] = useState('')
    const keyword = useDebouncedValue(search.trim(), 300)

    const listQuery = $api.useQuery(
        'get',
        '/api/issues',
        { params: { query: { keyword: keyword || undefined, size: 20 } } },
        { placeholderData: keepPreviousData, enabled: !disabled },
    )
    const selectedQuery = $api.useQuery(
        'get',
        '/api/issues/{id}',
        { params: { path: { id: value ?? 0 } } },
        { enabled: value !== undefined },
    )

    const toOption = (s: { id?: number; prescriptionNo?: string; patientName?: string; destinationName?: string }): Option => ({
        value: s.id as number,
        label: `${s.prescriptionNo} · ${s.patientName} → ${s.destinationName}`,
    })
    const listed = (listQuery.data?.content ?? []).filter((s) => s.id !== undefined).map(toOption)
    const selected = selectedQuery.data?.id !== undefined ? toOption(selectedQuery.data) : undefined

    return (
        <SearchSelect
            value={value}
            onChange={onChange}
            disabled={disabled}
            options={mergeOptions(listed, selected)}
            loading={listQuery.isFetching}
            placeholder="처방전번호 · 환자번호 · 환자명 (앞부분)"
            onSearch={setSearch}
        />
    )
}
