package com.hrm.employeemanagement.domain.task.cascade;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.hrm.employeemanagement.application.dto.task.cascade.AffectedMilestoneResult;
import com.hrm.employeemanagement.application.dto.task.cascade.AffectedTaskResult;
import com.hrm.employeemanagement.application.dto.task.cascade.CascadeDelayWarningResult;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;

public class CascadeDelayEvaluator {

    public CascadeDelayWarningResult evaluate(
            Task rootTask,
            LocalDate newActualEndDate,
            List<Task> allProjectTasks,
            List<TaskDependency> dependencies,
            List<Milestone> milestones) {

        Objects.requireNonNull(rootTask, "Công việc đầu chuỗi không được null");
        Objects.requireNonNull(newActualEndDate, "Ngày kết thúc thực tế mới không được null");

        LocalDate originalDueDate = rootTask.getDueDate() != null
                ? rootTask.getDueDate()
                : (rootTask.getCreatedAt() != null ? rootTask.getCreatedAt().toLocalDate() : LocalDate.now());

        long rootSlipDays = ChronoUnit.DAYS.between(originalDueDate, newActualEndDate);

        if (rootSlipDays <= 0) {
            return new CascadeDelayWarningResult(
                    rootTask.getIdValue(),
                    rootTask.getTaskCode(),
                    rootTask.getName(),
                    originalDueDate,
                    newActualEndDate,
                    0,
                    true,
                    List.of(),
                    List.of(),
                    "Công việc hoàn thành đúng hạn hoặc sớm hơn kế hoạch."
            );
        }

        // Map task by ID
        Map<Long, Task> taskMap = new HashMap<>();
        for (Task t : allProjectTasks) {
            if (t.getIdValue() != null) {
                taskMap.put(t.getIdValue(), t);
            }
        }

        // Map predecessors -> list of dependencies
        Map<Long, List<TaskDependency>> predMap = new HashMap<>();
        for (TaskDependency dep : dependencies) {
            predMap.computeIfAbsent(dep.getPredecessorIdValue(), k -> new ArrayList<>()).add(dep);
        }

        // BFS / Propagation
        Queue<Long> queue = new LinkedList<>();
        queue.add(rootTask.getIdValue());

        Map<Long, Long> effectiveDelays = new HashMap<>();
        effectiveDelays.put(rootTask.getIdValue(), rootSlipDays);

        Map<Long, AffectedTaskResult> affectedTaskMap = new HashMap<>();

        Set<Long> visited = new HashSet<>();
        visited.add(rootTask.getIdValue());

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            long currentDelay = effectiveDelays.getOrDefault(currentId, 0L);

            List<TaskDependency> outgoing = predMap.getOrDefault(currentId, List.of());
            for (TaskDependency dep : outgoing) {
                Long succId = dep.getSuccessorIdValue();
                Task succTask = taskMap.get(succId);
                if (succTask == null) {
                    continue;
                }

                int lag = dep.getLagDays() != null ? dep.getLagDays() : 0;
                int slack = succTask.getSlackDays() != null ? succTask.getSlackDays() : 0;

                long netDelay = currentDelay + lag - slack;
                boolean isProtected = netDelay <= 0;
                long actualNetDelay = Math.max(0, netDelay);

                LocalDate succDueDate = succTask.getDueDate() != null ? succTask.getDueDate() : originalDueDate;
                LocalDate newCalculatedEndDate = succDueDate.plusDays(actualNetDelay > 0 ? actualNetDelay : 0);

                String statusDescription;
                if (isProtected) {
                    statusDescription = "Chuỗi vẫn đúng hạn nhờ thời gian dự phòng (" + slack + " ngày)";
                } else {
                    statusDescription = "Bị đẩy lùi " + actualNetDelay + " ngày";
                }

                AffectedTaskResult item = new AffectedTaskResult(
                        succTask.getIdValue(),
                        succTask.getTaskCode(),
                        succTask.getName(),
                        succDueDate,
                        newCalculatedEndDate,
                        actualNetDelay,
                        slack,
                        isProtected,
                        statusDescription
                );

                affectedTaskMap.put(succId, item);
                effectiveDelays.put(succId, actualNetDelay);

                if (!visited.contains(succId)) {
                    visited.add(succId);
                    queue.add(succId);
                }
            }
        }

        List<AffectedTaskResult> affectedTasks = new ArrayList<>(affectedTaskMap.values());

        // Milestones evaluation
        List<AffectedMilestoneResult> affectedMilestones = new ArrayList<>();
        if (milestones != null) {
            for (Milestone m : milestones) {
                Set<TaskId> linked = m.getLinkedTaskIds();
                long maxMilestoneDelay = 0;
                boolean milestoneProtected = true;

                if (linked != null && !linked.isEmpty()) {
                    for (TaskId tId : linked) {
                        Long idVal = tId.value();
                        if (affectedTaskMap.containsKey(idVal)) {
                            AffectedTaskResult taskRes = affectedTaskMap.get(idVal);
                            if (!taskRes.protectedBySlack()) {
                                milestoneProtected = false;
                                maxMilestoneDelay = Math.max(maxMilestoneDelay, taskRes.delayDays());
                            }
                        }
                    }
                }

                if (affectedTaskMap.containsKey(rootTask.getIdValue()) || linked.stream().anyMatch(t -> Objects.equals(t.value(), rootTask.getIdValue()))) {
                    if (rootSlipDays > 0 && maxMilestoneDelay > 0) {
                        milestoneProtected = false;
                    }
                }

                if (!linked.isEmpty() && affectedTaskMap.keySet().stream().anyMatch(id -> linked.contains(new TaskId(id)))) {
                    LocalDate newDate = m.getPlannedDate().plusDays(maxMilestoneDelay);
                    String mStatus = milestoneProtected
                            ? "Mốc vẫn đúng hạn nhờ thời gian dự phòng"
                            : "Bị đẩy lùi " + maxMilestoneDelay + " ngày";

                    affectedMilestones.add(new AffectedMilestoneResult(
                            m.getIdValue(),
                            m.getName(),
                            m.getPlannedDate(),
                            newDate,
                            maxMilestoneDelay,
                            milestoneProtected,
                            mStatus
                    ));
                }
            }
        }

        boolean chainOnTimeDueToSlack = !affectedTasks.isEmpty() && affectedTasks.stream().allMatch(AffectedTaskResult::protectedBySlack);

        String summaryMessage;
        if (chainOnTimeDueToSlack) {
            summaryMessage = "Hệ thống báo chuỗi vẫn đúng hạn nhờ thời gian dự phòng";
        } else if (affectedTasks.isEmpty()) {
            summaryMessage = "Không có công việc phụ thuộc phía sau bị ảnh hưởng";
        } else {
            summaryMessage = "Phát hiện " + affectedTasks.stream().filter(t -> !t.protectedBySlack()).count()
                    + " công việc và " + affectedMilestones.stream().filter(m -> !m.protectedBySlack()).count()
                    + " mốc tiến độ bị lùi do trễ dây chuyền";
        }

        return new CascadeDelayWarningResult(
                rootTask.getIdValue(),
                rootTask.getTaskCode(),
                rootTask.getName(),
                originalDueDate,
                newActualEndDate,
                rootSlipDays,
                chainOnTimeDueToSlack,
                affectedTasks,
                affectedMilestones,
                summaryMessage
        );
    }
}
