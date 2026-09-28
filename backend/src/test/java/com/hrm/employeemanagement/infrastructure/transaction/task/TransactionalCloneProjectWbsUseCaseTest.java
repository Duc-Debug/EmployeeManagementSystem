package com.hrm.employeemanagement.infrastructure.transaction.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsCommand;
import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsResult;
import com.hrm.employeemanagement.application.port.inbound.task.CloneProjectWbsUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionalCloneProjectWbsUseCase Tests")
class TransactionalCloneProjectWbsUseCaseTest {

    @Mock
    private CloneProjectWbsUseCase delegate;

    private TransactionalCloneProjectWbsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TransactionalCloneProjectWbsUseCase(delegate);
    }

    @Test
    @DisplayName("Ủy quyền cloneWbs chính xác tới delegate use case")
    void testCloneWbs_DelegatesCorrectly() {
        CloneProjectWbsCommand command = new CloneProjectWbsCommand(100L, 200L);
        CloneProjectWbsResult expectedResult = new CloneProjectWbsResult(100L, 200L, 5, 1, List.of());

        when(delegate.cloneWbs(command)).thenReturn(expectedResult);

        CloneProjectWbsResult actualResult = useCase.cloneWbs(command);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(delegate).cloneWbs(command);
    }

    @Test
    @DisplayName("Ngoại lệ phát sinh từ delegate được truyền ra ngoài để kích hoạt Transaction rollback")
    void testCloneWbs_ExceptionPropagated_TriggersRollback() {
        CloneProjectWbsCommand command = new CloneProjectWbsCommand(100L, 200L);
        when(delegate.cloneWbs(command)).thenThrow(new RuntimeException("Database error during cloning"));

        assertThatThrownBy(() -> useCase.cloneWbs(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error during cloning");

        verify(delegate).cloneWbs(command);
    }

    @Test
    @DisplayName("Phương thức cloneWbs được đánh dấu @Transactional để đảm bảo Atomicity (All-or-Nothing)")
    void testCloneWbs_HasTransactionalAnnotation() throws NoSuchMethodException {
        var method = TransactionalCloneProjectWbsUseCase.class.getMethod("cloneWbs", CloneProjectWbsCommand.class);
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertThat(annotation).isNotNull();
    }
}