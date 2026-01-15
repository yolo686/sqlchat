package com.sqlchat.executor;

import com.sqlchat.model.DatabaseConfig;

import java.util.List;
import java.util.Map;

/**
 * SQL执行器接口
 * @author sqlChat
 */
public interface SqlExecutor {
    /**
     * 执行查询SQL
     */
    List<Map<String, Object>> executeQuery(DatabaseConfig config, String sql) throws Exception;

    /**
     * 执行更新SQL（INSERT、UPDATE、DELETE）
     */
    int executeUpdate(DatabaseConfig config, String sql) throws Exception;

    /**
     * 验证SQL语句是否安全（防止SQL注入）
     */
    boolean validateSql(String sql);
}
