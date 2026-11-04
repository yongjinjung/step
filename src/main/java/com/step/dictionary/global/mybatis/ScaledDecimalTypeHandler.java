package com.step.dictionary.global.mybatis;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * BigDecimal 조회 시 컬럼 정의의 소수 자릿수를 유지한다.
 *
 * <p>Oracle은 {@code NUMBER(12,3)}에 저장한 30.000을 소수 자릿수 0인 {@code 30}으로 돌려준다. 그러면 저장 직후 응답(30.000)과
 * 조회 응답(30)의 모양이 달라진다. 결과 메타데이터의 소수 자릿수(scale)가 양수면 그 자릿수로 맞춘다.
 * {@code SUM()} 같은 계산 결과는 메타데이터에 자릿수가 없어 그대로 두므로 호출한 쪽에서 맞춘다(예: {@code Quantities.normalize}).
 *
 * <p>{@code mybatis.type-handlers-package}로 등록되어 모든 BigDecimal 결과에 기본으로 쓰인다.
 */
@MappedTypes(BigDecimal.class)
public class ScaledDecimalTypeHandler extends BaseTypeHandler<BigDecimal> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, BigDecimal parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setBigDecimal(i, parameter);
    }

    @Override
    public BigDecimal getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return scaled(rs.getBigDecimal(columnName), rs, rs.findColumn(columnName));
    }

    @Override
    public BigDecimal getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return scaled(rs.getBigDecimal(columnIndex), rs, columnIndex);
    }

    @Override
    public BigDecimal getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getBigDecimal(columnIndex);
    }

    private static BigDecimal scaled(BigDecimal value, ResultSet rs, int columnIndex) throws SQLException {
        if (value == null) {
            return null;
        }
        int scale = rs.getMetaData().getScale(columnIndex);
        return scale > 0 && value.scale() < scale ? value.setScale(scale) : value;
    }
}
