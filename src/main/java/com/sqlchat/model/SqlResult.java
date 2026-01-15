package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SQL执行结果
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlResult {
    private String sql; // 生成的SQL语句
    private List<Map<String, Object>> data; // 查询结果数据
    private Integer rowCount; // 行数
    private Boolean success; // 是否成功
    private String errorMessage; // 错误信息
}
