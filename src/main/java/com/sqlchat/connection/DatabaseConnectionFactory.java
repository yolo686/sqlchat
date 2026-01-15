package com.sqlchat.connection;

import com.sqlchat.model.DatabaseType;
import com.sqlchat.connection.impl.MySQLConnection;
import com.sqlchat.connection.impl.PostgreSQLConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库连接工厂
 * @author sqlChat
 */
@Component
public class DatabaseConnectionFactory {
    
    private final Map<DatabaseType, DatabaseConnection> connections = new HashMap<>();
    
    @Autowired
    public DatabaseConnectionFactory(MySQLConnection mysqlConnection, 
                                     PostgreSQLConnection postgresqlConnection) {
        connections.put(DatabaseType.MYSQL, mysqlConnection);
        connections.put(DatabaseType.POSTGRESQL, postgresqlConnection);
    }
    
    public DatabaseConnection getConnection(DatabaseType type) {
        DatabaseConnection connection = connections.get(type);
        if (connection == null) {
            throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
        return connection;
    }
}
