package com.sqlchat.executor;

import com.sqlchat.model.DatabaseType;
import com.sqlchat.executor.impl.MySQLSqlExecutor;
import com.sqlchat.executor.impl.PostgreSQLSqlExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * SQL执行器工厂
 * @author sqlChat
 */
@Component
public class SqlExecutorFactory {
    
    private final Map<DatabaseType, SqlExecutor> executors = new HashMap<>();
    
    @Autowired
    public SqlExecutorFactory(MySQLSqlExecutor mysqlExecutor, 
                              PostgreSQLSqlExecutor postgresqlExecutor) {
        executors.put(DatabaseType.MYSQL, mysqlExecutor);
        executors.put(DatabaseType.POSTGRESQL, postgresqlExecutor);
    }
    
    public SqlExecutor getExecutor(DatabaseType type) {
        SqlExecutor executor = executors.get(type);
        if (executor == null) {
            throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
        return executor;
    }
}
