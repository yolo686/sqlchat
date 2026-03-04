package com.sqlchat.repository;

import com.sqlchat.entity.SchemaInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Schema信息Repository
 * @author sqlChat
 */
@Repository
public interface SchemaInfoRepository extends JpaRepository<SchemaInfoEntity, String> {

    /**
     * 获取某个数据库配置的所有Schema信息
     */
    List<SchemaInfoEntity> findByDatabaseConfigIdOrderByTableNameAscOrdinalPositionAsc(String databaseConfigId);

    /**
     * 获取某个数据库配置下某个表的所有列信息
     */
    List<SchemaInfoEntity> findByDatabaseConfigIdAndTableNameOrderByOrdinalPositionAsc(String databaseConfigId, String tableName);

    /**
     * 删除某个数据库配置的所有Schema信息
     */
    void deleteByDatabaseConfigId(String databaseConfigId);

    /**
     * 更新表注释（批量更新同一张表所有行的tableComment）
     */
    @Modifying
    @Query("UPDATE SchemaInfoEntity s SET s.tableComment = :tableComment WHERE s.databaseConfigId = :configId AND s.tableName = :tableName")
    int updateTableComment(@Param("configId") String configId, @Param("tableName") String tableName, @Param("tableComment") String tableComment);

    /**
     * 更新列注释
     */
    @Modifying
    @Query("UPDATE SchemaInfoEntity s SET s.columnComment = :columnComment WHERE s.databaseConfigId = :configId AND s.tableName = :tableName AND s.columnName = :columnName")
    int updateColumnComment(@Param("configId") String configId, @Param("tableName") String tableName, @Param("columnName") String columnName, @Param("columnComment") String columnComment);

    /**
     * 检查某个数据库配置是否已有Schema信息
     */
    boolean existsByDatabaseConfigId(String databaseConfigId);
}
