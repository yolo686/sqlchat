package com.sqlchat.parser;

import com.sqlchat.model.ParsedQuestion;

/**
 * 提问解析器接口
 * @author sqlChat
 */
public interface QuestionParser {
    /**
     * 解析用户问题
     */
    ParsedQuestion parse(String question);
}
