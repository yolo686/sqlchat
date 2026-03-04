package com.sqlchat.formatter.impl;

import com.sqlchat.formatter.PromptFormatter;
import com.sqlchat.model.ParsedQuestion;
import com.sqlchat.model.RagContext;
import org.springframework.stereotype.Component;

/**
 * 默认提示词格式化器
 * @author sqlChat
 */
@Component
public class DefaultPromptFormatter implements PromptFormatter {

    private static final String DEFAULT_TEMPLATE = """
        你是一个专业的SQL生成助手，专门用于审计场景下的自然语言转SQL任务。
        
        ## 数据库Schema信息
        {{SCHEMA_DESCRIPTION}}
        
        ## 领域识别
        {{DOMAIN}}
        
        ## 意图分类
        {{INTENT}}
        
        ## 关键实体
        {{KEY_ENTITIES}}
        
        ## 审计通用文档
        {{GENERAL_DOCS}}
        
        ## 审计业务规则
        {{BUSINESS_RULES}}
        
        ## 术语映射
        {{TERM_MAPPINGS}}
        
        ## 历史SQL示例
        {{SQL_EXAMPLES}}
        
        ## 用户问题
        {{USER_QUESTION}}
        
        ## 要求
        1. 根据用户问题生成准确的SQL查询语句
        2. 只生成SELECT查询语句，不允许生成INSERT、UPDATE、DELETE等修改语句
        3. 确保SQL语句语法正确，能够正常执行
        4. 优先使用相关的表和字段
        5. 如果问题中涉及审计专业术语，请正确映射到对应的数据库字段
        6. 只返回SQL语句，不要返回其他解释性文字
        
        ## 生成的SQL：
        """;

    @Override
    public String format(ParsedQuestion question, RagContext context, String template) {
        if (template == null || template.trim().isEmpty()) {
            template = DEFAULT_TEMPLATE;
        }
        
        String prompt = template;
        
        // 替换Schema描述
        if (context != null && context.getSchemaDescription() != null) {
            prompt = prompt.replace("{{SCHEMA_DESCRIPTION}}", context.getSchemaDescription());
        } else {
            prompt = prompt.replace("{{SCHEMA_DESCRIPTION}}", "暂无Schema信息");
        }
        
        // 替换领域
        if (context != null && context.getMatchedDomain() != null && !context.getMatchedDomain().isEmpty()) {
            prompt = prompt.replace("{{DOMAIN}}", context.getMatchedDomain());
        } else {
            prompt = prompt.replace("{{DOMAIN}}", "未识别到明确领域");
        }
        prompt = prompt.replace("{{INTENT}}",
                question != null && question.getIntent() != null ? question.getIntent() : "通用查询");
        prompt = prompt.replace("{{KEY_ENTITIES}}", formatEntities(question));

        prompt = prompt.replace("{{GENERAL_DOCS}}", formatSectionList(context != null ? context.getGeneralDocs() : null, "暂无通用文档"));
        prompt = prompt.replace("{{BUSINESS_RULES}}", formatSectionList(context != null ? context.getBusinessRules() : null, "暂无业务规则"));
        prompt = prompt.replace("{{TERM_MAPPINGS}}", formatSectionList(context != null ? context.getTermMappings() : null, "暂无术语映射"));
        
        // 替换SQL示例
        if (context != null && context.getSqlExamples() != null && !context.getSqlExamples().isEmpty()) {
            StringBuilder examplesBuilder = new StringBuilder();
            for (String example : context.getSqlExamples()) {
                examplesBuilder.append("```sql\n").append(example).append("\n```\n");
            }
            prompt = prompt.replace("{{SQL_EXAMPLES}}", examplesBuilder.toString());
        } else {
            prompt = prompt.replace("{{SQL_EXAMPLES}}", "暂无SQL示例");
        }
        
        // 替换用户问题
        String userQuestion = question != null && question.getNormalizedQuestion() != null 
            ? question.getNormalizedQuestion() 
            : (question != null ? question.getOriginalQuestion() : "");
        prompt = prompt.replace("{{USER_QUESTION}}", userQuestion);
        
        // 添加审计专业名词映射提示
        if (question != null && question.getAuditTerms() != null && !question.getAuditTerms().isEmpty()) {
            StringBuilder auditTermsBuilder = new StringBuilder("\n## 审计专业名词识别\n");
            for (String term : question.getAuditTerms()) {
                auditTermsBuilder.append("- ").append(term).append("\n");
            }
            prompt = prompt.replace("## 用户问题", auditTermsBuilder.toString() + "\n## 用户问题");
        }
        
        return prompt;
    }

    private String formatSectionList(java.util.List<String> list, String fallback) {
        if (list == null || list.isEmpty()) {
            return fallback;
        }
        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            sb.append("- ").append(item).append("\n");
        }
        return sb.toString();
    }

    private String formatEntities(ParsedQuestion question) {
        if (question == null || question.getKeyEntities() == null || question.getKeyEntities().isEmpty()) {
            return "暂无";
        }
        StringBuilder sb = new StringBuilder();
        question.getKeyEntities().forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append("\n"));
        return sb.toString();
    }
}
