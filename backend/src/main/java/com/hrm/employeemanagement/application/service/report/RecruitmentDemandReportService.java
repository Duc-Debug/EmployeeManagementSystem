package com.hrm.employeemanagement.application.service.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportQuery;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportResult;
import com.hrm.employeemanagement.application.port.inbound.report.GetRecruitmentDemandReportUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.report.LoadRecruitmentDemandReportPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.report.RecruitmentSkillDemand;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class RecruitmentDemandReportService implements GetRecruitmentDemandReportUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadRecruitmentDemandReportPort loadReportPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;

    public RecruitmentDemandReportService(
            AuthorizationService authorizationService,
            LoadRecruitmentDemandReportPort loadReportPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this(authorizationService, null, null, loadReportPort, saveAuditLogPort);
    }

    public RecruitmentDemandReportService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadRecruitmentDemandReportPort loadReportPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = loadUserPort;
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.loadReportPort = Objects.requireNonNull(loadReportPort, "LoadRecruitmentDemandReportPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    @Override
    public RecruitmentDemandReportResult execute(RecruitmentDemandReportQuery query) {
        // [TC-03] Kiểm tra quyền hạn và tự động ghi log từ chối nếu không có quyền
        Long currentUserId = authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ);

        RecruitmentDemandReportQuery effectiveQuery = query != null ? query : new RecruitmentDemandReportQuery(null, null, null, null, null);
        effectiveQuery.validate();

        Long effectiveOrgUnitId = effectiveQuery.orgUnitId();

        // Enforcement of DataScope
        if (loadUserPort != null) {
            User currentUser = loadUserPort.findById(new UserId(currentUserId)).orElse(null);
            if (currentUser != null && currentUser.getDataScope() != null) {
                switch (currentUser.getDataScope()) {
                    case COMPANY -> {
                        effectiveOrgUnitId = effectiveQuery.orgUnitId();
                    }
                    case ORGANIZATION_BRANCH -> {
                        if (currentUser.getScopeOrgUnitId() == null) {
                            saveAuditLogPort.save(AuditLog.create(null, "ACCESS_DENIED", "RECRUITMENT_DEMAND_REPORT", effectiveQuery.orgUnitId()));
                            throw new PermissionDeniedException(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ);
                        }
                        if (effectiveQuery.orgUnitId() != null) {
                            boolean inScope = loadOrgUnitPort != null && loadOrgUnitPort.existsInOrgUnitBranch(effectiveQuery.orgUnitId(), currentUser.getScopeOrgUnitId());
                            if (!inScope) {
                                saveAuditLogPort.save(AuditLog.create(null, "ACCESS_DENIED", "RECRUITMENT_DEMAND_REPORT", effectiveQuery.orgUnitId()));
                                throw new PermissionDeniedException(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ);
                            }
                            effectiveOrgUnitId = effectiveQuery.orgUnitId();
                        } else {
                            effectiveOrgUnitId = currentUser.getScopeOrgUnitId();
                        }
                    }
                    case SELF -> {
                        saveAuditLogPort.save(AuditLog.create(null, "ACCESS_DENIED", "RECRUITMENT_DEMAND_REPORT", effectiveQuery.orgUnitId()));
                        throw new PermissionDeniedException(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ);
                    }
                    default -> {
                        saveAuditLogPort.save(AuditLog.create(null, "ACCESS_DENIED", "RECRUITMENT_DEMAND_REPORT", effectiveQuery.orgUnitId()));
                        throw new PermissionDeniedException(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ);
                    }
                }
            }
        }

        // 1. Tải danh mục kỹ năng chuẩn trong hệ thống
        List<Skill> skills = loadReportPort.loadAllActiveSkills();

        // 2. Tải tổng số giờ nhu cầu theo dự án và năng lực hiện có theo kỹ năng
        Map<Long, BigDecimal> demandHoursMap = loadReportPort.loadProjectDemandHoursGroupedBySkill(
                effectiveQuery.fromYear(), effectiveQuery.fromWeek(),
                effectiveQuery.toYear(), effectiveQuery.toWeek(),
                effectiveOrgUnitId);

        Map<Long, BigDecimal> capacityHoursMap = loadReportPort.loadAvailableCapacityHoursGroupedBySkill(
                effectiveQuery.fromYear(), effectiveQuery.fromWeek(),
                effectiveQuery.toYear(), effectiveQuery.toWeek(),
                effectiveOrgUnitId);

        List<RecruitmentSkillDemand> skillDemands = new ArrayList<>();
        BigDecimal totalDeficitHours = BigDecimal.ZERO;

        // 3. Tính toán số giờ thiếu hụt cho từng kỹ năng
        for (Skill skill : skills) {
            Long skillId = skill.getId();
            BigDecimal demand = demandHoursMap.getOrDefault(skillId, BigDecimal.ZERO);
            BigDecimal capacity = capacityHoursMap.getOrDefault(skillId, BigDecimal.ZERO);

            RecruitmentSkillDemand item = new RecruitmentSkillDemand(
                    skillId,
                    skill.getCode(),
                    skill.getName(),
                    skill.getCategory(),
                    demand,
                    capacity
            );

            skillDemands.add(item);
            if (item.isDeficit()) {
                totalDeficitHours = totalDeficitHours.add(item.getShortfallHours());
            }
        }

        // Sắp xếp các kỹ năng có số giờ thiếu cao nhất lên đầu
        skillDemands.sort(Comparator.comparing(RecruitmentSkillDemand::getShortfallHours).reversed()
                .thenComparing(RecruitmentSkillDemand::getSkillName));

        int deficitCount = (int) skillDemands.stream().filter(RecruitmentSkillDemand::isDeficit).count();

        String timeRangeText = buildTimeRangeText(effectiveQuery);

        // [TC-04] Ghi nhận lịch sử tra cứu/xem báo cáo thành công vào Audit Log
        recordSuccessAuditLog(currentUserId, effectiveQuery, effectiveOrgUnitId, deficitCount, totalDeficitHours);

        return new RecruitmentDemandReportResult(
                skillDemands.size(),
                totalDeficitHours,
                deficitCount,
                skillDemands,
                timeRangeText,
                LocalDateTime.now()
        );
    }

    private String buildTimeRangeText(RecruitmentDemandReportQuery query) {
        if (query.fromYear() != null && query.fromWeek() != null && query.toYear() != null && query.toWeek() != null) {
            return String.format("%d-W%02d đến %d-W%02d", query.fromYear(), query.fromWeek(), query.toYear(), query.toWeek());
        }
        return "Toàn thời gian";
    }

    private void recordSuccessAuditLog(Long userId, RecruitmentDemandReportQuery query, Long effectiveOrgUnitId, int deficitCount, BigDecimal totalDeficitHours) {
        String detail = String.format(
                "Tra cứu Báo cáo Nhu cầu Tuyển dụng: từ %s, phòng ban=%s, tìm thấy %d kỹ năng thiếu tổng cộng %s giờ",
                buildTimeRangeText(query),
                effectiveOrgUnitId != null ? effectiveOrgUnitId : "Tất cả",
                deficitCount,
                totalDeficitHours.toString()
        );

        saveAuditLogPort.save(AuditLog.createChange(
                userId,
                "QUERY",
                "RECRUITMENT_DEMAND_REPORT",
                null,
                null,
                detail
        ));
    }
}
