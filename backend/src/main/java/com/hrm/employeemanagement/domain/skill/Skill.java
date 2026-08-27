package com.hrm.employeemanagement.domain.skill;

import java.time.LocalDateTime;
import java.util.Objects;

public class Skill {

    private final Long id;
    private final String code;
    private String name;
    private String category;
    private String description;
    private final LocalDateTime createdAt;

    public Skill(Long id, String code, String name, String category, String description, LocalDateTime createdAt) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã kỹ năng không được để trống");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên kỹ năng không được để trống");
        }
        this.id = id;
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.category = category != null ? category.trim() : "GENERAL";
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Skill create(String code, String name, String category, String description) {
        return new Skill(null, code, name, category, description, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Skill skill = (Skill) o;
        return Objects.equals(id, skill.id) || Objects.equals(code, skill.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
