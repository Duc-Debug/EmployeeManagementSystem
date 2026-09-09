package com.hrm.employeemanagement.domain.project;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.hrm.employeemanagement.domain.orgunit.OrgUnit;

import java.util.concurrent.ThreadLocalRandom;

public final class ProjectCodeGenerator {
    private ProjectCodeGenerator() {

    }

    public static String generate(OrgUnit orgUnit) {
        String unitCode = (orgUnit != null && orgUnit.getUnitCode() != null && !orgUnit.getUnitCode().isBlank())
                ? orgUnit.getUnitCode().trim().toUpperCase()
                : "GEN";

        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomHex = String.format("%06X", ThreadLocalRandom.current().nextInt(0x1000000));
        return String.format("PRJ-%s-%s-%s", unitCode, datePart, randomHex);
    }
}
