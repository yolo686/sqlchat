package com.sqlchat.rag;

import com.sqlchat.model.RagContext;
import com.sqlchat.model.TableInfo;

import java.util.List;

/**
 * RAG服务接口
 * @author sqlChat
 */
public interface RagService {
    /**
     * 检索相关上下文
     */
    RagContext retrieveContext(String question, List<TableInfo> allTables);
}
