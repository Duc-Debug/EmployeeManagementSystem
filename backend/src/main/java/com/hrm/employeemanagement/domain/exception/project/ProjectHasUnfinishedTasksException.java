package com.hrm.employeemanagement.domain.exception.project;

import java.util.Collections;
import java.util.List;
import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectHasUnfinishedTasksException extends DomainException {
    private final List<String> unfinishedTaskCodes;

    public ProjectHasUnfinishedTasksException(String message, List<String> unfinishedTaskCodes) {
        super(message);
        this.unfinishedTaskCodes = unfinishedTaskCodes != null ? List.copyOf(unfinishedTaskCodes)
                : Collections.emptyList();
    }

    public ProjectHasUnfinishedTasksException(String message) {
        this(message, Collections.emptyList());
    }

    public List<String> getUnfinishedTaskCodes() {
        return unfinishedTaskCodes;
    }
}