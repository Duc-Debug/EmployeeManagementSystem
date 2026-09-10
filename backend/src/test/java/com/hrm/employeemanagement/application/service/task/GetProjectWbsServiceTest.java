package com.hrm.employeemanagement.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.TaskNodeResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class GetProjectWbsServiceTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long PROJECT_ID = 100L;

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    @Mock
    private AuthorizationService authorizationService;

    private GetProjectWbsService service;

    @BeforeEach
    void setUp() {
        service = new GetProjectWbsService(
                loadTaskPort,
                loadProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    private User createCompanyUser() {
        return new User(
                new UserId(CURRENT_USER_ID),
                "admin",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám đốc"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L);
    }

    private Project createActiveProject() {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án",
                10L,
                new EmployeeId(10L),
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                new BigDecimal("100.00"),
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0);
    }

    private Task createTask(Long id, Long parentId, String name, TaskType type, int sortOrder) {
        return new Task(
                new TaskId(id),
                new ProjectId(PROJECT_ID),
                parentId != null ? new TaskId(parentId) : null,
                "PRJ-01-T00" + id,
                name,
                null,
                type,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TaskStatus.TODO,
                sortOrder,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
    }

    @Test
    @DisplayName("Dựng cây WBS nhiều cấp theo thứ tự sắp xếp thành công")
    void testGetProjectWbs_Success_BuildsHierarchicalTree() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        // Tạo cấu trúc:
        // 1. Root Category A (sortOrder: 1)
        //    ├── 2. Sub Task A1 (sortOrder: 1)
        //    └── 3. Sub Task A2 (sortOrder: 2)
        // 4. Root Task B (sortOrder: 2)
        Task rootCategoryA = createTask(1L, null, "Hạng mục Backend", TaskType.CATEGORY, 1);
        Task subTaskA1 = createTask(2L, 1L, "Thiết kế DB", TaskType.TASK, 1);
        Task subTaskA2 = createTask(3L, 1L, "Viết API", TaskType.TASK, 2);
        Task rootTaskB = createTask(4L, null, "Hạng mục Frontend", TaskType.TASK, 2);

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID)))
                .thenReturn(List.of(subTaskA2, rootTaskB, rootCategoryA, subTaskA1)); // Danh sách phẳng lộn xộn

        List<TaskNodeResult> tree = service.getProjectWbs(PROJECT_ID);

        assertThat(tree).hasSize(2); // 2 root nodes

        TaskNodeResult rootA = tree.get(0);
        assertThat(rootA.id()).isEqualTo(1L);
        assertThat(rootA.name()).isEqualTo("Hạng mục Backend");
        assertThat(rootA.children()).hasSize(2);
        assertThat(rootA.children().get(0).id()).isEqualTo(2L);
        assertThat(rootA.children().get(0).name()).isEqualTo("Thiết kế DB");
        assertThat(rootA.children().get(1).id()).isEqualTo(3L);
        assertThat(rootA.children().get(1).name()).isEqualTo("Viết API");

        TaskNodeResult rootB = tree.get(1);
        assertThat(rootB.id()).isEqualTo(4L);
        assertThat(rootB.name()).isEqualTo("Hạng mục Frontend");
        assertThat(rootB.children()).isEmpty();
    }

    @Test
    @DisplayName("Trả về danh sách rỗng khi dự án chưa có task nào")
    void testGetProjectWbs_EmptyTasks() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of());

        List<TaskNodeResult> tree = service.getProjectWbs(PROJECT_ID);

        assertThat(tree).isEmpty();
    }

    @Test
    @DisplayName("Ném lỗi khi dự án không tồn tại")
    void testGetProjectWbs_ProjectNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser()));
        when(loadProjectPort.findById(new ProjectId(999L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProjectWbs(999L))
                .isInstanceOf(ProjectNotFoundException.class);
    }
}
