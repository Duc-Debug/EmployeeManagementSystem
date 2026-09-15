package com.hrm.employeemanagement.application.port.outbound.scenario;

import java.util.List;
import java.util.Optional;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;

public interface LoadResourceScenarioPort {
    Optional<ResourceScenario> findById(Long id);
    Optional<ResourceScenario> findByCode(String code);
    boolean existsByCode(String code);
    List<ResourceScenario> findAllByOrgUnitIds(List<Long> orgUnitIds);
    List<ResourceScenario> findAll();
}
