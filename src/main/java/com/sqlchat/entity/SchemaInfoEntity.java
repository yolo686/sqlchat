package com.sqlchat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Schema信息实体（缓存外部数据库的表结构，支持用户自定义注释）
 * @author sqlChat
 */
@Entity
@Table(name = "schema_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaInfoEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "database_config_id", nullable = false, length = 64)
    private String databaseConfigId;

    @Column(name = "table_name", nullable = false, length = 200)
    private String tableName;

    @Column(name = "table_comment", length = 500)
    private String tableComment;

    @Column(name = "column_name", nullable = false, length = 200)
    private String columnName;

    @Column(name = "data_type", length = 100)
    private String dataType;

    @Column(name = "column_size")
    private Integer columnSize;

    @Column(name = "nullable")
    private Boolean nullable;

    @Column(name = "is_primary_key")
    private Boolean isPrimaryKey;

    @Column(name = "column_comment", length = 500)
    private String columnComment;

    @Column(name = "ordinal_position")
    private Integer ordinalPosition;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
