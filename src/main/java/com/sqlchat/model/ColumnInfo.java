package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列信息
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {
    private String columnName;
    private String dataType;
    private Integer columnSize;
    private Boolean nullable;
    private String columnComment;
    private Boolean isPrimaryKey;
}
