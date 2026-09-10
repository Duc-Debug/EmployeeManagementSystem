package com.hrm.employeemanagement.application.service.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateDetailResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateSummaryResult;
import com.hrm.employeemanagement.application.port.outbound.projecttemplate.LoadProjectTemplatePort;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.projecttemplate.ProjectTemplateNotFoundException;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplate;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateId;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTask;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTaskId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.UserId;

class GetProjectTemplatesServiceTest {

    private LoadProjectTemplatePort loadProjectTemplatePort;
    private GetProjectTemplatesService service;

    @BeforeEach
    void setUp() {
        loadProjectTemplatePort = mock(LoadProjectTemplatePort.class);
        service = new GetProjectTemplatesService(loadProjectTemplatePort);
    }

    @Test
    @DisplayName("getActiveTemplates tra ve danh sach template active kem thong ke tong gio va so luong task")
    void getActiveTemplates_Success() {
        ProjectTemplate template = new ProjectTemplate(
                new ProjectTemplateId(1L),
                "TPL-DEV-001",
                "Mẫu phần mềm",
                "Mô tả",
                true,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L
        );

        ProjectTemplateTask cat1 = new ProjectTemplateTask(
                new ProjectTemplateTaskId(10L),
                new ProjectTemplateId(1L),
                null,
                "Khảo sát",
                null,
                TaskType.CATEGORY,
                BigDecimal.ZERO,
                1,
                LocalDateTime.now()
        );

        ProjectTemplateTask task1 = new ProjectTemplateTask(
                new ProjectTemplateTaskId(11L),
                new ProjectTemplateId(1L),
                new ProjectTemplateTaskId(10L),
                "Viết tài liệu",
                null,
                TaskType.TASK,
                new BigDecimal("16.00"),
                1,
                LocalDateTime.now()
        );

        ProjectTemplateTask task2 = new ProjectTemplateTask(
                new ProjectTemplateTaskId(12L),
                new ProjectTemplateId(1L),
                new ProjectTemplateTaskId(10L),
                "Lập trình",
                null,
                TaskType.TASK,
                new BigDecimal("24.00"),
                2,
                LocalDateTime.now()
        );

        when(loadProjectTemplatePort.findAllActive()).thenReturn(List.of(template));
        when(loadProjectTemplatePort.findTasksByTemplateId(template.getId())).thenReturn(List.of(cat1, task1, task2));

        List<ProjectTemplateSummaryResult> results = service.getActiveTemplates();

        assertThat(results).hasSize(1);
        ProjectTemplateSummaryResult result = results.get(0);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.templateCode()).isEqualTo("TPL-DEV-001");
        assertThat(result.totalEstimatedHours()).isEqualByComparingTo("40.00");
        assertThat(result.categoriesCount()).isEqualTo(1);
        assertThat(result.tasksCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getTemplateDetail tra ve chi tiet template kem danh sach tasks")
    void getTemplateDetail_Success() {
        ProjectTemplate template = new ProjectTemplate(
                new ProjectTemplateId(1L),
                "TPL-DEV-001",
                "Mẫu phần mềm",
                "Mô tả",
                true,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L
        );

        ProjectTemplateTask task1 = new ProjectTemplateTask(
                new ProjectTemplateTaskId(11L),
                new ProjectTemplateId(1L),
                null,
                "Viết tài liệu",
                "Mô tả task",
                TaskType.TASK,
                new BigDecimal("16.00"),
                1,
                LocalDateTime.now()
        );

        when(loadProjectTemplatePort.findById(any(ProjectTemplateId.class))).thenReturn(Optional.of(template));
        when(loadProjectTemplatePort.findTasksByTemplateId(any(ProjectTemplateId.class))).thenReturn(List.of(task1));

        ProjectTemplateDetailResult result = service.getTemplateDetail(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Mẫu phần mềm");
        assertThat(result.totalEstimatedHours()).isEqualByComparingTo("16.00");
        assertThat(result.tasks()).hasSize(1);
        assertThat(result.tasks().get(0).name()).isEqualTo("Viết tài liệu");
    }

    @Test
    @DisplayName("getTemplateDetail voi id null nem loi InvalidProjectDataException")
    void getTemplateDetail_NullId_ThrowsException() {
        assertThatThrownBy(() -> service.getTemplateDetail(null))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("không được để trống");
    }

    @Test
    @DisplayName("getTemplateDetail voi id khong ton tai nem loi ProjectTemplateNotFoundException")
    void getTemplateDetail_NotFound_ThrowsException() {
        when(loadProjectTemplatePort.findById(any(ProjectTemplateId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTemplateDetail(999L))
                .isInstanceOf(ProjectTemplateNotFoundException.class);
    }
}
