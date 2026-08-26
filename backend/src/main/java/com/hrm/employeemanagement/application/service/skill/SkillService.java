package com.hrm.employeemanagement.application.service.skill;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.skill.CreateSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.CreateSkillGroupCommand;
import com.hrm.employeemanagement.application.dto.skill.DeactivateSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.DeactivateSkillGroupCommand;
import com.hrm.employeemanagement.application.dto.skill.MergeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.SkillGroupResult;
import com.hrm.employeemanagement.application.dto.skill.SkillResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.UpdateSkillGroupCommand;
import com.hrm.employeemanagement.application.port.inbound.skill.CreateSkillGroupUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.CreateSkillUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.DeactivateSkillGroupUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.DeactivateSkillUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.GetSkillGroupListUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.GetSkillListUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.MergeSkillUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.UpdateSkillGroupUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.UpdateSkillUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillGroupPort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillPort;
import com.hrm.employeemanagement.application.port.outbound.skill.SaveSkillGroupPort;
import com.hrm.employeemanagement.application.port.outbound.skill.SaveSkillPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.skill.DuplicateSkillNameException;
import com.hrm.employeemanagement.domain.exception.skill.InvalidSkillMergeException;
import com.hrm.employeemanagement.domain.exception.skill.SkillGroupNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.SkillNotFoundException;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillGroup;
import com.hrm.employeemanagement.domain.skill.SkillGroupId;
import com.hrm.employeemanagement.domain.skill.SkillId;
import com.hrm.employeemanagement.domain.skill.SkillStatus;

