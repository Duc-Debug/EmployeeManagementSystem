package com.hrm.employeemanagement;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Automated ArchUnit tests enforcing Hexagonal Architecture and Pure Java rules.
 * Runs on every build and CI/CD pipeline to strictly prevent architectural erosion.
 */
@AnalyzeClasses(packages = "com.hrm.employeemanagement", importOptions = {ImportOption.DoNotIncludeTests.class})
public class HexagonalArchitectureTest {

    @ArchTest
    public static final ArchRule domain_must_be_pure_java_and_not_depend_on_frameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.servlet..",
                            "com.hrm.employeemanagement.infrastructure..",
                            "com.hrm.employeemanagement.application.."
                    ).as("Domain layer must be 100% Pure Java and have no dependencies on Frameworks or outer layers");

    @ArchTest
    public static final ArchRule application_must_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.servlet..",
                            "com.hrm.employeemanagement.infrastructure.."
                    ).as("Application layer must be Pure Java and must not depend on Infrastructure");

    @ArchTest
    public static final ArchRule domain_must_not_have_spring_annotations =
            noClasses().that().resideInAPackage("..domain..")
                    .should().beAnnotatedWith("org.springframework.stereotype.Component")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
                    .orShould().beAnnotatedWith("jakarta.persistence.Entity")
                    .orShould().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .as("Domain classes must not have Spring or JPA annotations");

    @ArchTest
    public static final ArchRule application_must_not_have_spring_annotations =
            noClasses().that().resideInAPackage("..application..")
                    .should().beAnnotatedWith("org.springframework.stereotype.Component")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
                    .orShould().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .as("Application services must not have Spring annotations");
}
