package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 解析后的问题
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedQuestion {
    private String originalQuestion;
    private String normalizedQuestion; // 标准化后的问题
    private String domain; // 识别出的领域（预算执行/工程投资/自然资源）
    private List<String> auditTerms; // 审计专业名词
    private List<String> tableNames; // 可能涉及的表名
    private List<String> columnNames; // 可能涉及的列名
    private String intent; // 查询意图
    private Map<String, String> keyEntities; // 关键实体（时间、金额阈值等）
}
