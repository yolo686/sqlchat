package com.sqlchat.executor.impl;

import com.sqlchat.executor.SqlExecutor;
import com.sqlchat.model.DatabaseConfig;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PostgreSQL SQL执行器实现
 * @author sqlChat
 */
@Component
public class PostgreSQLSqlExecutor implements SqlExecutor {

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        "(?i)(DROP|DELETE|TRUNCATE|ALTER|CREATE|GRANT|REVOKE|INSERT|UPDATE).*",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<Map<String, Object>> executeQuery(DatabaseConfig config, String sql) throws Exception {
        if (!validateSql(sql)) {
            throw new IllegalArgumentException("SQL语句不安全，仅支持SELECT查询");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = getConnection(config);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        }
        return results;
    }

    @Override
    public int executeUpdate(DatabaseConfig config, String sql) throws Exception {
        throw new UnsupportedOperationException("NL2SQL场景下不支持执行更新操作");
    }

    @Override
    public boolean validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String trimmedSql = sql.trim().toUpperCase();
        if (!trimmedSql.startsWith("SELECT")) {
            return false;
        }
        
        if (DANGEROUS_PATTERN.matcher(sql).find()) {
            return false;
        }
        
        return true;
    }

    private Connection getConnection(DatabaseConfig config) throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(
            config.getJdbcUrl(),
            config.getUsername(),
            config.getPassword()
        );
    }
}
