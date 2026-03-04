package com.sqlchat.rag.impl;

import com.sqlchat.model.RagContext;
import com.sqlchat.model.TableInfo;
import com.sqlchat.model.ParsedQuestion;
import com.sqlchat.rag.RagService;
import com.sqlchat.service.KnowledgeBaseService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 基于向量检索的RAG服务
 * @author sqlChat
 */
@Service
public class VectorRagService implements RagService {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    private final AllMiniLmL6V2QuantizedEmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

    // 缓存每个用户+领域+类型的检索器
    private final Map<String, EmbeddingStoreContentRetriever> retrieverCache = new HashMap<>();

    /**
     * 获取检索器（按类型+领域）
     */
    private EmbeddingStoreContentRetriever getRetriever(String userId, String type, String domain) {
        String cacheKey = userId + "|" + (domain == null ? "" : domain.trim()) + "|" + type;
        return retrieverCache.computeIfAbsent(cacheKey, key -> {
            InMemoryEmbeddingStore<TextSegment> store = knowledgeBaseService.getStoreForUser(userId, type, domain);
            return EmbeddingStoreContentRetriever.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(store)
                    .maxResults(5)
                    .displayName(type)
                    .build();
        });
    }

    /**
     * 清除用户的检索器缓存（当知识库更新时调用）
     */
    public void clearUserCache(String userId) {
        String prefix = userId + "|";
        retrieverCache.keySet().removeIf(key -> key.startsWith(prefix));
    }


    @Override
    public RagContext retrieveContext(String question, List<TableInfo> allTables) {
        return retrieveContext(question, allTables, null);
    }

    /**
     * 检索上下文（带用户ID）
     */
    public RagContext retrieveContext(String question, List<TableInfo> allTables, String userId) {
        return retrieveContext(question, allTables, userId, null);
    }

    /**
     * 检索上下文（带用户ID + 问题解析结果）
     */
    public RagContext retrieveContext(String question, List<TableInfo> allTables, String userId, ParsedQuestion parsedQuestion) {
        // 直接使用所有表结构信息，生成Schema描述
        String schemaDescription = generateSchemaDescription(allTables);

        List<String> generalDocs = new ArrayList<>();
        List<String> businessRules = new ArrayList<>();
        List<String> termMappings = new ArrayList<>();
        List<String> sqlExamples = new ArrayList<>();
        String matchedDomain = null;

        if (userId != null) {
            try {
                String domain = parsedQuestion != null ? parsedQuestion.getDomain() : null;
                if (domain != null && knowledgeBaseService.hasDomainKnowledge(userId, domain)) {
                    matchedDomain = domain;
                }

                List<Content> sqlContents = getRetriever(userId, KnowledgeBaseService.TYPE_SQL_EXAMPLE, matchedDomain)
                        .retrieve(Query.from(question));
                sqlContents.forEach(content -> {
                    TextSegment segment = content.textSegment();
                    String questionText = segment.text();
                    String sqlContent = segment.metadata().getString("sql_content");
                    sqlExamples.add(sqlContent != null && !sqlContent.trim().isEmpty()
                            ? questionText + "\nSQL: " + sqlContent
                            : questionText);
                });

                List<Content> docContents = getRetriever(userId, KnowledgeBaseService.TYPE_GENERAL_DOC, matchedDomain)
                        .retrieve(Query.from(question));
                docContents.forEach(content -> generalDocs.add(content.textSegment().text()));

                List<Content> ruleContents = getRetriever(userId, KnowledgeBaseService.TYPE_BUSINESS_RULE, matchedDomain)
                        .retrieve(Query.from(question));
                ruleContents.forEach(content -> businessRules.add(content.textSegment().text()));

                List<Content> mappingContents = getRetriever(userId, KnowledgeBaseService.TYPE_TERM_MAPPING, matchedDomain)
                        .retrieve(Query.from(question));
                mappingContents.forEach(content -> termMappings.add(content.textSegment().text()));
            } catch (Exception e) {
                System.err.println("检索知识库时出错: " + e.getMessage());
            }
        }

        System.out.println("命中领域：" + matchedDomain);
        System.out.println("参考文档：" + generalDocs);
        System.out.println("SQL示例：" + sqlExamples);

        return new RagContext(allTables, generalDocs, businessRules, termMappings, sqlExamples, schemaDescription, matchedDomain);
    }

    /**
     * 生成Schema描述
     */
    private String generateSchemaDescription(List<TableInfo> tables) {
        if (tables == null || tables.isEmpty()) {
            return "暂无相关表结构信息";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("数据库Schema信息:\n");
        for (TableInfo table : tables) {
            sb.append("\n表: ").append(table.getTableName());
            if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
                sb.append(" (").append(table.getTableComment()).append(")");
            }
            sb.append("\n字段:\n");
            if (table.getColumns() != null) {
                for (var column : table.getColumns()) {
                    sb.append("  - ").append(column.getColumnName())
                            .append(": ").append(column.getDataType());
                    if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
                        sb.append(" (").append(column.getColumnComment()).append(")");
                    }
                    if (Boolean.TRUE.equals(column.getIsPrimaryKey())) {
                        sb.append(" [主键]");
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
}