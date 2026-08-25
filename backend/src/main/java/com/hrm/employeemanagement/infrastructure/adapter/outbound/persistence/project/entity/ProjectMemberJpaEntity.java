package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberJpaEntity.ProjectMemberJpaId.class)
public class ProjectMemberJpaEntity {

    @Id
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Id
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    public ProjectMemberJpaEntity() {
    }

    public ProjectMemberJpaEntity(
            Long projectId,
            Long employeeId
    ) {
        this.projectId = projectId;
        this.employeeId = employeeId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public static class ProjectMemberJpaId implements Serializable {
        private Long projectId;
        private Long employeeId;

        public ProjectMemberJpaId() {
        }

        public ProjectMemberJpaId(
                Long projectId,
                Long employeeId
        ) {
            this.projectId = projectId;
            this.employeeId = employeeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ProjectMemberJpaId that)) {
                return false;
            }

            return Objects.equals(projectId, that.projectId)
                    && Objects.equals(employeeId, that.employeeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectId, employeeId);
        }
    }
}
