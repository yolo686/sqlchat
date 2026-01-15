package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表信息
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;
}
