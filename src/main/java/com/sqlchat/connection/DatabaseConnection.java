package com.sqlchat.connection;

import com.sqlchat.model.DatabaseConfig;
import com.sqlchat.model.TableInfo;

import java.sql.Connection;
import java.util.List;

/**
 * 数据库连接接口
 * @author sqlChat
 */
public interface DatabaseConnection {
    /**
     * 获取数据库连接
     */
    Connection getConnection(DatabaseConfig config) throws Exception;

    /**
     * 测试连接
     */
    boolean testConnection(DatabaseConfig config);

    /**
     * 获取所有表名
     */
    List<String> getTableNames(DatabaseConfig config) throws Exception;

    /**
     * 获取表结构信息
     */
    TableInfo getTableInfo(DatabaseConfig config, String tableName) throws Exception;

    /**
     * 获取所有表的结构信息
     */
    List<TableInfo> getAllTableInfo(DatabaseConfig config) throws Exception;
}
