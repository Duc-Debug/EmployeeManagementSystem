package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.skill.*;
import com.hrm.employeemanagement.application.port.inbound.skill.*;
import com.hrm.employeemanagement.domain.exception.skill.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillController Web Inbound Adapter Tests")
class SkillControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateSkillUseCase createSkillUseCase;
    @Mock
    private UpdateSkillUseCase updateSkillUseCase;
    @Mock
    private MergeSkillUseCase mergeSkillUseCase;
    @Mock
    private DeactivateSkillUseCase deactivateSkillUseCase;
    @Mock
    private GetSkillListUseCase getSkillListUseCase;
    @Mock
    private GetSkillGroupListUseCase getSkillGroupListUseCase;
    @Mock
    private CreateSkillGroupUseCase createSkillGroupUseCase;
    @Mock
    private UpdateSkillGroupUseCase updateSkillGroupUseCase;
    @Mock
    private DeactivateSkillGroupUseCase deactivateSkillGroupUseCase;

    @InjectMocks
    private SkillController skillController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(skillController)
                .setControllerAdvice(new SkillExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/skills trả về 201 Created khi request hợp lệ")
    void shouldReturn201CreatedWhenCreateSkillIsValid() throws Exception {
        String requestJson = "{\"groupId\":1,\"name\":\"Java\",\"description\":\"Lập trình Java\"}";

        SkillResult mockResult = new SkillResult(
                1L, 1L, "Backend", "Java", "Lập trình Java", "ACTIVE", null, LocalDateTime.now(), null
        );

        when(createSkillUseCase.execute(any(CreateSkillCommand.class))).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/skills/merge trả về 200 OK khi gộp thành công")
    void shouldReturn200OkWhenMergeSkillsIsValid() throws Exception {
        String requestJson = "{\"targetSkillId\":1,\"sourceSkillIds\":[2,3]}";

        SkillResult mockResult = new SkillResult(
                1L, 1L, "Backend", "Java", "Lập trình Java", "ACTIVE", null, LocalDateTime.now(), null
        );

        when(mergeSkillUseCase.execute(any(MergeSkillCommand.class))).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/skills/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    @DisplayName("POST /api/v1/skills/merge trả về 400 Bad Request khi gộp không hợp lệ")
    void shouldReturn400BadRequestWhenMergeFailsDomainRules() throws Exception {
        String requestJson = "{\"targetSkillId\":1,\"sourceSkillIds\":[2]}";

        when(mergeSkillUseCase.execute(any(MergeSkillCommand.class)))
                .thenThrow(new InvalidSkillMergeException("Kỹ năng đích phải ở trạng thái ACTIVE."));

        mockMvc.perform(post("/api/v1/skills/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SKILL_MERGE"))
                .andExpect(jsonPath("$.message").value("Kỹ năng đích phải ở trạng thái ACTIVE."));
    }

    @Test
    @DisplayName("GET /api/v1/skills trả về danh sách kỹ năng")
    void shouldReturnSkillList() throws Exception {
        SkillResult item = new SkillResult(1L, 1L, "Backend", "Java", "Desc", "ACTIVE", null, LocalDateTime.now(), null);
        when(getSkillListUseCase.execute(null, null, null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Java"));
    }
}