public class SkillService implements
        CreateSkillUseCase,
        UpdateSkillUseCase,
        MergeSkillUseCase,
        DeactivateSkillUseCase,
        GetSkillListUseCase,
        GetSkillGroupListUseCase,
        CreateSkillGroupUseCase,
        UpdateSkillGroupUseCase,
        DeactivateSkillGroupUseCase {

    private final LoadSkillPort loadSkillPort;
    private final SaveSkillPort saveSkillPort;
    private final LoadSkillGroupPort loadSkillGroupPort;
    private final SaveSkillGroupPort saveSkillGroupPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final AuthorizationService authorizationService;
    private final CurrentUserPort currentUserPort;

    public SkillService(LoadSkillPort loadSkillPort,
                        SaveSkillPort saveSkillPort,
                        LoadSkillGroupPort loadSkillGroupPort,
                        SaveSkillGroupPort saveSkillGroupPort,
                        SaveAuditLogPort saveAuditLogPort,
                        AuthorizationService authorizationService,
                        CurrentUserPort currentUserPort) {
        this.loadSkillPort = Objects.requireNonNull(loadSkillPort, "LoadSkillPort must not be null");
        this.saveSkillPort = Objects.requireNonNull(saveSkillPort, "SaveSkillPort must not be null");
        this.loadSkillGroupPort = Objects.requireNonNull(loadSkillGroupPort, "LoadSkillGroupPort must not be null");
        this.saveSkillGroupPort = Objects.requireNonNull(saveSkillGroupPort, "SaveSkillGroupPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.currentUserPort = currentUserPort;
    }

    @Override
    public SkillResult execute(CreateSkillCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.SKILL_CREATE);

        if (!loadSkillGroupPort.existsById(new SkillGroupId(command.groupId()))) {
            throw new SkillGroupNotFoundException("Nhóm kỹ năng với ID " + command.groupId() + " không tồn tại.");
        }

        if (loadSkillPort.existsByNameIgnoreCase(command.name())) {
            throw new DuplicateSkillNameException("Tên kỹ năng '" + command.name() + "' đã tồn tại trong hệ thống.");
        }

        Skill newSkill = new Skill(
                null,
                command.groupId(),
                command.name(),
                command.description(),
                SkillStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                null
        );

        Skill savedSkill = saveSkillPort.save(newSkill);

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SKILL_CREATED",
                "skills",
                savedSkill.getId().value(),
                null,
                "name=" + savedSkill.getName() + ";groupId=" + savedSkill.getGroupId()
        ));

        return toSkillResult(savedSkill);
    }

    @Override
    public SkillResult execute(UpdateSkillCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.SKILL_UPDATE);

        Skill skill = loadSkillPort.findById(new SkillId(command.id()))
                .orElseThrow(() -> new SkillNotFoundException("Không tìm thấy kỹ năng với ID: " + command.id()));

        if (!loadSkillGroupPort.existsById(new SkillGroupId(command.groupId()))) {
            throw new SkillGroupNotFoundException("Nhóm kỹ năng với ID " + command.groupId() + " không tồn tại.");
        }

        if (loadSkillPort.existsByNameIgnoreCaseAndIdNot(command.name(), command.id())) {
            throw new DuplicateSkillNameException("Tên kỹ năng '" + command.name() + "' đã tồn tại trong hệ thống.");
        }

        String oldValue = "name=" + skill.getName() + ";groupId=" + skill.getGroupId() + ";description=" + skill.getDescription();

        skill.updateInfo(command.name(), command.groupId(), command.description());
        Skill savedSkill = saveSkillPort.save(skill);

        String newValue = "name=" + savedSkill.getName() + ";groupId=" + savedSkill.getGroupId() + ";description=" + savedSkill.getDescription();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SKILL_UPDATED",
                "skills",
                savedSkill.getId().value(),
                oldValue,
                newValue
        ));

        return toSkillResult(savedSkill);
    }

    @Override
    public SkillResult execute(MergeSkillCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.SKILL_MERGE);

        Skill targetSkill = loadSkillPort.findById(new SkillId(command.targetSkillId()))
                .orElseThrow(() -> new SkillNotFoundException("Không tìm thấy kỹ năng đích với ID: " + command.targetSkillId()));

        if (targetSkill.getStatus() != SkillStatus.ACTIVE) {
            throw new InvalidSkillMergeException("Kỹ năng đích phải ở trạng thái ACTIVE.");
        }

        List<Skill> sourceSkills = loadSkillPort.findAllByIdIn(command.sourceSkillIds());
        if (sourceSkills.size() != command.sourceSkillIds().size()) {
            throw new SkillNotFoundException("Một hoặc nhiều kỹ năng nguồn không tồn tại trong hệ thống.");
        }

        int affectedEmployeesCount = 0;

        for (Skill sourceSkill : sourceSkills) {
            sourceSkill.mergeInto(targetSkill.getId());

            List<Long> employeeIds = loadSkillPort.findEmployeeIdsWithSkill(sourceSkill.getId().value());

            for (Long empId : employeeIds) {
                affectedEmployeesCount++;
                boolean alreadyHasTarget = loadSkillPort.hasEmployeeSkill(empId, targetSkill.getId().value());

                if (alreadyHasTarget) {
                    saveSkillPort.removeEmployeeSkill(empId, sourceSkill.getId().value());
                } else {
                    saveSkillPort.reassignEmployeeSkills(sourceSkill.getId().value(), targetSkill.getId().value());
                }
            }

            saveSkillPort.save(sourceSkill);
        }

        String oldValue = "sourceSkillIds=" + command.sourceSkillIds();
        String newValue = "targetSkillId=" + targetSkill.getId().value()
                + ";status=MERGED;affectedEmployees=" + affectedEmployeesCount;

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SKILL_MERGED",
                "skills",
                targetSkill.getId().value(),
                oldValue,
                newValue
        ));

        return toSkillResult(targetSkill);
    }

    @Override
    public SkillResult execute(DeactivateSkillCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.SKILL_DEACTIVATE);

        Skill skill = loadSkillPort.findById(new SkillId(command.id()))
                .orElseThrow(() -> new SkillNotFoundException("Không tìm thấy kỹ năng với ID: " + command.id()));

        String oldValue = "status=" + skill.getStatus();

        skill.deactivate();
        Skill savedSkill = saveSkillPort.save(skill);

        String newValue = "status=" + savedSkill.getStatus();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SKILL_DEACTIVATED",
                "skills",
                savedSkill.getId().value(),
                oldValue,
                newValue
        ));

        return toSkillResult(savedSkill);
    }

    @Override
    public List<SkillResult> execute(Long groupId, String status, String keyword) {
        authorizationService.require(PermissionCode.SKILL_READ);
        List<Skill> skills = loadSkillPort.findAll(groupId, status, keyword);
        Map<Long, String> groupNameMap = loadSkillGroupPort.findAll().stream()
                .collect(Collectors.toMap(g -> g.getId().value(), SkillGroup::getName, (a, b) -> a));

        return skills.stream()
                .map(s -> toSkillResult(s, groupNameMap.get(s.getGroupId())))
                .toList();
    }

    @Override
    public List<SkillGroupResult> execute() {
        authorizationService.require(PermissionCode.SKILL_READ);
        return loadSkillGroupPort.findAll().stream()
                .map(this::toSkillGroupResult)
                .toList();
    }

    @Override
    public SkillGroupResult execute(CreateSkillGroupCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.SKILL_CREATE);

        if (loadSkillGroupPort.existsGroupByNameIgnoreCase(command.name())) {
            throw new DuplicateSkillNameException("Tên nhóm kỹ năng '" + command.name() + "' đã tồn tại.");
        }

        SkillGroup newGroup = new SkillGroup(
                null,
                command.name(),
                command.description(),
                SkillStatus.ACTIVE,
                LocalDateTime.now(),
                null
        );

        SkillGroup savedGroup = saveSkillGroupPort.save(newGroup);

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SKILL_GROUP_CREATED",
                "skill_groups",
                savedGroup.getId().value(),
                null,
                "name=" + savedGroup.getName()
        ));

        return toSkillGroupResult(savedGroup);
    }

    @Override
    public SkillGroupResult execute(UpdateSkillGroupCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.SKILL_UPDATE);

        SkillGroup group = loadSkillGroupPort.findById(new SkillGroupId(command.id()))
                .orElseThrow(() -> new SkillGroupNotFoundException("Không tìm thấy nhóm kỹ năng với ID: " + command.id()));

        if (loadSkillGroupPort.existsGroupByNameIgnoreCaseAndIdNot(command.name(), command.id())) {
            throw new DuplicateSkillNameException("Tên nhóm kỹ năng '" + command.name() + "' đã tồn tại.");
        }

        String oldValue = "name=" + group.getName() + ";description=" + group.getDescription();
        group.updateInfo(command.name(), command.description());
        SkillGroup savedGroup = saveSkillGroupPort.save(group);
        String newValue = "name=" + savedGroup.getName() + ";description=" + savedGroup.getDescription();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SKILL_GROUP_UPDATED",
                "skill_groups",
                savedGroup.getId().value(),
                oldValue,
                newValue
        ));

        return toSkillGroupResult(savedGroup);
    }

    @Override
    public SkillGroupResult execute(DeactivateSkillGroupCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.SKILL_DEACTIVATE);

        SkillGroup group = loadSkillGroupPort.findById(new SkillGroupId(command.id()))
                .orElseThrow(() -> new SkillGroupNotFoundException("Không tìm thấy nhóm kỹ năng với ID: " + command.id()));

        long activeSkillCount = loadSkillPort.countByGroupIdAndStatus(command.id(), SkillStatus.ACTIVE);
        if (activeSkillCount > 0) {
            throw new IllegalStateException("Không thể vô hiệu hóa nhóm kỹ năng đang chứa " + activeSkillCount + " kỹ năng hoạt động.");
        }

        String oldValue = "status=" + group.getStatus();
        group.deactivate();
        SkillGroup savedGroup = saveSkillGroupPort.save(group);
        String newValue = "status=" + savedGroup.getStatus();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SKILL_GROUP_DEACTIVATED",
                "skill_groups",
                savedGroup.getId().value(),
                oldValue,
                newValue
        ));

        return toSkillGroupResult(savedGroup);
    }

    private SkillResult toSkillResult(Skill s) {
        String groupName = loadSkillGroupPort.findById(new SkillGroupId(s.getGroupId()))
                .map(SkillGroup::getName)
                .orElse(null);
        return toSkillResult(s, groupName);
    }

    private SkillResult toSkillResult(Skill s, String groupName) {
        return new SkillResult(
                s.getId() != null ? s.getId().value() : null,
                s.getGroupId(),
                groupName,
                s.getName(),
                s.getDescription(),
                s.getStatus().name(),
                s.getMergedIntoSkillId() != null ? s.getMergedIntoSkillId().value() : null,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    private SkillGroupResult toSkillGroupResult(SkillGroup g) {
        return new SkillGroupResult(
                g.getId() != null ? g.getId().value() : null,
                g.getName(),
                g.getDescription(),
                g.getStatus().name(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }
}