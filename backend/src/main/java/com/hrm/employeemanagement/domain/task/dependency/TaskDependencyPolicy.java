package com.hrm.employeemanagement.domain.task.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hrm.employeemanagement.domain.exception.task.CyclicTaskDependencyException;
import com.hrm.employeemanagement.domain.task.TaskId;

public final class TaskDependencyPolicy {

    private TaskDependencyPolicy() {
        // Utility class
    }

    /**
     * Kiểm tra và chặn việc tạo quan hệ phụ thuộc vòng lặp (TC-02).
     * Dùng thuật toán duyệt đồ thị theo chiều sâu (DFS).
     *
     * @param existingDependencies Các quan hệ phụ thuộc hiện có trong dự án.
     * @param candidatePredecessorId Task tiền đề sắp tạo.
     * @param candidateSuccessorId Task phụ thuộc sắp tạo.
     * @param taskNamesMap Map ánh xạ từ TaskId sang tên/mã công việc phục vụ hiển thị thông tin lỗi rõ ràng.
     */
    public static void validateNoCycle(
            List<TaskDependency> existingDependencies,
            TaskId candidatePredecessorId,
            TaskId candidateSuccessorId,
            Map<Long, String> taskNamesMap) {

        if (candidatePredecessorId.equals(candidateSuccessorId)) {
            String taskLabel = taskNamesMap.getOrDefault(candidatePredecessorId.value(), "TK-" + candidatePredecessorId.value());
            throw new CyclicTaskDependencyException(List.of(taskLabel, taskLabel));
        }

        // Dựng đồ thị hướng: Key = Predecessor, Value = Danh sách các Successor
        Map<Long, List<Long>> adjacencyList = new HashMap<>();
        for (TaskDependency dep : existingDependencies) {
            adjacencyList.computeIfAbsent(dep.getPredecessorIdValue(), k -> new ArrayList<>())
                    .add(dep.getSuccessorIdValue());
        }

        // Thêm cạnh ứng viên mới: candidatePredecessor -> candidateSuccessor
        adjacencyList.computeIfAbsent(candidatePredecessorId.value(), k -> new ArrayList<>())
                .add(candidateSuccessorId.value());

        // Kiểm tra xem từ candidateSuccessorId có đường đi nào dẫn ngược lại candidatePredecessorId không
        List<Long> cyclePathIds = findCyclePath(adjacencyList, candidatePredecessorId.value(), candidateSuccessorId.value());
        if (!cyclePathIds.isEmpty()) {
            List<String> cyclePathLabels = cyclePathIds.stream()
                    .map(id -> taskNamesMap.getOrDefault(id, "TK-" + id))
                    .toList();
            throw new CyclicTaskDependencyException(cyclePathLabels);
        }
    }

    /**
     * Tìm đường đi từ targetStart đến targetGoal trong đồ thị nếu tồn tại vòng lặp.
     */
    private static List<Long> findCyclePath(
            Map<Long, List<Long>> graph,
            Long targetGoal,
            Long current) {

        Set<Long> visited = new HashSet<>();
        List<Long> currentPath = new ArrayList<>();
        currentPath.add(targetGoal); // Bắt đầu chuỗi hiển thị từ targetGoal (Predecessor)

        if (dfsSearch(graph, current, targetGoal, visited, currentPath)) {
            return currentPath;
        }

        return Collections.emptyList();
    }

    private static boolean dfsSearch(
            Map<Long, List<Long>> graph,
            Long current,
            Long targetGoal,
            Set<Long> visited,
            List<Long> path) {

        path.add(current);
        visited.add(current);

        if (current.equals(targetGoal)) {
            return true;
        }

        List<Long> neighbors = graph.getOrDefault(current, Collections.emptyList());
        for (Long neighbor : neighbors) {
            if (neighbor.equals(targetGoal)) {
                path.add(targetGoal);
                return true;
            }
            if (!visited.contains(neighbor)) {
                if (dfsSearch(graph, neighbor, targetGoal, visited, path)) {
                    return true;
                }
            }
        }

        path.remove(path.size() - 1);
        return false;
    }
}
