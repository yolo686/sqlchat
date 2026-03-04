package com.sqlchat.voting;

import org.springframework.stereotype.Component;

/**
 * SQL规范化工具，用于对SQL文本进行标准化处理以便比较
 * @author sqlChat
 */
@Component
public class SqlNormalizer {

    /**
     * 规范化SQL语句，去除格式差异
     */
    public String normalize(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        String normalized = sql.trim();

        // 统一小写
        normalized = normalized.toLowerCase();

        // 去掉末尾分号
        normalized = normalized.replaceAll(";\\s*$", "");

        // 压缩所有空白字符（换行、制表符、多空格）为单个空格
        normalized = normalized.replaceAll("\\s+", " ");

        // 统一逗号格式：逗号后一个空格
        normalized = normalized.replaceAll("\\s*,\\s*", ", ");

        // 统一括号格式：括号内无多余空格
        normalized = normalized.replaceAll("\\(\\s+", "(");
        normalized = normalized.replaceAll("\\s+\\)", ")");

        // 去掉反引号和双引号
        normalized = normalized.replaceAll("[`\"]", "");

        // 统一运算符周围的空格
        normalized = normalized.replaceAll("\\s*=\\s*", " = ");
        normalized = normalized.replaceAll("\\s*<>\\s*", " <> ");
        normalized = normalized.replaceAll("\\s*!=\\s*", " != ");
        normalized = normalized.replaceAll("\\s*>=\\s*", " >= ");
        normalized = normalized.replaceAll("\\s*<=\\s*", " <= ");

        // 再次压缩多余空格
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }
}
