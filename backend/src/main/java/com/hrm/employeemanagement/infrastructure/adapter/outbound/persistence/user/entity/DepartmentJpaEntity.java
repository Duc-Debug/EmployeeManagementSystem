package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class DepartmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    public DepartmentJpaEntity() {
    }

    public DepartmentJpaEntity(Long id, String code, String name, Long parentId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.parentId = parentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
