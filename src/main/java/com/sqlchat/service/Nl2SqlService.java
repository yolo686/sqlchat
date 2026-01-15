package com.sqlchat.service;

import com.sqlchat.connection.DatabaseConnection;
import com.sqlchat.connection.DatabaseConnectionFactory;
import com.sqlchat.executor.SqlExecutor;
import com.sqlchat.executor.SqlExecutorFactory;
import com.sqlchat.formatter.PromptFormatter;
import com.sqlchat.llm.SqlGenerator;
import com.sqlchat.model.*;
import com.sqlchat.parser.QuestionParser;
import com.sqlchat.rag.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NL2SQL核心服务
 * @author sqlChat
 */
@Service
public class Nl2SqlService {

    @Autowired
    private QuestionParser questionParser;

    @Autowired
    private DatabaseConnectionFactory connectionFactory;

    @Autowired
    private RagService ragService;

    @Autowired
    private PromptFormatter promptFormatter;

    @Autowired
    private SqlGenerator sqlGenerator;

    @Autowired
    private SqlExecutorFactory executorFactory;

    // 临时存储数据库配置（实际应该从数据库或配置中心读取）
    private final Map<String, DatabaseConfig> databaseConfigs = new HashMap<>();

    // 临时存储提示词模板（实际应该从数据库或配置中心读取）
    private final Map<String, PromptTemplate> promptTemplates = new HashMap<>();

    /**
     * 执行NL2SQL转换
     */
    public Nl2SqlResponse convert(Nl2SqlRequest request) {
        try {
            // 1. 解析用户问题
            ParsedQuestion parsedQuestion = questionParser.parse(request.getQuestion());

            // 2. 获取数据库配置
            DatabaseConfig dbConfig = getDatabaseConfig(request.getDatabaseConfigId());
            if (dbConfig == null) {
                return createErrorResponse("数据库配置不存在: " + request.getDatabaseConfigId());
            }

            // 3. 获取表结构信息
            DatabaseConnection connection = connectionFactory.getConnection(dbConfig.getType());
            List<TableInfo> allTables = connection.getAllTableInfo(dbConfig);

            // 4. RAG检索上下文
            RagContext ragContext = ragService.retrieveContext(request.getQuestion(), allTables);

            // 5. 获取提示词模板
            String template = getPromptTemplate(request.getPromptTemplateId());

            // 6. 格式化提示词
            String prompt = promptFormatter.format(parsedQuestion, ragContext, template);

            // 7. 生成SQL
            String sql = sqlGenerator.generateSql(prompt);

            // 8. 执行SQL（如果需要）
            SqlResult executionResult = null;
            if (Boolean.TRUE.equals(request.getExecuteSql())) {
                SqlExecutor executor = executorFactory.getExecutor(dbConfig.getType());
                try {
                    List<Map<String, Object>> data = executor.executeQuery(dbConfig, sql);
                    executionResult = new SqlResult(sql, data, data.size(), true, null);
                } catch (Exception e) {
                    executionResult = new SqlResult(sql, null, 0, false, e.getMessage());
                }
            }

            // 9. 构建响应
            Nl2SqlResponse response = new Nl2SqlResponse();
            response.setSql(sql);
            response.setExecutionResult(executionResult);
            response.setParsedQuestion(parsedQuestion);
            response.setSuccess(true);
            response.setErrorMessage(null);

            return response;

        } catch (Exception e) {
            return createErrorResponse("处理请求时发生错误: " + e.getMessage());
        }
    }

    /**
     * 保存数据库配置
     */
    public void saveDatabaseConfig(DatabaseConfig config) {
        if (config.getId() == null) {
            config.setId("db_" + System.currentTimeMillis());
        }
        databaseConfigs.put(config.getId(), config);
    }

    /**
     * 获取数据库配置
     */
    public DatabaseConfig getDatabaseConfig(String id) {
        return databaseConfigs.get(id);
    }

    /**
     * 获取所有数据库配置
     */
    public List<DatabaseConfig> getAllDatabaseConfigs() {
        return databaseConfigs.values().stream().toList();
    }

    /**
     * 删除数据库配置
     */
    public void deleteDatabaseConfig(String id) {
        databaseConfigs.remove(id);
    }

    /**
     * 保存提示词模板
     */
    public void savePromptTemplate(PromptTemplate template) {
        if (template.getId() == null) {
            template.setId("template_" + System.currentTimeMillis());
        }
        promptTemplates.put(template.getId(), template);
    }

//    /**
//     * 获取提示词模板
//     */
//    public PromptTemplate getPromptTemplate(String id) {
//        return promptTemplates.get(id);
//    }

    /**
     * 获取所有提示词模板
     */
    public List<PromptTemplate> getAllPromptTemplates() {
        return promptTemplates.values().stream().toList();
    }

    /**
     * 删除提示词模板
     */
    public void deletePromptTemplate(String id) {
        promptTemplates.remove(id);
    }

    /**
     * 获取提示词模板内容
     */
    private String getPromptTemplate(String templateId) {
        if (templateId != null && !templateId.isEmpty()) {
            PromptTemplate template = promptTemplates.get(templateId);
            if (template != null && template.getTemplate() != null) {
                return template.getTemplate();
            }
        }
        return null; // 使用默认模板
    }

    /**
     * 创建错误响应
     */
    private Nl2SqlResponse createErrorResponse(String errorMessage) {
        Nl2SqlResponse response = new Nl2SqlResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
