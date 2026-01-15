package com.sqlchat.llm;

/**
 * SQL生成器接口
 * @author sqlChat
 */
public interface SqlGenerator {
    /**
     * 生成SQL语句
     */
    String generateSql(String prompt);
}
