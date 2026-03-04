package com.sqlchat.service;

import com.sqlchat.connection.DatabaseConnection;
import com.sqlchat.connection.DatabaseConnectionFactory;
import com.sqlchat.entity.DatabaseConfigEntity;
import com.sqlchat.entity.QueryTemplateEntity;
import com.sqlchat.model.DatabaseConfig;
import com.sqlchat.model.QueryTemplate;
import com.sqlchat.model.TableInfo;
import com.sqlchat.repository.DatabaseConfigRepository;
import com.sqlchat.repository.QueryTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 配置服务（数据库配置和查询模板配置）
 * @author sqlChat
 */
@Service
public class ConfigService {

    @Autowired
    private DatabaseConfigRepository databaseConfigRepository;

    @Autowired
    private QueryTemplateRepository queryTemplateRepository;

    @Autowired
    private DatabaseConnectionFactory connectionFactory;

    // ========== 数据库配置相关 ==========

    /**
     * 保存数据库配置
     */
    public DatabaseConfig saveDatabaseConfig(String userId, DatabaseConfig config) {
        DatabaseConfigEntity entity = new DatabaseConfigEntity();
        if (config.getId() != null) {
            // 更新
            entity = databaseConfigRepository.findById(config.getId())
                .orElseThrow(() -> new RuntimeException("配置不存在"));
            if (!entity.getUserId().equals(userId)) {
                throw new RuntimeException("无权访问此配置");
            }
        } else {
            // 新建
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setUserId(userId);
        }

        entity.setName(config.getName());
        entity.setType(config.getType());
        entity.setHost(config.getHost());
        entity.setPort(config.getPort());
        entity.setDatabase(config.getDatabase());
        entity.setUsername(config.getUsername());
        entity.setPassword(config.getPassword());

        DatabaseConfigEntity saved = databaseConfigRepository.save(entity);
        return convertToDatabaseConfig(saved);
    }

    /**
     * 获取用户的数据库配置列表
     */
    public List<DatabaseConfig> getDatabaseConfigsByUserId(String userId) {
        return databaseConfigRepository.findByUserId(userId).stream()
            .map(this::convertToDatabaseConfig)
            .collect(Collectors.toList());
    }

    /**
     * 获取单个数据库配置
     */
    public DatabaseConfig getDatabaseConfig(String userId, String id) {
        DatabaseConfigEntity entity = databaseConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("配置不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此配置");
        }
        return convertToDatabaseConfig(entity);
    }

    /**
     * 删除数据库配置
     */
    public void deleteDatabaseConfig(String userId, String id) {
        DatabaseConfigEntity entity = databaseConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("配置不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此配置");
        }
        databaseConfigRepository.delete(entity);
    }

    /**
     * 获取数据库Schema（所有表的结构信息）
     */
    public List<TableInfo> getDatabaseSchema(String userId, String id) throws Exception {
        DatabaseConfig config = getDatabaseConfig(userId, id);
        DatabaseConnection connection = connectionFactory.getConnection(config.getType());
        return connection.getAllTableInfo(config);
    }

    /**
     * 实体转模型
     */
    private DatabaseConfig convertToDatabaseConfig(DatabaseConfigEntity entity) {
        DatabaseConfig config = new DatabaseConfig();
        config.setId(entity.getId());
        config.setType(entity.getType());
        config.setName(entity.getName());
        config.setHost(entity.getHost());
        config.setPort(entity.getPort());
        config.setDatabase(entity.getDatabase());
        config.setUsername(entity.getUsername());
        config.setPassword(entity.getPassword());
        return config;
    }

    // ========== 查询模板相关 ==========

    /**
     * 保存查询模板
     */
    public QueryTemplate saveQueryTemplate(String userId, QueryTemplate template) {
        QueryTemplateEntity entity = new QueryTemplateEntity();
        if (template.getId() != null) {
            // 更新
            entity = queryTemplateRepository.findById(template.getId())
                .orElseThrow(() -> new RuntimeException("模板不存在"));
            if (!entity.getUserId().equals(userId)) {
                throw new RuntimeException("无权访问此模板");
            }
        } else {
            // 新建
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setUserId(userId);
        }

        entity.setName(template.getName());
        entity.setDescription(template.getDescription());
        entity.setQueryExample(template.getQueryExample());
        entity.setIsDefault(template.getIsDefault());

        QueryTemplateEntity saved = queryTemplateRepository.save(entity);
        return convertToQueryTemplate(saved);
    }

    /**
     * 获取用户的查询模板列表
     */
    public List<QueryTemplate> getQueryTemplatesByUserId(String userId) {
        return queryTemplateRepository.findByUserId(userId).stream()
            .map(this::convertToQueryTemplate)
            .collect(Collectors.toList());
    }

    /**
     * 获取单个查询模板
     */
    public QueryTemplate getQueryTemplate(String userId, String id) {
        QueryTemplateEntity entity = queryTemplateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此模板");
        }
        return convertToQueryTemplate(entity);
    }

    /**
     * 删除查询模板
     */
    public void deleteQueryTemplate(String userId, String id) {
        QueryTemplateEntity entity = queryTemplateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此模板");
        }
        queryTemplateRepository.delete(entity);
    }

    /**
     * 实体转模型
     */
    private QueryTemplate convertToQueryTemplate(QueryTemplateEntity entity) {
        QueryTemplate template = new QueryTemplate();
        template.setId(entity.getId());
        template.setName(entity.getName());
        template.setDescription(entity.getDescription());
        template.setQueryExample(entity.getQueryExample());
        template.setIsDefault(entity.getIsDefault());
        return template;
    }
}
