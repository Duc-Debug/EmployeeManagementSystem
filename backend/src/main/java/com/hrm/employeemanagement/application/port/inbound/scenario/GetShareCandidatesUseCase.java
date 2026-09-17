package com.hrm.employeemanagement.application.port.inbound.scenario;

import java.util.List;
import com.hrm.employeemanagement.application.dto.scenario.ShareCandidateResult;

public interface GetShareCandidatesUseCase {
    List<ShareCandidateResult> getShareCandidates(Long scenarioId, String query);
}
