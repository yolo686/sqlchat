package com.sqlchat.formatter;

import com.sqlchat.model.ParsedQuestion;
import com.sqlchat.model.RagContext;

/**
 * 提示词格式化器接口
 * @author sqlChat
 */
public interface PromptFormatter {
    /**
     * 格式化提示词
     */
    String format(ParsedQuestion question, RagContext context, String template);
}
