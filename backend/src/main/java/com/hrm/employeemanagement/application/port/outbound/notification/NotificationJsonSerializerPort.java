package com.hrm.employeemanagement.application.port.outbound.notification;

/**
 * Port tuần tự hóa JSON phục vụ ghi audit log và message snapshots.
 * Tách biệt logic serialization khỏi application layer để tuân thủ Hexagonal Architecture.
 */
public interface NotificationJsonSerializerPort {

    /**
     * Chuyển đổi payload thành chuỗi JSON chuẩn.
     *
     * @param payload đối tượng cần serialize
     * @return chuỗi JSON
     */
    String toJson(Object payload);
}
