package com.hrm.employeemanagement.application.service.task.comment;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.task.comment.CreateTaskCommentCommand;
import com.hrm.employeemanagement.application.dto.task.comment.MentionedUserDto;
import com.hrm.employeemanagement.application.dto.task.comment.TaskAttachmentDownloadResult;
import com.hrm.employeemanagement.application.dto.task.comment.TaskAttachmentResult;
import com.hrm.employeemanagement.application.dto.task.comment.TaskCommentResult;
import com.hrm.employeemanagement.application.dto.task.comment.UploadedAttachmentDto;
import com.hrm.employeemanagement.application.port.inbound.task.comment.CreateTaskCommentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.DeleteTaskCommentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.DownloadTaskAttachmentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.GetTaskCommentsUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.DeleteTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskAttachmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.LoadTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.SaveTaskCommentPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.task.TaskAttachmentNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.TaskCommentNotFoundException;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.comment.MentionParserService;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachment;
import com.hrm.employeemanagement.domain.task.comment.TaskAttachmentId;
import com.hrm.employeemanagement.domain.task.comment.TaskComment;
import com.hrm.employeemanagement.domain.task.comment.TaskCommentId;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class TaskCommentApplicationService
        implements CreateTaskCommentUseCase, GetTaskCommentsUseCase, DeleteTaskCommentUseCase, DownloadTaskAttachmentUseCase {

    private final LoadTaskCommentPort loadTaskCommentPort;
    private final LoadTaskAttachmentPort loadTaskAttachmentPort;
    private final SaveTaskCommentPort saveTaskCommentPort;
    private final DeleteTaskCommentPort deleteTaskCommentPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveNotificationPort saveNotificationPort;
    private final TaskAttachmentStoragePort taskAttachmentStoragePort;
    private final MentionParserService mentionParserService;
    private final TaskDiscussionAccessService accessService;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final LoadProjectPort loadProjectPort;

    public TaskCommentApplicationService(
            LoadTaskCommentPort loadTaskCommentPort,
            LoadTaskAttachmentPort loadTaskAttachmentPort,
            SaveTaskCommentPort saveTaskCommentPort,
            DeleteTaskCommentPort deleteTaskCommentPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            TaskAttachmentStoragePort taskAttachmentStoragePort,
            TaskDiscussionAccessService accessService,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadProjectPort loadProjectPort) {
        this.loadTaskCommentPort = Objects.requireNonNull(loadTaskCommentPort, "LoadTaskCommentPort must not be null");
        this.loadTaskAttachmentPort = Objects.requireNonNull(loadTaskAttachmentPort, "LoadTaskAttachmentPort must not be null");
        this.saveTaskCommentPort = Objects.requireNonNull(saveTaskCommentPort, "SaveTaskCommentPort must not be null");
        this.deleteTaskCommentPort = Objects.requireNonNull(deleteTaskCommentPort, "DeleteTaskCommentPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveNotificationPort = Objects.requireNonNull(saveNotificationPort, "SaveNotificationPort must not be null");
        this.taskAttachmentStoragePort = Objects.requireNonNull(taskAttachmentStoragePort, "TaskAttachmentStoragePort must not be null");
        this.accessService = Objects.requireNonNull(accessService, "TaskDiscussionAccessService must not be null");
        this.loadTaskAssignmentPort = loadTaskAssignmentPort;
        this.loadProjectPort = loadProjectPort;
        this.mentionParserService = new MentionParserService();
    }

    public TaskCommentApplicationService(
            LoadTaskCommentPort loadTaskCommentPort,
            LoadTaskAttachmentPort loadTaskAttachmentPort,
            SaveTaskCommentPort saveTaskCommentPort,
            DeleteTaskCommentPort deleteTaskCommentPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort,
            TaskAttachmentStoragePort taskAttachmentStoragePort,
            TaskDiscussionAccessService accessService) {
        this(loadTaskCommentPort, loadTaskAttachmentPort, saveTaskCommentPort, deleteTaskCommentPort,
                loadUserPort, loadEmployeePort, saveNotificationPort, taskAttachmentStoragePort, accessService, null, null);
    }

    @Override
    public TaskCommentResult execute(CreateTaskCommentCommand command) {
        Objects.requireNonNull(command, "Command không được null");
        TaskId taskId = TaskId.of(command.taskId());
        Task task = accessService.requireCreateAccess(command.taskId(), command.authorId());

        UserId authorId = new UserId(command.authorId());
        User author = loadUserPort.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng tác giả"));

        // Phân tích danh sách người được nhắc tên và xác thực quyền truy cập dự án/task
        Set<UserId> candidateMentionIds = new HashSet<>();
        if (command.mentionedUserIds() != null) {
            for (Long uid : command.mentionedUserIds()) {
                if (uid != null && !uid.equals(command.authorId())) {
                    candidateMentionIds.add(new UserId(uid));
                }
            }
        }

        Set<String> parsedUsernames = mentionParserService.extractMentionedUsernames(command.content());
        for (String uname : parsedUsernames) {
            loadUserPort.findByUsername(uname).ifPresent(u -> {
                if (!u.getId().equals(authorId)) {
                    candidateMentionIds.add(u.getId());
                }
            });
        }

        Set<UserId> targetMentions = new HashSet<>();
        if (!candidateMentionIds.isEmpty()) {
            List<User> candidateUsers = loadUserPort.findAllByIdIn(List.copyOf(candidateMentionIds));
            for (User candidate : candidateUsers) {
                if (accessService.canUserAccess(candidate, task)) {
                    targetMentions.add(candidate.getId());
                }
            }
        }

        List<TaskAttachment> attachments = new ArrayList<>();
        if (command.attachments() != null) {
            for (UploadedAttachmentDto att : command.attachments()) {
                attachments.add(new TaskAttachment(
                        null,
                        null,
                        taskId,
                        att.originalFileName(),
                        att.storedFilePath(),
                        att.fileSize(),
                        att.contentType(),
                        authorId,
                        LocalDateTime.now()));
            }
        }

        TaskComment comment = new TaskComment(
                null,
                taskId,
                authorId,
                command.content(),
                attachments,
                targetMentions,
                LocalDateTime.now(),
                null,
                0L);

        TaskComment saved = saveTaskCommentPort.save(comment);

        // Tìm danh sách những người liên quan đến task để gửi thông báo trao đổi (TASK_COMMENT)
        Set<UserId> taskParticipantIds = new HashSet<>();

        // Nhân sự được phân công thực hiện task
        if (loadTaskAssignmentPort != null) {
            List<TaskAssignment> assignments = loadTaskAssignmentPort.findByTaskId(task.getId());
            if (assignments != null) {
                for (TaskAssignment a : assignments) {
                    if (a.getEmployeeId() != null) {
                        loadEmployeePort.findById(a.getEmployeeId())
                                .map(Employee::getUserId)
                                .ifPresent(taskParticipantIds::add);
                    }
                }
            }
        }
        if (task.getAssigneeId() != null) {
            loadEmployeePort.findById(task.getAssigneeId())
                    .map(Employee::getUserId)
                    .ifPresent(taskParticipantIds::add);
        }

        // Quản lý dự án (PM)
        if (loadProjectPort != null && task.getProjectId() != null) {
            loadProjectPort.findById(task.getProjectId()).ifPresent(project -> {
                if (project.getManagerId() != null) {
                    loadEmployeePort.findById(project.getManagerId())
                            .map(Employee::getUserId)
                            .ifPresent(taskParticipantIds::add);
                }
            });
        }

        // Những người từng tham gia trao đổi trong task này trước đó
        List<TaskComment> pastComments = loadTaskCommentPort.findAllByTaskId(taskId);
        if (pastComments != null) {
            for (TaskComment pc : pastComments) {
                if (pc.getAuthorId() != null) {
                    taskParticipantIds.add(pc.getAuthorId());
                }
            }
        }

        // Loại trừ tác giả của comment hiện tại và những người đã nhận mention
        taskParticipantIds.remove(authorId);
        taskParticipantIds.removeAll(targetMentions);

        // Xác thực quyền truy cập task cho những người liên quan
        Set<UserId> verifiedParticipants = new HashSet<>();
        if (!taskParticipantIds.isEmpty()) {
            List<User> candidateParticipants = loadUserPort.findAllByIdIn(List.copyOf(taskParticipantIds));
            for (User p : candidateParticipants) {
                if (accessService.canUserAccess(p, task)) {
                    verifiedParticipants.add(p.getId());
                }
            }
        }

        // Gửi thông báo cho từng người được nhắc tên
        String authorDisplayName = resolveDisplayName(author);
        String taskName = task.getName();
        String safeContent = command.content() != null ? command.content().trim() : "";
        String previewText = safeContent.length() > 120
                ? safeContent.substring(0, 117) + "..."
                : (!safeContent.isEmpty() ? safeContent : "Đã đính kèm tệp");

        for (UserId mentionedUser : targetMentions) {
            Notification notification = Notification.create(
                    mentionedUser,
                    authorId,
                    NotificationType.TASK_MENTION,
                    "TASK",
                    task.getId().value(),
                    authorDisplayName + " đã nhắc tên bạn trong công việc: " + taskName,
                    previewText);
            saveNotificationPort.save(notification);
        }

        // Gửi thông báo trao đổi mới cho những người liên quan trong công việc
        for (UserId participantId : verifiedParticipants) {
            Notification notification = Notification.create(
                    participantId,
                    authorId,
                    NotificationType.TASK_COMMENT,
                    "TASK",
                    task.getId().value(),
                    authorDisplayName + " đã gửi trao đổi mới trong công việc: " + taskName,
                    previewText);
            saveNotificationPort.save(notification);
        }

        return mapToResult(saved);
    }

    @Override
    public List<TaskCommentResult> execute(Long taskId) {
        accessService.requireAccess(taskId, PermissionCode.TASK_DISCUSSION_READ);
        TaskId id = TaskId.of(taskId);
        List<TaskComment> comments = loadTaskCommentPort.findAllByTaskId(id);
        Set<UserId> relevantUserIds = comments.stream()
                .flatMap(comment -> {
                    Set<UserId> ids = new HashSet<>(comment.getMentionedUserIds());
                    ids.add(comment.getAuthorId());
                    return ids.stream();
                })
                .collect(Collectors.toSet());
        List<UserId> userIds = List.copyOf(relevantUserIds);
        Map<UserId, User> usersById = loadUserPort.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UserId, Employee> employeesByUserId = loadEmployeePort.findAllByUserIdIn(userIds).stream()
                .filter(employee -> employee.getUserId() != null)
                .collect(Collectors.toMap(Employee::getUserId, Function.identity(), (first, ignored) -> first));
        return comments.stream()
                .map(comment -> mapToResult(comment, usersById, employeesByUserId))
                .toList();
    }

    @Override
    public void execute(Long taskId, Long commentId, Long requestingUserId) {
        TaskCommentId id = TaskCommentId.of(commentId);
        TaskComment comment = loadTaskCommentPort.findById(id)
                .orElseThrow(() -> new TaskCommentNotFoundException("Không tìm thấy trao đổi với ID: " + commentId));

        if (!comment.getTaskId().value().equals(taskId)) {
            throw new TaskCommentNotFoundException("Không tìm thấy trao đổi trong công việc này");
        }

        accessService.requireDeleteAccess(taskId, comment.getAuthorId().value(), requestingUserId);

        deleteTaskCommentPort.deleteById(id);
    }

    @Override
    public TaskAttachmentDownloadResult downloadAttachment(Long attachmentId) {
        Objects.requireNonNull(attachmentId, "Mã tệp đính kèm không được null");
        TaskAttachment attachment = loadTaskAttachmentPort.findById(TaskAttachmentId.of(attachmentId))
                .orElseThrow(() -> new TaskAttachmentNotFoundException(attachmentId));

        accessService.requireAccess(attachment.getTaskId().value(), PermissionCode.TASK_DISCUSSION_READ);

        InputStream is = taskAttachmentStoragePort.loadFile(attachment.getFilePath());
        return new TaskAttachmentDownloadResult(
                attachment.getFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                is);
    }

    private TaskCommentResult mapToResult(TaskComment comment) {
        List<UserId> userIds = new ArrayList<>(comment.getMentionedUserIds());
        userIds.add(comment.getAuthorId());
        Map<UserId, User> usersById = loadUserPort.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UserId, Employee> employeesByUserId = loadEmployeePort.findAllByUserIdIn(userIds).stream()
                .filter(employee -> employee.getUserId() != null)
                .collect(Collectors.toMap(Employee::getUserId, Function.identity(), (first, ignored) -> first));
        return mapToResult(comment, usersById, employeesByUserId);
    }

    private TaskCommentResult mapToResult(TaskComment comment, Map<UserId, User> usersById,
            Map<UserId, Employee> employeesByUserId) {
        User author = usersById.get(comment.getAuthorId());
        String authorName = author != null ? resolveDisplayName(author, employeesByUserId) : "Người dùng";
        String authorEmail = author != null ? author.getEmail() : "";
        String authorRole = author != null && author.getRole() != null ? author.getRole().getName() : "";

        List<TaskAttachmentResult> attachmentResults = comment.getAttachments().stream().map(att -> {
            String downloadUrl = "/api/v1/tasks/attachments/" + (att.getId() != null ? att.getId().value() : 0) + "/download";
            return new TaskAttachmentResult(
                    att.getId() != null ? att.getId().value() : null,
                    comment.getId() != null ? comment.getId().value() : null,
                    comment.getTaskId().value(),
                    att.getFileName(),
                    downloadUrl,
                    att.getFileSize(),
                    att.getFileType(),
                    att.getUploadedBy().value(),
                    authorName,
                    att.getUploadedAt());
        }).toList();

        List<MentionedUserDto> mentions = new ArrayList<>();
        for (UserId mId : comment.getMentionedUserIds()) {
            User user = usersById.get(mId);
            if (user != null) {
                mentions.add(new MentionedUserDto(
                        user.getId().value(),
                        user.getUsername(),
                        resolveDisplayName(user, employeesByUserId),
                        user.getEmail()));
            }
        }

        return new TaskCommentResult(
                comment.getId() != null ? comment.getId().value() : null,
                comment.getTaskId().value(),
                comment.getAuthorId().value(),
                authorName,
                authorEmail,
                authorRole,
                comment.getContent(),
                attachmentResults,
                mentions,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getVersion());
    }

    private String resolveDisplayName(User user) {
        Optional<Employee> emp = loadEmployeePort.findByUserId(user.getId());
        if (emp.isPresent() && emp.get().getFullName() != null && !emp.get().getFullName().isBlank()) {
            return emp.get().getFullName();
        }
        return user.getUsername();
    }

    private String resolveDisplayName(User user, Map<UserId, Employee> employeesByUserId) {
        Employee employee = employeesByUserId.get(user.getId());
        if (employee != null && employee.getFullName() != null && !employee.getFullName().isBlank()) {
            return employee.getFullName();
        }
        return user.getUsername();
    }
}
