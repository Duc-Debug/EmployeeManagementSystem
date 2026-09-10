package com.hrm.employeemanagement.application.service.calendar;

import com.hrm.employeemanagement.application.dto.calendar.CompanyWorkingCalendarResult;
import com.hrm.employeemanagement.application.dto.calendar.CreateHolidayCommand;
import com.hrm.employeemanagement.application.dto.calendar.HolidayResult;
import com.hrm.employeemanagement.application.dto.calendar.UpdateHolidayCommand;
import com.hrm.employeemanagement.application.dto.calendar.WorkingCalendarDayDto;
import com.hrm.employeemanagement.application.port.inbound.calendar.CreateHolidayUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.DeleteHolidayUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.GetHolidaysUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.GetWorkingCalendarUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.UpdateHolidayUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.UpdateWorkingCalendarUseCase;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayCommandPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayQueryPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayRecord;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.SaveWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.calendar.WorkingCalendarDay;
import com.hrm.employeemanagement.domain.exception.calendar.DuplicateHolidayException;
import com.hrm.employeemanagement.domain.exception.calendar.HolidayNotFoundException;
import com.hrm.employeemanagement.domain.exception.calendar.InvalidWorkingCalendarException;

import java.util.List;
import java.util.Objects;

public class WorkingCalendarService implements
        GetWorkingCalendarUseCase,
        UpdateWorkingCalendarUseCase,
        GetHolidaysUseCase,
        CreateHolidayUseCase,
        UpdateHolidayUseCase,
        DeleteHolidayUseCase {

    private final AuthorizationService authorizationService;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final SaveWorkingCalendarPort saveWorkingCalendarPort;
    private final HolidayQueryPort holidayQueryPort;
    private final HolidayCommandPort holidayCommandPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public WorkingCalendarService(
            AuthorizationService authorizationService,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            SaveWorkingCalendarPort saveWorkingCalendarPort,
            HolidayQueryPort holidayQueryPort,
            HolidayCommandPort holidayCommandPort,
            SaveAuditLogPort saveAuditLogPort) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadWorkingCalendarPort = Objects.requireNonNull(loadWorkingCalendarPort, "LoadWorkingCalendarPort must not be null");
        this.saveWorkingCalendarPort = Objects.requireNonNull(saveWorkingCalendarPort, "SaveWorkingCalendarPort must not be null");
        this.holidayQueryPort = Objects.requireNonNull(holidayQueryPort, "HolidayQueryPort must not be null");
        this.holidayCommandPort = Objects.requireNonNull(holidayCommandPort, "HolidayCommandPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
    }

    @Override
    public CompanyWorkingCalendarResult getWorkingCalendar() {
        authorizationService.require(PermissionCode.WORKING_CALENDAR_READ);
        CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
        List<WorkingCalendarDayDto> dtos = calendar.getDays().stream()
                .map(d -> new WorkingCalendarDayDto(d.dayOfWeek(), d.isWorkingDay()))
                .toList();
        return new CompanyWorkingCalendarResult(dtos);
    }

    @Override
    public CompanyWorkingCalendarResult updateWorkingCalendar(List<WorkingCalendarDayDto> dayDtos) {
        Long currentUserId = authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE);

        if (dayDtos == null || dayDtos.isEmpty()) {
            throw new InvalidWorkingCalendarException("Danh sách ngày làm việc trong tuần không được để trống");
        }

        CompanyWorkingCalendar currentCalendar = loadWorkingCalendarPort.loadCompanyCalendar();
        String oldValue = currentCalendar.getDays().stream()
                .map(d -> d.dayOfWeek().name() + "=" + d.isWorkingDay())
                .reduce((a, b) -> a + ";" + b).orElse("");

        List<WorkingCalendarDay> domainDays = dayDtos.stream()
                .map(dto -> new WorkingCalendarDay(dto.dayOfWeek(), dto.isWorkingDay()))
                .toList();

        CompanyWorkingCalendar newCalendar = new CompanyWorkingCalendar(domainDays);
        CompanyWorkingCalendar savedCalendar = saveWorkingCalendarPort.saveCompanyCalendar(newCalendar);

        String newValue = savedCalendar.getDays().stream()
                .map(d -> d.dayOfWeek().name() + "=" + d.isWorkingDay())
                .reduce((a, b) -> a + ";" + b).orElse("");

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "UPDATE_WORKING_CALENDAR",
                "working_calendar_configs",
                1L,
                oldValue,
                newValue
        ));

        List<WorkingCalendarDayDto> resultDtos = savedCalendar.getDays().stream()
                .map(d -> new WorkingCalendarDayDto(d.dayOfWeek(), d.isWorkingDay()))
                .toList();
        return new CompanyWorkingCalendarResult(resultDtos);
    }

    @Override
    public List<HolidayResult> getHolidaysByYear(int year) {
        authorizationService.require(PermissionCode.WORKING_CALENDAR_READ);
        return holidayQueryPort.findByYear(year).stream()
                .map(r -> new HolidayResult(r.id(), r.date(), r.name(), r.workingHoursDeducted()))
                .toList();
    }

    @Override
    public HolidayResult createHoliday(CreateHolidayCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE);

        validateHolidayInput(command.holidayDate(), command.name(), command.workingHoursDeducted());

        if (holidayQueryPort.existsByDate(command.holidayDate())) {
            throw new DuplicateHolidayException("Ngày lễ " + command.holidayDate() + " đã tồn tại trong lịch");
        }

        int hours = (command.workingHoursDeducted() != null && command.workingHoursDeducted() > 0)
                ? command.workingHoursDeducted() : 8;

        HolidayRecord saved = holidayCommandPort.create(
                command.holidayDate(),
                command.name().trim(),
                hours
        );

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "CREATE_HOLIDAY",
                "holidays",
                saved.id(),
                null,
                "date=" + saved.date() + ";name=" + saved.name() + ";hours=" + saved.workingHoursDeducted()
        ));

        return new HolidayResult(saved.id(), saved.date(), saved.name(), saved.workingHoursDeducted());
    }

    @Override
    public HolidayResult updateHoliday(UpdateHolidayCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE);

        if (command == null || command.id() == null) {
            throw new HolidayNotFoundException("ID ngày lễ không được null");
        }

        HolidayRecord existing = holidayQueryPort.findById(command.id())
                .orElseThrow(() -> new HolidayNotFoundException("Không tìm thấy ngày lễ với ID: " + command.id()));

        validateHolidayInput(command.holidayDate(), command.name(), command.workingHoursDeducted());

        if (holidayQueryPort.existsByDateAndIdNot(command.holidayDate(), command.id())) {
            throw new DuplicateHolidayException("Ngày lễ " + command.holidayDate() + " đã tồn tại trong lịch");
        }

        int hours = (command.workingHoursDeducted() != null && command.workingHoursDeducted() > 0)
                ? command.workingHoursDeducted() : 8;

        HolidayRecord updated = holidayCommandPort.update(
                command.id(),
                command.holidayDate(),
                command.name().trim(),
                hours
        );

        String oldValue = "date=" + existing.date() + ";name=" + existing.name() + ";hours=" + existing.workingHoursDeducted();
        String newValue = "date=" + updated.date() + ";name=" + updated.name() + ";hours=" + updated.workingHoursDeducted();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "UPDATE_HOLIDAY",
                "holidays",
                updated.id(),
                oldValue,
                newValue
        ));

        return new HolidayResult(updated.id(), updated.date(), updated.name(), updated.workingHoursDeducted());
    }

    @Override
    public void deleteHoliday(Long holidayId) {
        Long currentUserId = authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE);

        if (holidayId == null) {
            throw new HolidayNotFoundException("ID ngày lễ không được null");
        }

        HolidayRecord existing = holidayQueryPort.findById(holidayId)
                .orElseThrow(() -> new HolidayNotFoundException("Không tìm thấy ngày lễ với ID: " + holidayId));

        holidayCommandPort.deleteById(holidayId);

        String oldValue = "date=" + existing.date() + ";name=" + existing.name() + ";hours=" + existing.workingHoursDeducted();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "DELETE_HOLIDAY",
                "holidays",
                holidayId,
                oldValue,
                null
        ));
    }

    private void validateHolidayInput(java.time.LocalDate date, String name, Integer hours) {
        if (date == null) {
            throw new InvalidWorkingCalendarException("Ngày lễ không được để trống");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidWorkingCalendarException("Tên ngày lễ không được để trống");
        }
        if (name.trim().length() > 255) {
            throw new InvalidWorkingCalendarException("Tên ngày lễ không được vượt quá 255 ký tự");
        }
        if (hours != null && (hours <= 0 || hours > 24)) {
            throw new InvalidWorkingCalendarException("Số giờ khấu trừ của ngày lễ phải từ 1 đến 24 giờ");
        }
    }
}
