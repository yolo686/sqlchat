package com.sqlchat.repository;

import com.sqlchat.entity.QueryTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 查询模板Repository
 * @author sqlChat
 */
@Repository
public interface QueryTemplateRepository extends JpaRepository<QueryTemplateEntity, String> {
    List<QueryTemplateEntity> findByUserId(String userId);

    Page<QueryTemplateEntity> findByUserId(String userId, Pageable pageable);
}
