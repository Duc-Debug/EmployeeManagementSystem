package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException;

public class ScenarioDemand {
    private Long id;
    private Long scenarioId;
    private String demandName;
    private Integer headcount;
    private Integer startYear;
    private Integer startWeek;
    private Integer endYear;
    private Integer endWeek;
    private BigDecimal hoursPerWeekPerPerson;
    private String skillRequirement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScenarioDemand(
            Long id,
            Long scenarioId,
            String demandName,
            Integer headcount,
            Integer startYear,
            Integer startWeek,
            Integer endYear,
            Integer endWeek,
            BigDecimal hoursPerWeekPerPerson,
            String skillRequirement,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.demandName = demandName;
        this.headcount = headcount;
        this.startYear = startYear;
        this.startWeek = startWeek;
        this.endYear = endYear;
        this.endWeek = endWeek;
        this.hoursPerWeekPerPerson = hoursPerWeekPerPerson;
        this.skillRequirement = skillRequirement;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        validate();
    }

    public static ScenarioDemand create(
            Long scenarioId,
            String demandName,
            Integer headcount,
            Integer startYear,
            Integer startWeek,
            Integer endYear,
            Integer endWeek,
            BigDecimal hoursPerWeekPerPerson,
            String skillRequirement
    ) {
        return new ScenarioDemand(
                null,
                scenarioId,
                demandName,
                headcount,
                startYear,
                startWeek,
                endYear,
                endWeek,
                hoursPerWeekPerPerson,
                skillRequirement,
                LocalDateTime.now(),
                null
        );
    }

    public static ScenarioDemand create(
            Long scenarioId,
            String demandName,
            Integer headcount,
            YearWeek demandStart,
            YearWeek demandEnd,
            BigDecimal hoursPerWeekPerPerson,
            String skillRequirement
    ) {
        Objects.requireNonNull(demandStart, "demandStart không được null");
        Objects.requireNonNull(demandEnd, "demandEnd không được null");
        return create(
                scenarioId,
                demandName,
                headcount,
                demandStart.year(),
                demandStart.weekNumber(),
                demandEnd.year(),
                demandEnd.weekNumber(),
                hoursPerWeekPerPerson,
                skillRequirement
        );
    }

    public void update(
            String demandName,
            Integer headcount,
            Integer startYear,
            Integer startWeek,
            Integer endYear,
            Integer endWeek,
            BigDecimal hoursPerWeekPerPerson,
            String skillRequirement
    ) {
        this.demandName = demandName;
        this.headcount = headcount;
        this.startYear = startYear;
        this.startWeek = startWeek;
        this.endYear = endYear;
        this.endWeek = endWeek;
        this.hoursPerWeekPerPerson = hoursPerWeekPerPerson;
        this.skillRequirement = skillRequirement;
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    public void validate() {
        if (demandName == null || demandName.trim().isEmpty()) {
            throw new InvalidScenarioDemandException("Tên nhu cầu không được để trống");
        }
        if (headcount == null || headcount <= 0) {
            throw new InvalidScenarioDemandException("Số lượng nhân sự cần phải lớn hơn 0");
        }
        if (hoursPerWeekPerPerson == null || hoursPerWeekPerPerson.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidScenarioDemandException("Số giờ/tuần/người phải lớn hơn 0");
        }
        if (startYear == null || startWeek == null || endYear == null || endWeek == null) {
            throw new InvalidScenarioDemandException("Năm và tuần bắt đầu/kết thúc không được để trống");
        }
        if (startWeek < 1 || startWeek > 53 || endWeek < 1 || endWeek > 53) {
            throw new InvalidScenarioDemandException("Tuần bắt đầu và tuần kết thúc phải nằm trong khoảng từ 1 đến 53");
        }
        YearWeek start = YearWeek.of(startYear, startWeek);
        YearWeek end = YearWeek.of(endYear, endWeek);
        if (start.isAfter(end)) {
            throw new InvalidScenarioDemandException(
                    "Thời điểm bắt đầu nhu cầu (" + startYear + "-W" + startWeek + ") không được sau thời điểm kết thúc (" + endYear + "-W" + endWeek + ")"
            );
        }
    }

    public BigDecimal getTotalHoursPerWeek() {
        if (headcount == null || hoursPerWeekPerPerson == null) {
            return BigDecimal.ZERO;
        }
        return hoursPerWeekPerPerson.multiply(BigDecimal.valueOf(headcount));
    }

    public boolean isActiveInWeek(YearWeek week) {
        if (week == null) return false;
        YearWeek start = getDemandStart();
        YearWeek end = getDemandEnd();
        return !week.isBefore(start) && !week.isAfter(end);
    }

    public YearWeek getDemandStart() {
        return (startYear != null && startWeek != null) ? YearWeek.of(startYear, startWeek) : null;
    }

    public YearWeek getDemandEnd() {
        return (endYear != null && endWeek != null) ? YearWeek.of(endYear, endWeek) : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getDemandName() { return demandName; }
    public Integer getHeadcount() { return headcount; }
    public Integer getStartYear() { return startYear; }
    public Integer getStartWeek() { return startWeek; }
    public Integer getEndYear() { return endYear; }
    public Integer getEndWeek() { return endWeek; }
    public Integer getWeekStart() { return startWeek; }
    public Integer getWeekEnd() { return endWeek; }
    public BigDecimal getHoursPerWeekPerPerson() { return hoursPerWeekPerPerson; }
    public String getSkillRequirement() { return skillRequirement; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
