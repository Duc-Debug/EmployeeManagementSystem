package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity;

import jakarta.persistence.*;
import java.time.Instant;

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

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "email")
    private String email;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Version
    private Long version;

    public UserJpaEntity() {
    }

    public UserJpaEntity(Long id, String username, String passwordHash, RoleJpaEntity role, Boolean isActive) {
        this(id, username, passwordHash, role, isActive, null, null, null);
    }

    public UserJpaEntity(Long id, String username, String passwordHash, RoleJpaEntity role, Boolean isActive, Long version) {
        this(id, username, passwordHash, role, isActive, null, null, version);
    }

    public UserJpaEntity(Long id, String username, String passwordHash, RoleJpaEntity role, Boolean isActive, String email, Instant passwordChangedAt, Long version) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
        this.email = email;
        this.passwordChangedAt = passwordChangedAt;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
