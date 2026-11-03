package com.step.dictionary.global.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * boolean ↔ 'Y'/'N' 문자 변환. 이 프로젝트의 여부 컬럼은 모두 {@code CHAR(1)} Y/N이다.
 *
 * <p>{@code mybatis.type-handlers-package}로 등록되어 모든 boolean 파라미터와 결과에 기본으로 쓰인다.
 * 그래서 SQL 결과를 boolean으로 바로 받지 않는다(존재 여부는 {@code COUNT(*)}를 long으로 받아 비교한다).
 */
@MappedTypes({Boolean.class, boolean.class})
@MappedJdbcTypes(value = JdbcType.CHAR, includeNullJdbcType = true)
public class YesNoTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter ? "Y" : "N");
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toBoolean(rs.getString(columnName));
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toBoolean(rs.getString(columnIndex));
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toBoolean(cs.getString(columnIndex));
    }

    private static Boolean toBoolean(String value) {
        return value == null ? null : "Y".equals(value);
    }
}
