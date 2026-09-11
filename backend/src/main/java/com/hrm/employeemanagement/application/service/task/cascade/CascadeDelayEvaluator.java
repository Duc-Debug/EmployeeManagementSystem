package com.hrm.employeemanagement.application.service.task.cascade;

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

        Map<Long, Task> taskMap = buildTaskMap(allProjectTasks);
        Map<Long, List<TaskDependency>> predMap = buildPredecessorMap(dependencies);

        Map<Long, AffectedTaskResult> affectedTaskMap = new HashMap<>();
        List<AffectedTaskResult> affectedTasks = evaluateAffectedTasks(rootTask, rootSlipDays, originalDueDate, taskMap, predMap, affectedTaskMap);
        List<AffectedMilestoneResult> affectedMilestones = evaluateAffectedMilestones(rootTask, rootSlipDays, affectedTaskMap, milestones);

        boolean chainOnTimeDueToSlack = !affectedTasks.isEmpty() && affectedTasks.stream().allMatch(AffectedTaskResult::protectedBySlack);
        String summaryMessage = buildSummary(affectedTasks, affectedMilestones, chainOnTimeDueToSlack);

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

    private Map<Long, Task> buildTaskMap(List<Task> allProjectTasks) {
        Map<Long, Task> taskMap = new HashMap<>();
        if (allProjectTasks != null) {
            for (Task t : allProjectTasks) {
                if (t.getIdValue() != null) {
                    taskMap.put(t.getIdValue(), t);
                }
            }
        }
        return taskMap;
    }

    private Map<Long, List<TaskDependency>> buildPredecessorMap(List<TaskDependency> dependencies) {
        Map<Long, List<TaskDependency>> predMap = new HashMap<>();
        if (dependencies != null) {
            for (TaskDependency dep : dependencies) {
                predMap.computeIfAbsent(dep.getPredecessorIdValue(), k -> new ArrayList<>()).add(dep);
            }
        }
        return predMap;
    }

    private List<AffectedTaskResult> evaluateAffectedTasks(
            Task rootTask,
            long rootSlipDays,
            LocalDate originalDueDate,
            Map<Long, Task> taskMap,
            Map<Long, List<TaskDependency>> predMap,
            Map<Long, AffectedTaskResult> affectedTaskMap) {

        Queue<Long> queue = new LinkedList<>();
        queue.add(rootTask.getIdValue());

        Map<Long, Long> effectiveDelays = new HashMap<>();
        effectiveDelays.put(rootTask.getIdValue(), rootSlipDays);

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

                Long prevDelay = effectiveDelays.get(succId);
                if (prevDelay == null || actualNetDelay > prevDelay) {
                    LocalDate succDueDate = succTask.getDueDate() != null ? succTask.getDueDate() : originalDueDate;
                    LocalDate newCalculatedEndDate = succDueDate.plusDays(actualNetDelay);

                    String statusDescription = isProtected
                            ? "Chuỗi vẫn đúng hạn nhờ thời gian dự phòng (" + slack + " ngày)"
                            : "Bị đẩy lùi " + actualNetDelay + " ngày";

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

                    effectiveDelays.put(succId, actualNetDelay);
                    affectedTaskMap.put(succId, item);
                    queue.add(succId);
                }
            }
        }

        return new ArrayList<>(affectedTaskMap.values());
    }

    private List<AffectedMilestoneResult> evaluateAffectedMilestones(
            Task rootTask,
            long rootSlipDays,
            Map<Long, AffectedTaskResult> affectedTaskMap,
            List<Milestone> milestones) {

        List<AffectedMilestoneResult> affectedMilestones = new ArrayList<>();
        if (milestones == null || milestones.isEmpty()) {
            return affectedMilestones;
        }

        for (Milestone m : milestones) {
            Set<TaskId> linked = m.getLinkedTaskIds();
            if (linked == null || linked.isEmpty()) {
                continue;
            }

            long maxMilestoneDelay = 0;
            boolean milestoneProtected = true;
            boolean containsAffectedOrRoot = false;

            for (TaskId tId : linked) {
                Long idVal = tId.value();
                if (Objects.equals(idVal, rootTask.getIdValue())) {
                    containsAffectedOrRoot = true;
                    if (rootSlipDays > 0) {
                        milestoneProtected = false;
                        maxMilestoneDelay = Math.max(maxMilestoneDelay, rootSlipDays);
                    }
                } else if (affectedTaskMap.containsKey(idVal)) {
                    containsAffectedOrRoot = true;
                    AffectedTaskResult taskRes = affectedTaskMap.get(idVal);
                    if (!taskRes.protectedBySlack()) {
                        milestoneProtected = false;
                        maxMilestoneDelay = Math.max(maxMilestoneDelay, taskRes.delayDays());
                    }
                }
            }

            if (containsAffectedOrRoot) {
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

        return affectedMilestones;
    }

    private String buildSummary(
            List<AffectedTaskResult> affectedTasks,
            List<AffectedMilestoneResult> affectedMilestones,
            boolean chainOnTimeDueToSlack) {

        if (chainOnTimeDueToSlack) {
            return "Hệ thống báo chuỗi vẫn đúng hạn nhờ thời gian dự phòng";
        }
        if (affectedTasks.isEmpty()) {
            return "Không có công việc phụ thuộc phía sau bị ảnh hưởng";
        }
        return "Phát hiện " + affectedTasks.stream().filter(t -> !t.protectedBySlack()).count()
                + " công việc và " + affectedMilestones.stream().filter(m -> !m.protectedBySlack()).count()
                + " mốc tiến độ bị lùi do trễ dây chuyền";
    }
}
