package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hrm.employeemanagement.application.dto.allocation.ConfirmScheduleViewedResult;
import com.hrm.employeemanagement.application.dto.allocation.MyWeeklyAllocationsResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.ConfirmScheduleViewedUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetMyAllocationsUseCase;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/my-allocations")
public class MyAllocationsController {

    private static final Set<String> ALLOWED_QUERY_PARAMS = Set.of("week_start", "weeks");

    private final GetMyAllocationsUseCase getMyAllocationsUseCase;
    private final ConfirmScheduleViewedUseCase confirmScheduleViewedUseCase;
    private final ClientIpResolver clientIpResolver;

    public MyAllocationsController(
            GetMyAllocationsUseCase getMyAllocationsUseCase,
            ConfirmScheduleViewedUseCase confirmScheduleViewedUseCase,
            ClientIpResolver clientIpResolver) {
        this.getMyAllocationsUseCase = Objects.requireNonNull(getMyAllocationsUseCase, "GetMyAllocationsUseCase must not be null");
        this.confirmScheduleViewedUseCase = Objects.requireNonNull(confirmScheduleViewedUseCase, "ConfirmScheduleViewedUseCase must not be null");
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver, "ClientIpResolver must not be null");
    }

    @GetMapping
    public ResponseEntity<MyAllocationsWebResponse> getMyAllocations(HttpServletRequest request) {
        // BR-01: Strict Query Parameter Allow-list
        Enumeration<String> parameterNames = request.getParameterNames();
        if (parameterNames != null) {
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                if (!ALLOWED_QUERY_PARAMS.contains(paramName)) {
                    throw new InvalidQueryParameterException("Query parameter '" + paramName + "' is not allowed");
                }
            }
        }

        String weekStartStr = request.getParameter("week_start");
        LocalDate weekStartDate = null;
        if (weekStartStr != null && !weekStartStr.isBlank()) {
            try {
                weekStartDate = LocalDate.parse(weekStartStr.trim());
            } catch (DateTimeParseException ex) {
                throw new InvalidWeekFormatException("Sai định dạng ngày: '" + weekStartStr + "'. Định dạng hợp lệ là YYYY-MM-DD");
            }
        }

        String weeksStr = request.getParameter("weeks");
        Integer weeks = null;
        if (weeksStr != null && !weeksStr.isBlank()) {
            try {
                weeks = Integer.parseInt(weeksStr.trim());
            } catch (NumberFormatException ex) {
                throw new InvalidWeeksFormatException("Sai định dạng số tuần: '" + weeksStr + "'. Số tuần phải là số nguyên");
            }
        }

        MyWeeklyAllocationsResult result = getMyAllocationsUseCase.getMyAllocations(weekStartDate, weeks);

        List<MyAllocationsWebResponse.WeekResponse> weekResponses = result.weeks().stream()
                .map(w -> new MyAllocationsWebResponse.WeekResponse(
                        w.weekStartDate(),
                        w.totalHours(),
                        w.confirmationStatus(),
                        w.confirmedAt() != null ? w.confirmedAt().toInstant(ZoneOffset.UTC) : null,
                        w.allocations() != null ? w.allocations().stream()
                                .map(a -> new MyAllocationsWebResponse.AllocationResponse(
                                        a.allocationId(),
                                        a.projectId(),
                                        a.projectName(),
                                        a.projectStatus(),
                                        a.allocatedHours()
                                ))
                                .toList() : Collections.emptyList()
                ))
                .toList();

        return ResponseEntity.ok(new MyAllocationsWebResponse(weekResponses));
    }

    @PostMapping("/{week_start}/confirm-viewed")
    public ResponseEntity<ConfirmScheduleWebResponse> confirmScheduleViewed(
            @PathVariable("week_start") String weekStartStr,
            HttpServletRequest request) {
        LocalDate weekStartDate;
        try {
            weekStartDate = LocalDate.parse(weekStartStr.trim());
        } catch (DateTimeParseException ex) {
            throw new InvalidWeekFormatException("Sai định dạng ngày: '" + weekStartStr + "'. Định dạng hợp lệ là YYYY-MM-DD");
        }

        String clientIp = clientIpResolver.resolveClientIp(request);
        ConfirmScheduleViewedResult result = confirmScheduleViewedUseCase.confirmScheduleViewed(weekStartDate, clientIp);

        ConfirmScheduleWebResponse responseBody = new ConfirmScheduleWebResponse(
                result.weekStartDate(),
                result.confirmedAt() != null ? result.confirmedAt().toInstant(ZoneOffset.UTC) : null,
                result.confirmationStatus(),
                result.alreadyConfirmed(),
                result.previousConfirmationWasStale()
        );

        return ResponseEntity.status(HttpStatus.valueOf(result.httpStatusCode())).body(responseBody);
    }

    public record MyAllocationsWebResponse(
            List<WeekResponse> weeks
    ) {
        public record WeekResponse(
                @JsonProperty("week_start_date")
                LocalDate weekStartDate,
                @JsonProperty("total_hours")
                BigDecimal totalHours,
                @JsonProperty("confirmation_status")
                String confirmationStatus,
                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
                @JsonProperty("confirmed_at")
                Instant confirmedAt,
                List<AllocationResponse> allocations
        ) {}

        public record AllocationResponse(
                @JsonProperty("allocation_id")
                Long allocationId,
                @JsonProperty("project_id")
                Long projectId,
                @JsonProperty("project_name")
                String projectName,
                @JsonProperty("project_status")
                String projectStatus,
                @JsonProperty("allocated_hours")
                BigDecimal allocatedHours
        ) {}
    }

    public record ConfirmScheduleWebResponse(
            @JsonProperty("week_start_date")
            LocalDate weekStartDate,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
            @JsonProperty("confirmed_at")
            Instant confirmedAt,
            @JsonProperty("confirmation_status")
            String confirmationStatus,
            @JsonProperty("already_confirmed")
            boolean alreadyConfirmed,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonProperty("previous_confirmation_was_stale")
            Boolean previousConfirmationWasStale
    ) {}
}