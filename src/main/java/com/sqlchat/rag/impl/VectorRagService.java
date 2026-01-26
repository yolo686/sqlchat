package com.sqlchat.rag.impl;

import com.sqlchat.model.RagContext;
import com.sqlchat.model.TableInfo;
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

    // 缓存每个用户的检索器
    private final Map<String, EmbeddingStoreContentRetriever> sqlRetrievers = new HashMap<>();
    private final Map<String, EmbeddingStoreContentRetriever> docRetrievers = new HashMap<>();

    /**
     * 获取用户的SQL示例检索器
     * 注意：用户登录时已经初始化知识库，这里直接使用即可
     */
    private EmbeddingStoreContentRetriever getSqlRetriever(String userId) {
        return sqlRetrievers.computeIfAbsent(userId, uid -> {
            InMemoryEmbeddingStore<TextSegment> store = knowledgeBaseService.getSqlStoreForUser(uid);
            // 如果存储为空，说明还未初始化，进行初始化
            if (store == null) {
                knowledgeBaseService.initializeUserKnowledgeBase(uid);
                store = knowledgeBaseService.getSqlStoreForUser(uid);
            }

            return EmbeddingStoreContentRetriever.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(store)
                    .maxResults(3)
                    .displayName("SQL正确示例")
                    .build();
        });
    }

    /**
     * 获取用户的文档检索器
     * 注意：用户登录时已经初始化知识库，这里直接使用即可
     */
    private EmbeddingStoreContentRetriever getDocRetriever(String userId) {

        return docRetrievers.computeIfAbsent(userId, uid -> {
            InMemoryEmbeddingStore<TextSegment> store = knowledgeBaseService.getDocStoreForUser(uid);
            // 如果存储为空，说明还未初始化，进行初始化
            if (store == null) {
                knowledgeBaseService.initializeUserKnowledgeBase(uid);
                store = knowledgeBaseService.getDocStoreForUser(uid);
            }

            return EmbeddingStoreContentRetriever.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(store)
                    .maxResults(3)
                    .displayName("文档")
                    .build();
        });
    }

    /**
     * 清除用户的检索器缓存（当知识库更新时调用）
     */
    public void clearUserCache(String userId) {
        sqlRetrievers.remove(userId);
        docRetrievers.remove(userId);
    }


    @Override
    public RagContext retrieveContext(String question, List<TableInfo> allTables) {
        return retrieveContext(question, allTables, null);
    }

    /**
     * 检索上下文（带用户ID）
     */
    public RagContext retrieveContext(String question, List<TableInfo> allTables, String userId) {
        // 直接使用所有表结构信息，生成Schema描述
        String schemaDescription = generateSchemaDescription(allTables);

        List<String> docs = new ArrayList<>();
        List<String> sqlExamples = new ArrayList<>();

        if (userId != null) {
            // 从用户的知识库中检索
            try {
                EmbeddingStoreContentRetriever docRetriever = getDocRetriever(userId);
                List<Content> docs_content = docRetriever.retrieve(Query.from(question));
                docs_content.forEach(content -> docs.add(content.textSegment().text()));

                EmbeddingStoreContentRetriever sqlRetriever = getSqlRetriever(userId);
                List<Content> sql_examples = sqlRetriever.retrieve(Query.from(question));
                sql_examples.forEach(content -> {
                    TextSegment segment = content.textSegment();
                    String questionText = segment.text(); // 检索到的提问
                    String sqlContent = segment.metadata().getString("sql_content"); // 从metadata获取SQL语句

                    // 组装返回格式：提问 + SQL语句
                    if (sqlContent != null && !sqlContent.trim().isEmpty()) {
                        String combined = questionText + "\nSQL: " + sqlContent;
                        sqlExamples.add(combined);
                    } else {
                        // 如果没有SQL语句，只返回提问（降级处理）
                        sqlExamples.add(questionText);
                    }
                });
            } catch (Exception e) {
                System.err.println("检索知识库时出错: " + e.getMessage());
            }
        }

        System.out.println("参考文档：" + docs);
        System.out.println("SQL示例：" + sqlExamples);

        return new RagContext(allTables, docs, sqlExamples, schemaDescription);
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