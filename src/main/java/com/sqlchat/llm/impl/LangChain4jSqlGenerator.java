package com.sqlchat.llm.impl;

import com.sqlchat.llm.SqlGenerator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 基于LangChain4j的SQL生成器
 * @author sqlChat
 */
@Component
public class LangChain4jSqlGenerator implements SqlGenerator {

    private final OpenAiChatModel chatModel;

    @Autowired
    public LangChain4jSqlGenerator(@Qualifier("openAiChatModel") OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generateSql(String prompt) {
        try {
            String response = chatModel.chat(prompt);
            // 清理响应，提取SQL语句
            return extractSql(response);
        } catch (Exception e) {
            throw new RuntimeException("生成SQL时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 从响应中提取SQL语句
     */
    private String extractSql(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }
        
        String cleaned = response.trim();
        
        // 如果包含```sql标记，提取其中的内容
        if (cleaned.contains("```sql")) {
            int start = cleaned.indexOf("```sql") + 6;
            int end = cleaned.indexOf("```", start);
            if (end > start) {
                cleaned = cleaned.substring(start, end).trim();
            }
        } else if (cleaned.contains("```")) {
            // 如果只有```标记，也尝试提取
            int start = cleaned.indexOf("```") + 3;
            int end = cleaned.indexOf("```", start);
            if (end > start) {
                cleaned = cleaned.substring(start, end).trim();
            }
        }
        
        // 移除可能的SELECT关键字前的其他文字
        int selectIndex = cleaned.toUpperCase().indexOf("SELECT");
        if (selectIndex > 0) {
            cleaned = cleaned.substring(selectIndex);
        }
        
        // 确保以SELECT开头
        if (!cleaned.toUpperCase().startsWith("SELECT")) {
            // 如果响应中没有SELECT，尝试查找
            String[] lines = cleaned.split("\n");
            for (String line : lines) {
                if (line.trim().toUpperCase().startsWith("SELECT")) {
                    cleaned = line.trim();
                    break;
                }
            }
        }
        
        return cleaned.trim();
    }
}
