package com.sqlchat.model;

/**
 * 数据库类型枚举
 * @author sqlChat
 */
public enum DatabaseType {
    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    ORACLE("Oracle"),
    SQLSERVER("SQL Server");

    private final String name;

    DatabaseType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DatabaseType fromString(String type) {
        if (type == null) {
            return MYSQL;
        }
        for (DatabaseType dbType : DatabaseType.values()) {
            if (dbType.name().equalsIgnoreCase(type) || dbType.getName().equalsIgnoreCase(type)) {
                return dbType;
            }
        }
        return MYSQL;
    }
}
