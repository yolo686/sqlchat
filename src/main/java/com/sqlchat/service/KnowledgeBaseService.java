package com.sqlchat.service;

import com.sqlchat.entity.KnowledgeBaseEntity;
import com.sqlchat.model.KnowledgeBase;
import com.sqlchat.repository.KnowledgeBaseRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库服务
 * @author sqlChat
 */
@Service
public class KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    private final AllMiniLmL6V2QuantizedEmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

    // 每个用户的向量存储（SQL示例和文档分开存储）
    private final Map<String, InMemoryEmbeddingStore<TextSegment>> sqlStores = new HashMap<>();
    private final Map<String, InMemoryEmbeddingStore<TextSegment>> docStores = new HashMap<>();

    /**
     * 获取用户的SQL示例向量存储
     */
    private InMemoryEmbeddingStore<TextSegment> getSqlStore(String userId) {
        return sqlStores.computeIfAbsent(userId, k -> new InMemoryEmbeddingStore<>());
    }

    /**
     * 获取用户的文档向量存储
     */
    private InMemoryEmbeddingStore<TextSegment> getDocStore(String userId) {
        return docStores.computeIfAbsent(userId, k -> new InMemoryEmbeddingStore<>());
    }

    /**
     * 根据类型获取向量存储
     */
    private InMemoryEmbeddingStore<TextSegment> getStore(String userId, String type) {
        if ("SQL_EXAMPLE".equals(type)) {
            return getSqlStore(userId);
        } else {
            return getDocStore(userId);
        }
    }

    /**
     * 初始化用户的知识库向量存储（从数据库加载）
     * 如果向量存储已存在，先清空再重新加载，确保数据一致性
     */
    public void initializeUserKnowledgeBase(String userId) {
        
        // 加载SQL示例
        if(!sqlStores.containsKey(userId)) {
            List<KnowledgeBaseEntity> sqlExamples = knowledgeBaseRepository.findByUserIdAndType(userId, "SQL_EXAMPLE");
            for (KnowledgeBaseEntity entity : sqlExamples) {
                // 使用addEmbedding方法，它会正确处理question和content的组合
                addEmbedding(userId, entity);
            }
        }


        // 加载文档
        if(!docStores.containsKey(userId)) {
            List<KnowledgeBaseEntity> documents = knowledgeBaseRepository.findByUserIdAndType(userId, "DOCUMENT");
            for (KnowledgeBaseEntity entity : documents) {
                // 使用addEmbedding方法
                addEmbedding(userId, entity);
            }
        }

    }

    /**
     * 获取用户的所有知识库（列表形式）
     */
    public List<KnowledgeBase> getKnowledgeBasesByUser(String userId, String type) {
        List<KnowledgeBaseEntity> entities = knowledgeBaseRepository.findByUserIdAndType(userId, type);
        return entities.stream()
            .map(this::convertToModel)
            .sorted(Comparator.comparing(KnowledgeBase::getChunkIndex))
            .collect(Collectors.toList());
    }

    /**
     * 保存知识库（支持新增和更新）
     * chunkIndex自动生成
     */
    @Transactional
    public KnowledgeBase saveKnowledgeBase(String userId, KnowledgeBase knowledgeBase) {
        KnowledgeBaseEntity entity;
        
        if (knowledgeBase.getId() != null && !knowledgeBase.getId().isEmpty()) {
            // 更新
            entity = knowledgeBaseRepository.findById(knowledgeBase.getId())
                .orElseThrow(() -> new RuntimeException("知识库不存在"));
            
            // 检查用户权限
            if (!entity.getUserId().equals(userId)) {
                throw new RuntimeException("无权修改此知识库");
            }
            
            // 更新内容
            entity.setQuestion(knowledgeBase.getQuestion());
            entity.setContent(knowledgeBase.getContent());
            // chunkIndex保持不变
            
            // 重新向量化
            updateEmbedding(userId, entity);
        } else {
            // 新增 - 自动生成chunkIndex
            List<KnowledgeBaseEntity> existing = knowledgeBaseRepository.findByUserIdAndType(userId, knowledgeBase.getType());
            int maxChunkIndex = existing.stream()
                .mapToInt(KnowledgeBaseEntity::getChunkIndex)
                .max()
                .orElse(-1);
            
            entity = new KnowledgeBaseEntity();
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setUserId(userId);
            entity.setType(knowledgeBase.getType());
            entity.setQuestion(knowledgeBase.getQuestion());
            entity.setChunkIndex(maxChunkIndex + 1); // 自动生成
            entity.setContent(knowledgeBase.getContent());
            entity.setEmbeddingId(UUID.randomUUID().toString().replace("-", ""));
            
            // 向量化
            addEmbedding(userId, entity);
        }
        
        entity = knowledgeBaseRepository.save(entity);
        return convertToModel(entity);
    }

    /**
     * 删除知识库
     */
    @Transactional
    public void deleteKnowledgeBase(String userId, String id) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("知识库不存在"));
        
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此知识库");
        }
        
        // 从向量存储中删除
        InMemoryEmbeddingStore<TextSegment> store = getStore(userId, entity.getType());
        if (entity.getEmbeddingId() != null) {
            store.remove(entity.getEmbeddingId());
        }
        
        knowledgeBaseRepository.delete(entity);
    }


    /**
     * 添加向量
     * 对于SQL_EXAMPLE，只对question进行向量化，SQL语句存储在metadata中
     */
    private void addEmbedding(String userId, KnowledgeBaseEntity entity) {
        InMemoryEmbeddingStore<TextSegment> store = getStore(userId, entity.getType());
        
        // 构建向量化文本
        String textForEmbedding;
        TextSegment segment;
        
        if ("SQL_EXAMPLE".equals(entity.getType())) {
            // SQL示例：只对question进行向量化
            if (entity.getQuestion() != null && !entity.getQuestion().trim().isEmpty()) {
                textForEmbedding = entity.getQuestion();
            } else {
                // 如果question为空，降级使用content
                textForEmbedding = entity.getContent() != null ? entity.getContent() : "";
            }
            
            // 创建TextSegment，将SQL语句存储在metadata中
            segment = TextSegment.from(textForEmbedding);
            if (entity.getContent() != null) {
                segment.metadata().put("sql_content", entity.getContent());
            }
            if (entity.getQuestion() != null) {
                segment.metadata().put("question", entity.getQuestion());
            }
        } else {
            // 文档：直接使用内容
            textForEmbedding = entity.getContent() != null ? entity.getContent() : "";
            segment = TextSegment.from(textForEmbedding);
        }
        
        Embedding embedding = embeddingModel.embed(segment).content();
        store.add(entity.getEmbeddingId(), embedding, segment);
    }

    /**
     * 更新向量（删除旧向量，添加新向量）
     */
    private void updateEmbedding(String userId, KnowledgeBaseEntity entity) {
        InMemoryEmbeddingStore<TextSegment> store = getStore(userId, entity.getType());
        
        // 删除旧向量
        if (entity.getEmbeddingId() != null) {
            store.remove(entity.getEmbeddingId());
        }
        
        // 添加新向量
        addEmbedding(userId, entity);
    }

    /**
     * 获取用户的SQL示例向量存储
     */
    public InMemoryEmbeddingStore<TextSegment> getSqlStoreForUser(String userId) {
        return getSqlStore(userId);
    }

    /**
     * 获取用户的文档向量存储
     */
    public InMemoryEmbeddingStore<TextSegment> getDocStoreForUser(String userId) {
        return getDocStore(userId);
    }

    /**
     * 实体转模型
     */
    private KnowledgeBase convertToModel(KnowledgeBaseEntity entity) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(entity.getId());
        kb.setUserId(entity.getUserId());
        kb.setType(entity.getType());
        kb.setQuestion(entity.getQuestion());
        kb.setChunkIndex(entity.getChunkIndex());
        kb.setContent(entity.getContent());
        kb.setEmbeddingId(entity.getEmbeddingId());
        return kb;
    }
}
