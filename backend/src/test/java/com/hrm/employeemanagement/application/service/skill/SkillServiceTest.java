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
    @DisplayName("Gộp kỹ năng (Merge Skill) thành công với cơ chế khử trùng lặp (Deduplication) nguyên tử")
    void shouldMergeSkillsSuccessfullyWithDeduplication() {
        when(authorizationService.require(PermissionCode.SKILL_MERGE)).thenReturn(99L);

        Skill targetSkill = new Skill(new SkillId(1L), 1L, "Java", "Target", SkillStatus.ACTIVE, null, LocalDateTime.now(), null);
        Skill sourceSkill2 = new Skill(new SkillId(2L), 1L, "Java Programming", "Source", SkillStatus.ACTIVE, null, LocalDateTime.now(), null);
        Skill sourceSkill3 = new Skill(new SkillId(3L), 1L, "Lập trình Java", "Source", SkillStatus.ACTIVE, null, LocalDateTime.now(), null);

        when(loadSkillPort.findById(new SkillId(1L))).thenReturn(Optional.of(targetSkill));
        when(loadSkillPort.findAllByIdIn(List.of(2L, 3L))).thenReturn(List.of(sourceSkill2, sourceSkill3));

        when(saveSkillPort.deleteDuplicateEmployeeSkills(2L, 1L)).thenReturn(1);
        when(saveSkillPort.reassignEmployeeSkills(2L, 1L)).thenReturn(1);
        when(saveSkillPort.deleteDuplicateEmployeeSkills(3L, 1L)).thenReturn(0);
        when(saveSkillPort.reassignEmployeeSkills(3L, 1L)).thenReturn(1);

        SkillResult result = skillService.execute(new MergeSkillCommand(1L, List.of(2L, 3L)));

        assertNotNull(result);
        assertEquals(1L, result.id());

        // Kiểm tra xử lý Employee qua atomic DB queries
        verify(saveSkillPort).deleteDuplicateEmployeeSkills(2L, 1L);
        verify(saveSkillPort).reassignEmployeeSkills(2L, 1L);
        verify(saveSkillPort).deleteDuplicateEmployeeSkills(3L, 1L);
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

    @Test
    @DisplayName("Ném InvalidSkillMergeException khi danh sách kỹ năng nguồn chứa chính kỹ năng đích")
    void shouldThrowWhenSourceSkillsContainTargetSkill() {
        assertThrows(InvalidSkillMergeException.class, () ->
                new MergeSkillCommand(1L, List.of(2L, 1L))
        );
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi danh sách kỹ năng nguồn rỗng")
    void shouldThrowWhenSourceSkillsIsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                new MergeSkillCommand(1L, List.of())
        );
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi danh sách kỹ năng nguồn chứa phần tử null")
    void shouldThrowWhenSourceSkillsContainsNull() {
        java.util.List<Long> idsWithNull = new java.util.ArrayList<>();
        idsWithNull.add(2L);
        idsWithNull.add(null);
        idsWithNull.add(3L);

        assertThrows(IllegalArgumentException.class, () ->
                new MergeSkillCommand(1L, idsWithNull)
        );
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi danh sách kỹ năng nguồn chứa số âm hoặc 0")
    void shouldThrowWhenSourceSkillsContainsNonPositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new MergeSkillCommand(1L, List.of(2L, -10L, 3L))
        );
        assertThrows(IllegalArgumentException.class, () ->
                new MergeSkillCommand(1L, List.of(2L, 0L, 3L))
        );
    }

    @Test
    @DisplayName("Lấy danh sách kỹ năng theo filter SkillStatus thành công và chỉ load đúng group theo ID")
    void shouldGetSkillsWithSkillStatusFilter() {
        when(authorizationService.require(PermissionCode.SKILL_READ)).thenReturn(99L);
        when(loadSkillPort.findAll(1L, SkillStatus.ACTIVE, "java")).thenReturn(List.of(
                new Skill(new SkillId(1L), 1L, "Java", "Desc", SkillStatus.ACTIVE, null, null, null)
        ));
        when(loadSkillGroupPort.findAllGroupsByIdIn(List.of(1L))).thenReturn(List.of(
                new SkillGroup(new SkillGroupId(1L), "Backend", "Backend group", SkillStatus.ACTIVE, null, null)
        ));

        List<SkillResult> results = skillService.execute(1L, SkillStatus.ACTIVE, "java");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Java", results.get(0).name());
        assertEquals("Backend", results.get(0).groupName());
    }

    @Test
    @DisplayName("Ném NullPointerException khi CurrentUserPort bị null trong constructor")
    void shouldThrowWhenCurrentUserPortIsNull() {
        assertThrows(NullPointerException.class, () ->
                new SkillService(
                        loadSkillPort,
                        saveSkillPort,
                        loadSkillGroupPort,
                        saveSkillGroupPort,
                        saveAuditLogPort,
                        authorizationService,
                        null
                )
        );
    }
}
