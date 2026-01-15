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
 * MySQL SQL执行器实现
 * @author sqlChat
 */
@Component
public class MySQLSqlExecutor implements SqlExecutor {

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
        // 在NL2SQL场景下，通常只允许查询，不允许更新
        throw new UnsupportedOperationException("NL2SQL场景下不支持执行更新操作");
    }

    @Override
    public boolean validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String trimmedSql = sql.trim().toUpperCase();
        // 只允许SELECT语句
        if (!trimmedSql.startsWith("SELECT")) {
            return false;
        }
        
        // 检查是否包含危险操作
        if (DANGEROUS_PATTERN.matcher(sql).find()) {
            return false;
        }
        
        return true;
    }

    private Connection getConnection(DatabaseConfig config) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(
            config.getJdbcUrl(),
            config.getUsername(),
            config.getPassword()
        );
    }
}
