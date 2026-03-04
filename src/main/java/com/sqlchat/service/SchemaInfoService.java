package com.sqlchat.service;

import com.sqlchat.connection.DatabaseConnection;
import com.sqlchat.connection.DatabaseConnectionFactory;
import com.sqlchat.entity.SchemaInfoEntity;
import com.sqlchat.model.ColumnInfo;
import com.sqlchat.model.DatabaseConfig;
import com.sqlchat.model.TableInfo;
import com.sqlchat.repository.SchemaInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Schema信息服务（管理本地缓存的Schema元数据）
 * @author sqlChat
 */
@Service
public class SchemaInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaInfoService.class);

    @Autowired
    private SchemaInfoRepository schemaInfoRepository;

    @Autowired
    private DatabaseConnectionFactory connectionFactory;

    /**
     * 从远程数据库拉取Schema并保存到本地MySQL
     * 如果本地已有Schema，保留用户编辑过的注释
     */
    @Transactional
    public List<TableInfo> fetchAndSaveSchema(DatabaseConfig dbConfig) throws Exception {
        String configId = dbConfig.getId();

        // 1. 从远程数据库获取最新Schema
        DatabaseConnection connection = connectionFactory.getConnection(dbConfig.getType());
        List<TableInfo> remoteTables = connection.getAllTableInfo(dbConfig);

        // 2. 获取本地已有的注释（用于保留用户自定义注释）
        Map<String, String> existingTableComments = new HashMap<>();
        Map<String, String> existingColumnComments = new HashMap<>();
        List<SchemaInfoEntity> existingEntities = schemaInfoRepository
                .findByDatabaseConfigIdOrderByTableNameAscOrdinalPositionAsc(configId);
        for (SchemaInfoEntity entity : existingEntities) {
            String tableKey = entity.getTableName();
            String columnKey = entity.getTableName() + "." + entity.getColumnName();
            if (entity.getTableComment() != null && !entity.getTableComment().isEmpty()) {
                existingTableComments.putIfAbsent(tableKey, entity.getTableComment());
            }
            if (entity.getColumnComment() != null && !entity.getColumnComment().isEmpty()) {
                existingColumnComments.put(columnKey, entity.getColumnComment());
            }
        }

        // 3. 删除旧的Schema信息
        schemaInfoRepository.deleteByDatabaseConfigId(configId);

        // 4. 保存新的Schema信息（合并用户注释）
        List<SchemaInfoEntity> newEntities = new ArrayList<>();
        for (TableInfo table : remoteTables) {
            String tableComment = existingTableComments.getOrDefault(
                    table.getTableName(),
                    table.getTableComment() != null ? table.getTableComment() : ""
            );

            if (table.getColumns() != null) {
                int position = 0;
                for (ColumnInfo column : table.getColumns()) {
                    String columnKey = table.getTableName() + "." + column.getColumnName();
                    String columnComment = existingColumnComments.getOrDefault(
                            columnKey,
                            column.getColumnComment() != null ? column.getColumnComment() : ""
                    );

                    SchemaInfoEntity entity = new SchemaInfoEntity();
                    entity.setId(UUID.randomUUID().toString().replace("-", ""));
                    entity.setDatabaseConfigId(configId);
                    entity.setTableName(table.getTableName());
                    entity.setTableComment(tableComment);
                    entity.setColumnName(column.getColumnName());
                    entity.setDataType(column.getDataType() != null ? column.getDataType() : "");
                    entity.setColumnSize(column.getColumnSize() != null ? column.getColumnSize() : 0);
                    entity.setNullable(column.getNullable() != null ? column.getNullable() : true);
                    entity.setIsPrimaryKey(column.getIsPrimaryKey() != null ? column.getIsPrimaryKey() : false);
                    entity.setColumnComment(columnComment);
                    entity.setOrdinalPosition(position++);

                    newEntities.add(entity);
                }
            }
        }

        schemaInfoRepository.saveAll(newEntities);
        logger.info("已保存数据库配置[{}]的Schema信息，共{}个字段", configId, newEntities.size());

        return buildTableInfoList(newEntities);
    }

    /**
     * 从本地MySQL获取Schema信息
     */
    public List<TableInfo> getLocalSchema(String databaseConfigId) {
        List<SchemaInfoEntity> entities = schemaInfoRepository
                .findByDatabaseConfigIdOrderByTableNameAscOrdinalPositionAsc(databaseConfigId);
        return buildTableInfoList(entities);
    }

    /**
     * 检查是否存在本地Schema
     */
    public boolean hasLocalSchema(String databaseConfigId) {
        return schemaInfoRepository.existsByDatabaseConfigId(databaseConfigId);
    }

    /**
     * 更新表注释
     */
    @Transactional
    public void updateTableComment(String databaseConfigId, String tableName, String tableComment) {
        int updated = schemaInfoRepository.updateTableComment(databaseConfigId, tableName, tableComment != null ? tableComment : "");
        if (updated == 0) {
            throw new RuntimeException("未找到表: " + tableName);
        }
        logger.info("更新表注释: configId={}, table={}, comment={}", databaseConfigId, tableName, tableComment);
    }

    /**
     * 更新列注释
     */
    @Transactional
    public void updateColumnComment(String databaseConfigId, String tableName, String columnName, String columnComment) {
        int updated = schemaInfoRepository.updateColumnComment(databaseConfigId, tableName, columnName, columnComment != null ? columnComment : "");
        if (updated == 0) {
            throw new RuntimeException("未找到列: " + tableName + "." + columnName);
        }
        logger.info("更新列注释: configId={}, table={}, column={}, comment={}", databaseConfigId, tableName, columnName, columnComment);
    }

    /**
     * 删除某个数据库配置的所有Schema信息
     */
    @Transactional
    public void deleteByConfigId(String databaseConfigId) {
        schemaInfoRepository.deleteByDatabaseConfigId(databaseConfigId);
    }

    /**
     * 从实体列表构建TableInfo列表
     */
    private List<TableInfo> buildTableInfoList(List<SchemaInfoEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<SchemaInfoEntity>> grouped = entities.stream()
                .collect(Collectors.groupingBy(SchemaInfoEntity::getTableName, LinkedHashMap::new, Collectors.toList()));

        List<TableInfo> result = new ArrayList<>();
        for (Map.Entry<String, List<SchemaInfoEntity>> entry : grouped.entrySet()) {
            String tableName = entry.getKey();
            List<SchemaInfoEntity> columns = entry.getValue();

            String tableComment = columns.isEmpty() ? "" : (columns.get(0).getTableComment() != null ? columns.get(0).getTableComment() : "");

            List<ColumnInfo> columnInfos = columns.stream()
                    .sorted(Comparator.comparingInt(e -> e.getOrdinalPosition() != null ? e.getOrdinalPosition() : 0))
                    .map(e -> {
                        ColumnInfo col = new ColumnInfo();
                        col.setColumnName(e.getColumnName());
                        col.setDataType(e.getDataType());
                        col.setColumnSize(e.getColumnSize());
                        col.setNullable(e.getNullable());
                        col.setColumnComment(e.getColumnComment() != null ? e.getColumnComment() : "");
                        col.setIsPrimaryKey(e.getIsPrimaryKey());
                        return col;
                    })
                    .collect(Collectors.toList());

            TableInfo tableInfo = new TableInfo();
            tableInfo.setTableName(tableName);
            tableInfo.setTableComment(tableComment);
            tableInfo.setColumns(columnInfos);
            result.add(tableInfo);
        }

        return result;
    }
}
