package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NL2SQL响应
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nl2SqlResponse {
    private String sql; // 生成的SQL语句
    private SqlResult executionResult; // SQL执行结果（如果执行了）
    private ParsedQuestion parsedQuestion; // 解析后的问题
    private Boolean success; // 是否成功
    private String errorMessage; // 错误信息
}
