package com.hrm.employeemanagement.domain.allocation.template;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hrm.employeemanagement.domain.exception.allocation.InvalidRoleAllocationTemplateException;

public final class ProjectRoleAllocationTemplatePolicy {

    private ProjectRoleAllocationTemplatePolicy() {
    }

    public static void validateTemplate(String templateCode, String name, List<ProjectRoleAllocationTemplateItem> items) {
        validateCode(templateCode);
        validateName(name);
        validateItems(items);
    }

    public static void validateCode(String templateCode) {
        if (templateCode == null || templateCode.trim().isEmpty()) {
            throw new InvalidRoleAllocationTemplateException("Mã mẫu phân bổ không được để trống");
        }
        String trimmed = templateCode.trim();
        if (trimmed.length() > 50) {
            throw new InvalidRoleAllocationTemplateException("Mã mẫu phân bổ không được vượt quá 50 ký tự");
        }
        if (!trimmed.matches("^[A-Za-z0-9_-]+$")) {
            throw new InvalidRoleAllocationTemplateException("Mã mẫu phân bổ chỉ chứa chữ cái, chữ số, gạch dưới hoặc gạch nối");
        }
    }

    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidRoleAllocationTemplateException("Tên mẫu phân bổ không được để trống");
        }
        if (name.trim().length() > 255) {
            throw new InvalidRoleAllocationTemplateException("Tên mẫu phân bổ không được vượt quá 255 ký tự");
        }
    }

    public static void validateItems(List<ProjectRoleAllocationTemplateItem> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidRoleAllocationTemplateException("Mẫu phân bổ phải có ít nhất một vai trò");
        }
        Set<Long> seenRoleIds = new HashSet<>();
        for (ProjectRoleAllocationTemplateItem item : items) {
            if (item == null) {
                throw new InvalidRoleAllocationTemplateException("Chi tiết vai trò trong mẫu không được rỗng");
            }
            if (!seenRoleIds.add(item.getRoleId())) {
                throw new InvalidRoleAllocationTemplateException("Không được trùng lặp vai trò trong cùng một mẫu phân bổ");
            }
        }
    }
}

