package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.DeactivateOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.MoveOrgUnitCommand;
import com.hrm.employeemanagement.application.port.inbound.orgunit.DeactivateOrgUnitUseCase;
import com.hrm.employeemanagement.application.port.inbound.orgunit.MoveOrgUnitUseCase;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrgUnitSubTreeIntegrationTest {

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Autowired
    @Qualifier("transactionalMoveOrgUnitUseCase")
    private MoveOrgUnitUseCase moveOrgUnitUseCase;

    @Autowired
    @Qualifier("transactionalDeactivateOrgUnitUseCase")
    private DeactivateOrgUnitUseCase deactivateOrgUnitUseCase;

    private Long rootAId;
    private Long targetXId;
    private Long nodeBId;
    private Long nodeCId;
    private Long nodeDId;

    @BeforeEach
    void setUpHierarchy() {
        // Hierarchy Structure:
        // Root A (level 1, path: /1/)
        // ├── Target X (level 2, path: /1/2/, parent: A)
        // └── Node B (level 2, path: /1/3/, parent: A)
        //     └── Node C (level 3, path: /1/3/4/, parent: B)
        //         └── Node D (level 4, path: /1/3/4/5/, parent: C)

        OrgUnitJpaEntity rootA = orgUnitRepository.save(new OrgUnitJpaEntity(
                null, "ROOT_A", "Tập đoàn A", OrgUnitType.COMPANY,
                null, "/pending/", 1, OrgUnitStatus.ACTIVE, "Root Node",
                LocalDateTime.now(), null
        ));
        rootA.setTreePath("/" + rootA.getId() + "/");
        rootA = orgUnitRepository.save(rootA);
        rootAId = rootA.getId();

        OrgUnitJpaEntity targetX = orgUnitRepository.save(new OrgUnitJpaEntity(
                null, "TARGET_X", "Khối X", OrgUnitType.CENTER,
                rootAId, "/" + rootAId + "/pending/", 2, OrgUnitStatus.ACTIVE, "Target Branch",
                LocalDateTime.now(), null
        ));
        targetX.setManagerId(1L);
        targetX.setTreePath("/" + rootAId + "/" + targetX.getId() + "/");
        targetX = orgUnitRepository.save(targetX);
        targetXId = targetX.getId();

        OrgUnitJpaEntity nodeB = orgUnitRepository.save(new OrgUnitJpaEntity(
                null, "NODE_B", "Khối B", OrgUnitType.CENTER,
                rootAId, "/" + rootAId + "/pending/", 2, OrgUnitStatus.ACTIVE, "Node B",
                LocalDateTime.now(), null
        ));
        nodeB.setManagerId(1L);
        nodeB.setTreePath("/" + rootAId + "/" + nodeB.getId() + "/");
        nodeB = orgUnitRepository.save(nodeB);
        nodeBId = nodeB.getId();

        OrgUnitJpaEntity nodeC = orgUnitRepository.save(new OrgUnitJpaEntity(
                null, "NODE_C", "Phòng C", OrgUnitType.DEPARTMENT,
                nodeBId, nodeB.getTreePath() + "pending/", 3, OrgUnitStatus.ACTIVE, "Node C",
                LocalDateTime.now(), null
        ));
        nodeC.setManagerId(1L);
        nodeC.setTreePath(nodeB.getTreePath() + nodeC.getId() + "/");
        nodeC = orgUnitRepository.save(nodeC);
        nodeCId = nodeC.getId();

        OrgUnitJpaEntity nodeD = orgUnitRepository.save(new OrgUnitJpaEntity(
                null, "NODE_D", "Đội D", OrgUnitType.TEAM,
                nodeCId, nodeC.getTreePath() + "pending/", 4, OrgUnitStatus.ACTIVE, "Node D",
                LocalDateTime.now(), null
        ));
        nodeD.setManagerId(1L);
        nodeD.setTreePath(nodeC.getTreePath() + nodeD.getId() + "/");
        nodeD = orgUnitRepository.save(nodeD);
        nodeDId = nodeD.getId();
    }

    @Test
    @DisplayName("Should successfully move entire multi-level subtree (B -> C -> D) under Target X")
    void shouldMoveMultiLevelSubtreeCorrectly() {
        // Move Node B to become a child of Target X
        MoveOrgUnitCommand command = new MoveOrgUnitCommand(nodeBId, targetXId);
        moveOrgUnitUseCase.execute(command);

        // Fetch fresh entities from DB
        OrgUnitJpaEntity freshB = orgUnitRepository.findById(nodeBId).orElseThrow();
        OrgUnitJpaEntity freshC = orgUnitRepository.findById(nodeCId).orElseThrow();
        OrgUnitJpaEntity freshD = orgUnitRepository.findById(nodeDId).orElseThrow();

        // 1. Assert Node B (moved node)
        String expectedBPath = "/" + rootAId + "/" + targetXId + "/" + nodeBId + "/";
        assertThat(freshB.getParentId()).isEqualTo(targetXId);
        assertThat(freshB.getTreePath()).isEqualTo(expectedBPath);
        assertThat(freshB.getLevel()).isEqualTo(3);

        // 2. Assert Node C (child of B)
        String expectedCPath = expectedBPath + nodeCId + "/";
        assertThat(freshC.getParentId()).isEqualTo(nodeBId);
        assertThat(freshC.getTreePath()).isEqualTo(expectedCPath);
        assertThat(freshC.getLevel()).isEqualTo(4);

        // 3. Assert Node D (grandchild of B / child of C)
        String expectedDPath = expectedCPath + nodeDId + "/";
        assertThat(freshD.getParentId()).isEqualTo(nodeCId);
        assertThat(freshD.getTreePath()).isEqualTo(expectedDPath);
        assertThat(freshD.getLevel()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should cascadingly deactivate entire multi-level subtree (B -> C -> D) when B is deactivated")
    void shouldDeactivateMultiLevelSubtreeCorrectly() {
        // Deactivate Node B
        DeactivateOrgUnitCommand command = new DeactivateOrgUnitCommand(nodeBId);
        deactivateOrgUnitUseCase.execute(command);

        // Fetch fresh entities from DB
        OrgUnitJpaEntity freshB = orgUnitRepository.findById(nodeBId).orElseThrow();
        OrgUnitJpaEntity freshC = orgUnitRepository.findById(nodeCId).orElseThrow();
        OrgUnitJpaEntity freshD = orgUnitRepository.findById(nodeDId).orElseThrow();

        assertThat(freshB.getStatus()).isEqualTo(OrgUnitStatus.INACTIVE);
        assertThat(freshC.getStatus()).isEqualTo(OrgUnitStatus.INACTIVE);
        assertThat(freshD.getStatus()).isEqualTo(OrgUnitStatus.INACTIVE);
    }
}
