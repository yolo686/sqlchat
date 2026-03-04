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
import com.sqlchat.rag.impl.VectorRagService;
import com.sqlchat.voting.CandidateGenerator;
import com.sqlchat.voting.SelfConsistencyVoter;
import com.sqlchat.voting.VoteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * NL2SQL核心服务
 * @author sqlChat
 */
@Service
public class Nl2SqlService {

    private static final Logger logger = LoggerFactory.getLogger(Nl2SqlService.class);

    @Autowired
    private QuestionParser questionParser;

    @Autowired
    private DatabaseConnectionFactory connectionFactory;

    @Autowired
    private RagService ragService;

    @Autowired
    private VectorRagService vectorRagService;

    @Autowired
    private PromptFormatter promptFormatter;

    @Autowired
    private SqlGenerator sqlGenerator;

    @Autowired
    private SqlExecutorFactory executorFactory;

    @Autowired
    private ConfigService configService;

    @Autowired
    private CandidateGenerator candidateGenerator;

    @Autowired
    private SelfConsistencyVoter selfConsistencyVoter;

    @Autowired
    private SchemaInfoService schemaInfoService;

    /**
     * 执行NL2SQL转换
     */
    public Nl2SqlResponse convert(Nl2SqlRequest request) {
        try {
            // 1. 解析用户问题
            ParsedQuestion parsedQuestion = questionParser.parse(request.getQuestion());

            // 2. 获取数据库配置
            DatabaseConfig dbConfig = configService.getDatabaseConfig(request.getUserId(), request.getDatabaseConfigId());
            if (dbConfig == null) {
                return createErrorResponse("数据库配置不存在: " + request.getDatabaseConfigId());
            }

            // 3. 获取表结构信息（优先从本地缓存获取，包含用户自定义注释）
            List<TableInfo> allTables;
            if (schemaInfoService.hasLocalSchema(dbConfig.getId())) {
                allTables = schemaInfoService.getLocalSchema(dbConfig.getId());
            } else {
                DatabaseConnection connection = connectionFactory.getConnection(dbConfig.getType());
                allTables = connection.getAllTableInfo(dbConfig);
            }

            // 4. RAG检索上下文（使用用户的知识库）
            RagContext ragContext;
            if (vectorRagService instanceof VectorRagService) {
                ragContext = vectorRagService.retrieveContext(request.getQuestion(), allTables, request.getUserId(), parsedQuestion);
            } else {
                ragContext = ragService.retrieveContext(request.getQuestion(), allTables);
            }

            // 5. 格式化提示词
            String prompt = promptFormatter.format(parsedQuestion, ragContext, null);

            String sql;
            VoteResult voteResult = null;
            boolean votingEnabled = Boolean.TRUE.equals(request.getEnableVoting());

            if (votingEnabled) {
                // ====== 多候选 + 投票模式 ======
                logger.info("启用Self-Consistency多候选投票模式");

                // 6. 并发生成多个候选SQL
                List<String> candidates = candidateGenerator.generateCandidates(prompt);

                if (candidates.isEmpty()) {
                    return createErrorResponse("候选SQL生成失败，未获得任何有效候选");
                }

                // 7. 混合投票选出最佳SQL
                voteResult = selfConsistencyVoter.vote(candidates, prompt, dbConfig);
                sql = voteResult.getBestSql();

                logger.info("投票完成：策略={}, 置信度={}, 得票={}/{}, 降级={}",
                        voteResult.getStrategy(), voteResult.getConfidence(),
                        voteResult.getVoteCount(), voteResult.getTotalCandidates(),
                        voteResult.isDegraded());
            } else {
                // ====== 普通单次生成模式 ======
                sql = sqlGenerator.generateSql(prompt);
            }

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
            response.setVotingEnabled(votingEnabled);

            // 填充投票信息
            if (voteResult != null) {
                response.setCandidateSqls(voteResult.getCandidateSqls());
                response.setVoteCount(voteResult.getVoteCount());
                response.setTotalCandidates(voteResult.getTotalCandidates());
                response.setConfidence(voteResult.getConfidence());
                response.setVotingStrategy(voteResult.getStrategy());
                response.setDegraded(voteResult.isDegraded());
            }

            return response;

        } catch (Exception e) {
            return createErrorResponse("处理请求时发生错误: " + e.getMessage());
        }
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
