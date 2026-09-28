package com.hrm.employeemanagement.application.dto.task.comment;

public record MentionedUserDto(
        Long userId,
        String username,
        String fullName,
        String email) {
}

