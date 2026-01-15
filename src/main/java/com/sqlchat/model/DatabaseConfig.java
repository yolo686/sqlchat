package com.sqlchat.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据库配置信息
 * @author sqlChat
 */
@Data
@NoArgsConstructor
public class DatabaseConfig {
    private String id;
    private DatabaseType type;
    private String host;
    private Integer port;
    private String database;
    private String username;
    private String password;
    private String name; // 数据源名称
    
    @JsonCreator
    public DatabaseConfig(@JsonProperty("id") String id,
                        @JsonProperty("type") String typeStr,
                        @JsonProperty("host") String host,
                        @JsonProperty("port") Integer port,
                        @JsonProperty("database") String database,
                        @JsonProperty("username") String username,
                        @JsonProperty("password") String password,
                        @JsonProperty("name") String name) {
        this.id = id;
        this.type = DatabaseType.fromString(typeStr);
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.name = name;
    }
    
    public DatabaseConfig(String id, DatabaseType type, String host, Integer port, 
                         String database, String username, String password, String name) {
        this.id = id;
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.name = name;
    }

    public String getJdbcUrl() {
        switch (type) {
            case MYSQL:
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=utf8", 
                    host, port, database);
            case POSTGRESQL:
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case ORACLE:
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case SQLSERVER:
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database);
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
    }
}
