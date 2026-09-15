package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.template.ProjectRoleStructureItem;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.LoadProjectRoleAllocationStructurePort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.LoadRoleAllocationTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.SaveRoleAllocationTemplatePort;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplate;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplateItem;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template.entity.ProjectRoleAllocationTemplateItemJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template.entity.ProjectRoleAllocationTemplateJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.template.repository.SpringDataProjectRoleAllocationTemplateRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;

@Component
public class RoleAllocationTemplateRepositoryAdapter
        implements SaveRoleAllocationTemplatePort, LoadRoleAllocationTemplatePort, LoadProjectRoleAllocationStructurePort {

    private final SpringDataProjectRoleAllocationTemplateRepository templateRepository;
    private final SpringDataProjectResourceDemandRepository demandRepository;
    private final SpringDataProjectRoleRepository roleRepository;
    private final RoleAllocationTemplatePersistenceMapper mapper;

    public RoleAllocationTemplateRepositoryAdapter(
            SpringDataProjectRoleAllocationTemplateRepository templateRepository,
            SpringDataProjectResourceDemandRepository demandRepository,
            SpringDataProjectRoleRepository roleRepository,
            RoleAllocationTemplatePersistenceMapper mapper
    ) {
        this.templateRepository = templateRepository;
        this.demandRepository = demandRepository;
        this.roleRepository = roleRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ProjectRoleAllocationTemplate save(ProjectRoleAllocationTemplate template) {
        if (template.getId() != null) {
            ProjectRoleAllocationTemplateJpaEntity entity = templateRepository.findById(template.getId())
                    .orElseGet(() -> mapper.toJpaEntity(template));
            entity.setName(template.getName());
            entity.setDescription(template.getDescription());
            entity.setSourceProjectId(template.getSourceProjectId());
            entity.clearItems();
            if (template.getItems() != null) {
                for (ProjectRoleAllocationTemplateItem item : template.getItems()) {
                    entity.addItem(new ProjectRoleAllocationTemplateItemJpaEntity(
                            null,
                            entity,
                            item.getRoleId(),
                            item.getHoursPerWeek(),
                            null
                    ));
                }
            }
            ProjectRoleAllocationTemplateJpaEntity saved = templateRepository.save(entity);
            return mapper.toDomain(saved);
        }

        ProjectRoleAllocationTemplateJpaEntity entity = mapper.toJpaEntity(template);
        ProjectRoleAllocationTemplateJpaEntity saved = templateRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectRoleAllocationTemplate> findById(Long id) {
        return templateRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectRoleAllocationTemplate> findByCode(String code) {
        return templateRepository.findByTemplateCode(code).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return templateRepository.existsByTemplateCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectRoleAllocationTemplate> findAll() {
        return templateRepository.findAllByOrderByIdDesc().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectRoleStructureItem> extractRoleStructureFromProject(Long projectId) {
        List<ProjectResourceDemandJpaEntity> demands = demandRepository.findByProjectId(projectId);
        if (demands.isEmpty()) {
            return List.of();
        }

        // Group demands by roleId
        Map<Long, List<ProjectResourceDemandJpaEntity>> demandsByRole = new LinkedHashMap<>();
        for (ProjectResourceDemandJpaEntity demand : demands) {
            demandsByRole.computeIfAbsent(demand.getRoleId(), k -> new ArrayList<>()).add(demand);
        }

        List<ProjectRoleStructureItem> result = new ArrayList<>();
        for (Map.Entry<Long, List<ProjectResourceDemandJpaEntity>> entry : demandsByRole.entrySet()) {
            Long roleId = entry.getKey();
            List<ProjectResourceDemandJpaEntity> roleDemands = entry.getValue();

            BigDecimal totalHours = roleDemands.stream()
                    .map(ProjectResourceDemandJpaEntity::getRequiredHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal countWeeks = BigDecimal.valueOf(roleDemands.size());
            BigDecimal avgHoursPerWeek = totalHours.divide(countWeeks, 2, RoundingMode.HALF_UP);

            Optional<ProjectRoleJpaEntity> roleOpt = roleRepository.findById(roleId);
            String roleCode = roleOpt.map(ProjectRoleJpaEntity::getCode).orElse("ROLE_" + roleId);
            String roleName = roleOpt.map(ProjectRoleJpaEntity::getName).orElse("Vai trò " + roleId);

            result.add(new ProjectRoleStructureItem(roleId, roleCode, roleName, avgHoursPerWeek));
        }

        return result;
    }
}

