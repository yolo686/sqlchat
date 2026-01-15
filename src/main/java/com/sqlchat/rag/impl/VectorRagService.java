package com.sqlchat.rag.impl;

import com.sqlchat.model.RagContext;
import com.sqlchat.model.TableInfo;
import com.sqlchat.rag.RagService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于向量检索的RAG服务
 * @author sqlChat
 */
@Service
public class VectorRagService implements RagService {

    @Override
    public RagContext retrieveContext(String question, List<TableInfo> allTables) {
        // 简化实现：直接使用所有表结构信息，生成Schema描述
        String schemaDescription = generateSchemaDescription(allTables);
        
        // 返回基本的业务术语和SQL示例
        List<String> businessTerms = new ArrayList<>();
        businessTerms.add("应收账款：企业因销售商品、提供服务等经营活动应收取的款项");
        businessTerms.add("应付账款：企业因购买商品、接受服务等经营活动应支付的款项");
        businessTerms.add("营业收入：企业在日常经营活动中形成的、会导致所有者权益增加的、与所有者投入资本无关的经济利益的总流入");
        
        List<String> sqlExamples = new ArrayList<>();
        sqlExamples.add("SELECT SUM(amount) as total FROM accounts_receivable WHERE create_date >= '2024-01-01'");
        sqlExamples.add("SELECT customer_name, SUM(amount) as total FROM sales_order GROUP BY customer_name");
        
        return new RagContext(allTables, businessTerms, sqlExamples, schemaDescription);
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

//    private EmbeddingModel embeddingModel;
//    private EmbeddingStore<TextSegment> schemaStore; // Schema存储
//    private EmbeddingStore<TextSegment> termStore; // 业务术语存储
//    private EmbeddingStore<TextSegment> sqlExampleStore; // SQL示例存储

//    @PostConstruct
//    public void init() {
//        // 初始化嵌入模型
//        embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
//
//        // 初始化向量存储
//        schemaStore = new InMemoryEmbeddingStore<>();
//        termStore = new InMemoryEmbeddingStore<>();
//        sqlExampleStore = new InMemoryEmbeddingStore<>();
//
//        // 初始化默认数据
//        initializeDefaultData();
//    }
//
//    @Override
//    public RagContext retrieveContext(String question, List<TableInfo> allTables) {
//        // 对问题进行向量化
//        Embedding questionEmbedding = embeddingModel.embed(question).content();
//
//        // 检索相关的Schema信息
//        List<TableInfo> relevantTables = retrieveRelevantTables(questionEmbedding, allTables);
//
//        // 检索业务术语
//        List<String> businessTerms = retrieveBusinessTerms(questionEmbedding);
//
//        // 检索SQL示例
//        List<String> sqlExamples = retrieveSqlExamples(questionEmbedding);
//
//        // 生成Schema描述
//        String schemaDescription = generateSchemaDescription(relevantTables);
//
//        return new RagContext(relevantTables, businessTerms, sqlExamples, schemaDescription);
//    }
//
//    /**
//     * 检索相关的表
//     */
//    private List<TableInfo> retrieveRelevantTables(Embedding questionEmbedding, List<TableInfo> allTables) {
//        if (allTables == null || allTables.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        // 计算每个表与问题的相似度
//        Map<TableInfo, Double> similarityMap = new HashMap<>();
//        for (TableInfo table : allTables) {
//            String tableText = buildTableText(table);
//            Embedding tableEmbedding = embeddingModel.embed(tableText).content();
//            double similarity = cosineSimilarity(questionEmbedding.vector(), tableEmbedding.vector());
//            similarityMap.put(table, similarity);
//        }
//
//        // 返回相似度最高的前5个表
//        return similarityMap.entrySet().stream()
//            .sorted(Map.Entry.<TableInfo, Double>comparingByValue().reversed())
//            .limit(5)
//            .map(Map.Entry::getKey)
//            .collect(Collectors.toList());
//    }
//
//    /**
//     * 构建表的文本描述
//     */
//    private String buildTableText(TableInfo table) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("表名: ").append(table.getTableName()).append(" ");
//        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
//            sb.append("说明: ").append(table.getTableComment()).append(" ");
//        }
//        sb.append("字段: ");
//        if (table.getColumns() != null) {
//            for (var column : table.getColumns()) {
//                sb.append(column.getColumnName()).append("(")
//                  .append(column.getDataType()).append(") ");
//                if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
//                    sb.append(column.getColumnComment()).append(" ");
//                }
//            }
//        }
//        return sb.toString();
//    }
//
//    /**
//     * 检索业务术语
//     */
//    private List<String> retrieveBusinessTerms(Embedding questionEmbedding) {
//        // 从向量存储中检索
//        List<String> terms = new ArrayList<>();
//
//        // 这里简化实现，实际应该从向量存储中检索
//        // 示例：返回一些常见的业务术语说明
//        terms.add("应收账款：企业因销售商品、提供服务等经营活动应收取的款项");
//        terms.add("应付账款：企业因购买商品、接受服务等经营活动应支付的款项");
//        terms.add("营业收入：企业在日常经营活动中形成的、会导致所有者权益增加的、与所有者投入资本无关的经济利益的总流入");
//
//        return terms;
//    }
//
//    /**
//     * 检索SQL示例
//     */
//    private List<String> retrieveSqlExamples(Embedding questionEmbedding) {
//        // 从向量存储中检索
//        List<String> examples = new ArrayList<>();
//
//        // 这里简化实现，实际应该从向量存储中检索
//        // 示例：返回一些常见的SQL示例
//        examples.add("SELECT SUM(amount) as total FROM accounts_receivable WHERE create_date >= '2024-01-01'");
//        examples.add("SELECT customer_name, SUM(amount) as total FROM sales_order GROUP BY customer_name");
//
//        return examples;
//    }
//
//    /**
//     * 生成Schema描述
//     */
//    private String generateSchemaDescription(List<TableInfo> tables) {
//        if (tables == null || tables.isEmpty()) {
//            return "暂无相关表结构信息";
//        }
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("数据库Schema信息:\n");
//        for (TableInfo table : tables) {
//            sb.append("\n表: ").append(table.getTableName());
//            if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
//                sb.append(" (").append(table.getTableComment()).append(")");
//            }
//            sb.append("\n字段:\n");
//            if (table.getColumns() != null) {
//                for (var column : table.getColumns()) {
//                    sb.append("  - ").append(column.getColumnName())
//                      .append(": ").append(column.getDataType());
//                    if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
//                        sb.append(" (").append(column.getColumnComment()).append(")");
//                    }
//                    if (Boolean.TRUE.equals(column.getIsPrimaryKey())) {
//                        sb.append(" [主键]");
//                    }
//                    sb.append("\n");
//                }
//            }
//        }
//        return sb.toString();
//    }
//
//    /**
//     * 计算余弦相似度
//     */
//    private double cosineSimilarity(float[] vec1, float[] vec2) {
//        if (vec1.length != vec2.length) {
//            return 0.0;
//        }
//
//        double dotProduct = 0.0;
//        double norm1 = 0.0;
//        double norm2 = 0.0;
//
//        for (int i = 0; i < vec1.length; i++) {
//            dotProduct += vec1[i] * vec2[i];
//            norm1 += vec1[i] * vec1[i];
//            norm2 += vec2[i] * vec2[i];
//        }
//
//        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
//    }
//
//    /**
//     * 初始化默认数据
//     */
//    private void initializeDefaultData() {
//        // 可以在这里初始化一些默认的Schema、业务术语和SQL示例
//        // 实际应用中，这些数据应该从数据库或配置文件中加载
//    }
}
