package com.sqlchat.llm;

/**
 * SQL生成器接口
 * @author sqlChat
 */
public interface SqlGenerator {
    /**
     * 生成SQL语句（使用默认temperature）
     */
    String generateSql(String prompt);

    /**
     * 生成SQL语句（指定temperature）
     * @param prompt 提示词
     * @param temperature 温度参数，0.0表示确定性输出，越高越随机
     */
    String generateSql(String prompt, double temperature);
}
