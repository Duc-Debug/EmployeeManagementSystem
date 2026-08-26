package com.hrm.employeemanagement.application.service.skill;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.skill.*;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.skill.*;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.skill.*;
import com.hrm.employeemanagement.domain.skill.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillService Application Service Unit Tests")
class SkillServiceTest {

    @Mock
    private LoadSkillPort loadSkillPort;

    @Mock
    private SaveSkillPort saveSkillPort;

    @Mock
    private LoadSkillGroupPort loadSkillGroupPort;

    @Mock
    private SaveSkillGroupPort saveSkillGroupPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private CurrentUserPort currentUserPort;

    private SkillService skillService;

    @BeforeEach
    void setUp() {
        skillService = new SkillService(
                loadSkillPort,
                saveSkillPort,
                loadSkillGroupPort,
                saveSkillGroupPort,
                saveAuditLogPort,
                authorizationService,
                currentUserPort
        );
        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(Optional.of(99L));
    }

    @Test
    @DisplayName("Tạo mới Skill thành công khi quyền và dữ liệu hợp lệ")
    void shouldCreateSkillSuccessfully() {
        when(authorizationService.require(PermissionCode.SKILL_CREATE)).thenReturn(99L);
        when(loadSkillGroupPort.existsById(new SkillGroupId(1L))).thenReturn(true);
        when(loadSkillPort.existsByNameIgnoreCase("Java")).thenReturn(false);

        Skill savedSkill = new Skill(new SkillId(1L), 1L, "Java", "Desc", SkillStatus.ACTIVE, null, LocalDateTime.now(), null);
        when(saveSkillPort.save(any(Skill.class))).thenReturn(savedSkill);

        SkillResult result = skillService.execute(new CreateSkillCommand(1L, "Java", "Desc"));

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Java", result.name());
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Ném DuplicateSkillNameException khi tạo Skill trùng tên")
    void shouldThrowWhenCreatingDuplicateSkillName() {
        when(authorizationService.require(PermissionCode.SKILL_CREATE)).thenReturn(99L);
        when(loadSkillGroupPort.existsById(new SkillGroupId(1L))).thenReturn(true);
        when(loadSkillPort.existsByNameIgnoreCase("Java")).thenReturn(true);

        assertThrows(DuplicateSkillNameException.class, () ->
                skillService.execute(new CreateSkillCommand(1L, "Java", "Desc"))
        );
        verify(saveSkillPort, never()).save(any());
    }

    @Test
    @DisplayName("Gộp kỹ năng (Merge Skill) thành công với cơ chế khử trùng lặp (Deduplication)")
    void shouldMergeSkillsSuccessfullyWithDeduplication() {
        when(authorizationService.require(PermissionCode.SKILL_MERGE)).thenReturn(99L);

        Skill targetSkill = new Skill(new SkillId(1L), 1L, "Java", "Target", SkillStatus.ACTIVE, null, LocalDateTime.now(), null);
        Skill sourceSkill2 = new Skill(new SkillId(2L), 1L, "Java Programming", "Source", SkillStatus.ACTIVE, null, LocalDateTime.now(), null);
        Skill sourceSkill3 = new Skill(new SkillId(3L), 1L, "Lập trình Java", "Source", SkillStatus.ACTIVE, null, LocalDateTime.now(), null);

        when(loadSkillPort.findById(new SkillId(1L))).thenReturn(Optional.of(targetSkill));
        when(loadSkillPort.findAllByIdIn(List.of(2L, 3L))).thenReturn(List.of(sourceSkill2, sourceSkill3));

        // Employee 101 có cả Skill 2 và Skill 1 (Target) -> Cần xóa Skill 2
        // Employee 102 chỉ có Skill 2 -> Cần chuyển Skill 2 thành Skill 1
        when(loadSkillPort.findEmployeeIdsWithSkill(2L)).thenReturn(List.of(101L, 102L));
        when(loadSkillPort.hasEmployeeSkill(101L, 1L)).thenReturn(true);
        when(loadSkillPort.hasEmployeeSkill(102L, 1L)).thenReturn(false);

        // Employee 103 có Skill 3 -> Cần chuyển Skill 3 thành Skill 1
        when(loadSkillPort.findEmployeeIdsWithSkill(3L)).thenReturn(List.of(103L));
        when(loadSkillPort.hasEmployeeSkill(103L, 1L)).thenReturn(false);

        SkillResult result = skillService.execute(new MergeSkillCommand(1L, List.of(2L, 3L)));

        assertNotNull(result);
        assertEquals(1L, result.id());

        // Kiểm tra xử lý Employee
        verify(saveSkillPort).removeEmployeeSkill(101L, 2L);
        verify(saveSkillPort).reassignEmployeeSkills(2L, 1L);
        verify(saveSkillPort).reassignEmployeeSkills(3L, 1L);

        // Kiểm tra trạng thái của Source Skills
        assertEquals(SkillStatus.MERGED, sourceSkill2.getStatus());
        assertEquals(new SkillId(1L), sourceSkill2.getMergedIntoSkillId());
        assertEquals(SkillStatus.MERGED, sourceSkill3.getStatus());
        assertEquals(new SkillId(1L), sourceSkill3.getMergedIntoSkillId());

        verify(saveSkillPort, times(2)).save(any(Skill.class));

        // Kiểm tra Audit Log
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals("SKILL_MERGED", audit.getAction());
        assertEquals(1L, audit.getRecordId());
    }

    @Test
    @DisplayName("Ném InvalidSkillMergeException khi kỹ năng đích không ở trạng thái ACTIVE")
    void shouldThrowWhenTargetSkillIsNotActive() {
        when(authorizationService.require(PermissionCode.SKILL_MERGE)).thenReturn(99L);

        Skill inactiveTarget = new Skill(new SkillId(1L), 1L, "Java", "Desc", SkillStatus.INACTIVE, null, null, null);
        when(loadSkillPort.findById(new SkillId(1L))).thenReturn(Optional.of(inactiveTarget));

        assertThrows(InvalidSkillMergeException.class, () ->
                skillService.execute(new MergeSkillCommand(1L, List.of(2L)))
        );
    }
}
