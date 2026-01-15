package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NL2SQL请求
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nl2SqlRequest {
    private String userId; // 用户ID
    private String question; // 用户问题
    private String databaseConfigId; // 数据库配置ID
    private Boolean executeSql; // 是否执行SQL
}
