package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskAttachmentJpaEntity;

@Repository
public interface SpringDataTaskAttachmentRepository extends JpaRepository<TaskAttachmentJpaEntity, Long> {

    List<TaskAttachmentJpaEntity> findByTaskIdOrderByUploadedAtDesc(Long taskId);

    List<TaskAttachmentJpaEntity> findByCommentId(Long commentId);
    
    boolean existsByFilePath(String filePath);
}

