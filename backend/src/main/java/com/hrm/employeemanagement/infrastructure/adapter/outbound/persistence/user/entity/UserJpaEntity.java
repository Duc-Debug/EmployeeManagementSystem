package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleJpaEntity role;
    @Column(name = "data_scope", nullable = false, length = 30)
    private String dataScope;

    @Column(name = "scope_org_unit_id")
    private Long scopeOrgUnitId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "email")
    private String email;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion;

    @Version
    private Long version;

    public UserJpaEntity() {
    }

    public UserJpaEntity(Long id, String username, String passwordHash, RoleJpaEntity role, Boolean isActive) {
        this(id, username, passwordHash, role, isActive, null, null, 1, null);
    }

    public UserJpaEntity(Long id, String username, String passwordHash, RoleJpaEntity role, Boolean isActive, Long version) {
        this(id, username, passwordHash, role, isActive, null, null, 1, version);
    }

    public UserJpaEntity(Long id, String username, String passwordHash, RoleJpaEntity role, Boolean isActive, String email, Instant passwordChangedAt, Long version) {
        this(id, username, passwordHash, role, isActive, email, passwordChangedAt, 1, version);
    }

    public UserJpaEntity(Long id, String username, String passwordHash, RoleJpaEntity role, Boolean isActive, String email, Instant passwordChangedAt, Integer tokenVersion, Long version) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
        this.email = email;
        this.passwordChangedAt = passwordChangedAt;
        this.tokenVersion = tokenVersion != null ? tokenVersion : 1;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }
    public String getDataScope() {
    return dataScope;
}

    public Long getScopeOrgUnitId() {
    return scopeOrgUnitId;
}

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public RoleJpaEntity getRole() {
        return role;
    }

    public void setRole(RoleJpaEntity role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(Instant passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public Integer getTokenVersion() {
        return tokenVersion != null ? tokenVersion : 1;
    }

    public void setTokenVersion(Integer tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
    public void setDataScope(String dataScope) {
    this.dataScope = dataScope;
}

public void setScopeOrgUnitId(Long scopeOrgUnitId) {
    this.scopeOrgUnitId = scopeOrgUnitId;
}
}
