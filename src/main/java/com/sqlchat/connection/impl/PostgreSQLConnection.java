package com.sqlchat.connection.impl;

import com.sqlchat.connection.DatabaseConnection;
import com.sqlchat.model.ColumnInfo;
import com.sqlchat.model.DatabaseConfig;
import com.sqlchat.model.TableInfo;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL数据库连接实现
 * @author sqlChat
 */
@Component
public class PostgreSQLConnection implements DatabaseConnection {

    @Override
    public Connection getConnection(DatabaseConfig config) throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(
            config.getJdbcUrl(),
            config.getUsername(),
            config.getPassword()
        );
    }

    @Override
    public boolean testConnection(DatabaseConfig config) {
        try (Connection conn = getConnection(config)) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> getTableNames(DatabaseConfig config) throws Exception {
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = getConnection(config)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, "public", null, new String[]{"TABLE"});
            while (rs.next()) {
                tableNames.add(rs.getString("TABLE_NAME"));
            }
        }
        return tableNames;
    }

    @Override
    public TableInfo getTableInfo(DatabaseConfig config, String tableName) throws Exception {
        try (Connection conn = getConnection(config)) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 获取表注释
            String tableComment = "";
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT obj_description(oid) as comment FROM pg_class WHERE relname = ?")) {
                ps.setString(1, tableName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tableComment = rs.getString("comment");
                }
            }
            
            // 获取列信息
            List<ColumnInfo> columns = new ArrayList<>();
            ResultSet columnsRs = metaData.getColumns(null, "public", tableName, null);
            
            // 获取主键
            ResultSet pkRs = metaData.getPrimaryKeys(null, "public", tableName);
            List<String> primaryKeys = new ArrayList<>();
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
            
            while (columnsRs.next()) {
                String columnName = columnsRs.getString("COLUMN_NAME");
                String dataType = columnsRs.getString("TYPE_NAME");
                int columnSize = columnsRs.getInt("COLUMN_SIZE");
                int nullable = columnsRs.getInt("NULLABLE");
                String columnComment = columnsRs.getString("REMARKS");
                
                ColumnInfo columnInfo = new ColumnInfo();
                columnInfo.setColumnName(columnName);
                columnInfo.setDataType(dataType);
                columnInfo.setColumnSize(columnSize);
                columnInfo.setNullable(nullable == DatabaseMetaData.columnNullable);
                columnInfo.setColumnComment(columnComment != null ? columnComment : "");
                columnInfo.setIsPrimaryKey(primaryKeys.contains(columnName));
                
                columns.add(columnInfo);
            }
            
            TableInfo tableInfo = new TableInfo();
            tableInfo.setTableName(tableName);
            tableInfo.setTableComment(tableComment);
            tableInfo.setColumns(columns);
            
            return tableInfo;
        }
    }

    @Override
    public List<TableInfo> getAllTableInfo(DatabaseConfig config) throws Exception {
        List<String> tableNames = getTableNames(config);
        List<TableInfo> tableInfos = new ArrayList<>();
        for (String tableName : tableNames) {
            tableInfos.add(getTableInfo(config, tableName));
        }
        return tableInfos;
    }
}
