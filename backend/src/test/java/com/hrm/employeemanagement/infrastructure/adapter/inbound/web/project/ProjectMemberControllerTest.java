package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.inbound.project.AddProjectMemberUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectMembersUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.RemoveProjectMemberUseCase;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectMemberException;
import com.hrm.employeemanagement.domain.exception.project.MemberHasActiveTasksException;
import com.hrm.employeemanagement.domain.exception.project.ProjectMemberNotFoundException;
import com.hrm.employeemanagement.domain.project.ProjectMemberRole;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class ProjectMemberControllerTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long EMPLOYEE_ID = 50L;

    private MockMvc mockMvc;

    @Mock
    private GetProjectMembersUseCase getProjectMembersUseCase;
    @Mock
    private AddProjectMemberUseCase addProjectMemberUseCase;
    @Mock
    private RemoveProjectMemberUseCase removeProjectMemberUseCase;

    @BeforeEach
    void setUp() {
        ProjectMemberController controller = new ProjectMemberController(
                getProjectMembersUseCase,
                addProjectMemberUseCase,
                removeProjectMemberUseCase
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new ProjectExceptionHandler(),
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/members - Tra ve 200 va danh sach thanh vien")
    void testGetMembers_Success() throws Exception {
        List<ProjectMemberResult> members = List.of(
                new ProjectMemberResult(50L, "EMP-050", "Manager Name", "pm@hrm.com", 100L, "Engineering", ProjectMemberRole.PROJECT_MANAGER, "ACTIVE"),
                new ProjectMemberResult(60L, "EMP-060", "Member Name", "member@hrm.com", 100L, "Engineering", ProjectMemberRole.MEMBER, "ACTIVE")
        );

        when(getProjectMembersUseCase.getProjectMembers(PROJECT_ID)).thenReturn(members);

        mockMvc.perform(get("/api/v1/projects/{projectId}/members", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].roleInProject", is("PROJECT_MANAGER")))
                .andExpect(jsonPath("$.data[1].roleInProject", is("MEMBER")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/members - Them thanh vien thanh cong tra ve 201")
    void testAddMember_Success() throws Exception {
        ProjectMemberResult result = new ProjectMemberResult(
                EMPLOYEE_ID, "EMP-050", "Member Name", "member@hrm.com", 100L, "Engineering", ProjectMemberRole.MEMBER, "ACTIVE"
        );

        when(addProjectMemberUseCase.addProjectMember(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/projects/{projectId}/members", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\": 50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.employeeId", is(50)))
                .andExpect(jsonPath("$.data.roleInProject", is("MEMBER")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/members - Thieu employeeId tra ve 400")
    void testAddMember_MissingEmployeeId() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/members", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/members - Duplicate thanh vien tra ve 409")
    void testAddMember_Duplicate() throws Exception {
        when(addProjectMemberUseCase.addProjectMember(any()))
                .thenThrow(new DuplicateProjectMemberException(EMPLOYEE_ID, PROJECT_ID));

        mockMvc.perform(post("/api/v1/projects/{projectId}/members", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\": 50}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/members - Nhan vien khong ton tai tra ve 404")
    void testAddMember_EmployeeNotFound() throws Exception {
        when(addProjectMemberUseCase.addProjectMember(any()))
                .thenThrow(new EmployeeNotFoundException("Không tìm thấy nhân viên"));

        mockMvc.perform(post("/api/v1/projects/{projectId}/members", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\": 999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/members/{employeeId} - Xoa thanh cong tra ve 200")
    void testRemoveMember_Success() throws Exception {
        doNothing().when(removeProjectMemberUseCase).removeProjectMember(any());

        mockMvc.perform(delete("/api/v1/projects/{projectId}/members/{employeeId}", PROJECT_ID, EMPLOYEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/members/{employeeId} - Thanh vien khong thuoc du an tra ve 404")
    void testRemoveMember_NotFound() throws Exception {
        doThrow(new ProjectMemberNotFoundException(EMPLOYEE_ID, PROJECT_ID))
                .when(removeProjectMemberUseCase).removeProjectMember(any());

        mockMvc.perform(delete("/api/v1/projects/{projectId}/members/{employeeId}", PROJECT_ID, EMPLOYEE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/members/{employeeId} - Con task hoat dong tra ve 400")
    void testRemoveMember_HasActiveTasks() throws Exception {
        doThrow(new MemberHasActiveTasksException(EMPLOYEE_ID, PROJECT_ID))
                .when(removeProjectMemberUseCase).removeProjectMember(any());

        mockMvc.perform(delete("/api/v1/projects/{projectId}/members/{employeeId}", PROJECT_ID, EMPLOYEE_ID))
                .andExpect(status().isBadRequest());
    }
}
