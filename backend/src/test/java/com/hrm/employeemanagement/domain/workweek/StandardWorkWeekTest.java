package com.hrm.employeemanagement.domain.workweek;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.workweek.InvalidStandardWorkWeekException;

class StandardWorkWeekTest {

    @Nested
    @DisplayName("Default Config and Invariant Tests")
    class InvariantTests {

        @Test
        @DisplayName("Tạo cấu hình mặc định toàn công ty thành công với 40h/tuần")
        void createDefaultCompany_success() {
            StandardWorkWeekConfig config = StandardWorkWeekConfig.createDefaultCompany(1L);

            assertThat(config.getScope().isCompany()).isTrue();
            assertThat(config.getCapacityUnit()).isEqualTo(CapacityUnit.HOURS);
            assertThat(config.getWeekStartDay()).isEqualTo(WeekStartDay.MONDAY);
            assertThat(config.getStandardHoursPerDay()).isEqualByComparingTo("8.00");
            assertThat(config.getStandardHoursPerWeek()).isEqualByComparingTo("40.00");
            assertThat(config.getDays()).hasSize(7);

            long workingDays = config.getDays().stream().filter(StandardWorkWeekDay::isWorkingDay).count();
            assertThat(workingDays).isEqualTo(5);
        }

        @Test
        @DisplayName("Ném ngoại lệ khi thiếu ngày hoặc ngày null")
        void validateDays_missingDays_throwsException() {
            List<StandardWorkWeekDay> days = List.of(
                    StandardWorkWeekDay.working(DayOfWeek.MONDAY, BigDecimal.valueOf(8)),
                    StandardWorkWeekDay.working(DayOfWeek.TUESDAY, BigDecimal.valueOf(8))
            );

            assertThatThrownBy(() -> new StandardWorkWeekConfig(
                    1L, WorkWeekScope.company(), CapacityUnit.HOURS, WeekStartDay.MONDAY,
                    BigDecimal.valueOf(8), days, 1L, null, null, null, 0L))
                    .isInstanceOf(InvalidStandardWorkWeekException.class)
                    .hasMessageContaining("đầy đủ 7 ngày");
        }

        @Test
        @DisplayName("Ném ngoại lệ khi trùng lặp ngày")
        void validateDays_duplicateDay_throwsException() {
            List<StandardWorkWeekDay> days = List.of(
                    StandardWorkWeekDay.working(DayOfWeek.MONDAY, BigDecimal.valueOf(8)),
                    StandardWorkWeekDay.working(DayOfWeek.MONDAY, BigDecimal.valueOf(8)),
                    StandardWorkWeekDay.working(DayOfWeek.WEDNESDAY, BigDecimal.valueOf(8)),
                    StandardWorkWeekDay.working(DayOfWeek.THURSDAY, BigDecimal.valueOf(8)),
                    StandardWorkWeekDay.working(DayOfWeek.FRIDAY, BigDecimal.valueOf(8)),
                    StandardWorkWeekDay.nonWorking(DayOfWeek.SATURDAY),
                    StandardWorkWeekDay.nonWorking(DayOfWeek.SUNDAY)
            );

            assertThatThrownBy(() -> new StandardWorkWeekConfig(
                    1L, WorkWeekScope.company(), CapacityUnit.HOURS, WeekStartDay.MONDAY,
                    BigDecimal.valueOf(8), days, 1L, null, null, null, 0L))
                    .isInstanceOf(InvalidStandardWorkWeekException.class)
                    .hasMessageContaining("Trùng lặp cấu hình");
        }

        @Test
        @DisplayName("Ném ngoại lệ khi tuần không có ngày làm việc nào")
        void validateDays_noWorkingDay_throwsException() {
            List<StandardWorkWeekDay> days = new ArrayList<>();
            for (DayOfWeek dow : DayOfWeek.values()) {
                days.add(StandardWorkWeekDay.nonWorking(dow));
            }

            assertThatThrownBy(() -> new StandardWorkWeekConfig(
                    1L, WorkWeekScope.company(), CapacityUnit.HOURS, WeekStartDay.MONDAY,
                    BigDecimal.valueOf(8), days, 1L, null, null, null, 0L))
                    .isInstanceOf(InvalidStandardWorkWeekException.class)
                    .hasMessageContaining("ít nhất một ngày làm việc");
        }

        @Test
        @DisplayName("Ném ngoại lệ khi số giờ làm việc ngày vượt quá 12h")
        void validateHours_exceedMax_throwsException() {
            assertThatThrownBy(() -> StandardWorkWeekDay.working(DayOfWeek.MONDAY, BigDecimal.valueOf(14.0)))
                    .isInstanceOf(InvalidStandardWorkWeekException.class)
                    .hasMessageContaining("từ 0.5 đến 12.0 giờ");
        }

        @Test
        @DisplayName("Ngày nghỉ (non-working) tự động có workingHours = 0")
        void nonWorkingDay_hasZeroHours() {
            StandardWorkWeekDay nonWork = new StandardWorkWeekDay(DayOfWeek.SUNDAY, false, BigDecimal.valueOf(8.0));
            assertThat(nonWork.getWorkingHours()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Capacity Conversion Tests")
    class ConversionTests {

        @Test
        @DisplayName("Quy đổi chuẩn 40h/tuần và 8h/ngày: 20h = 2.5 ngày = 0.5 FTE")
        void convertCapacity_fromHours() {
            BigDecimal hoursPerDay = BigDecimal.valueOf(8.0);
            BigDecimal hoursPerWeek = BigDecimal.valueOf(40.0);

            BigDecimal days = StandardWorkWeekPolicy.convertCapacity(
                    BigDecimal.valueOf(20.0), CapacityUnit.HOURS, CapacityUnit.DAYS, hoursPerDay, hoursPerWeek);
            assertThat(days).isEqualByComparingTo("2.50");

            BigDecimal fte = StandardWorkWeekPolicy.convertCapacity(
                    BigDecimal.valueOf(20.0), CapacityUnit.HOURS, CapacityUnit.FTE, hoursPerDay, hoursPerWeek);
            assertThat(fte).isEqualByComparingTo("0.50");
        }

        @Test
        @DisplayName("Quy đổi từ 1.0 FTE sang Hours trả về 40.0h")
        void convertCapacity_fromFte() {
            BigDecimal hoursPerDay = BigDecimal.valueOf(8.0);
            BigDecimal hoursPerWeek = BigDecimal.valueOf(40.0);

            BigDecimal hours = StandardWorkWeekPolicy.convertCapacity(
                    BigDecimal.valueOf(1.0), CapacityUnit.FTE, CapacityUnit.HOURS, hoursPerDay, hoursPerWeek);
            assertThat(hours).isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("Quy đổi từ 3.0 Ngày sang Hours trả về 24.0h")
        void convertCapacity_fromDays() {
            BigDecimal hoursPerDay = BigDecimal.valueOf(8.0);
            BigDecimal hoursPerWeek = BigDecimal.valueOf(40.0);

            BigDecimal hours = StandardWorkWeekPolicy.convertCapacity(
                    BigDecimal.valueOf(3.0), CapacityUnit.DAYS, CapacityUnit.HOURS, hoursPerDay, hoursPerWeek);
            assertThat(hours).isEqualByComparingTo("24.00");
        }
    }
}

