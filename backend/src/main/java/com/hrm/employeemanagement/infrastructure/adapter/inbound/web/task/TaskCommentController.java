package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hrm.employeemanagement.application.dto.task.comment.CreateTaskCommentCommand;
import com.hrm.employeemanagement.application.dto.task.comment.TaskAttachmentDownloadResult;
import com.hrm.employeemanagement.application.dto.task.comment.TaskCommentResult;
import com.hrm.employeemanagement.application.dto.task.comment.UploadedAttachmentDto;
import com.hrm.employeemanagement.application.port.inbound.task.comment.CreateTaskCommentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.DeleteTaskCommentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.DownloadTaskAttachmentUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.comment.GetTaskCommentsUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.task.comment.TaskAttachmentStoragePort;
import com.hrm.employeemanagement.application.service.task.comment.TaskDiscussionAccessService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.entity.TaskAttachmentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.comment.repository.SpringDataTaskAttachmentRepository;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskCommentController {

    private static final Logger log = LoggerFactory.getLogger(TaskCommentController.class);

    private final CreateTaskCommentUseCase createTaskCommentUseCase;
    private final GetTaskCommentsUseCase getTaskCommentsUseCase;
    private final DeleteTaskCommentUseCase deleteTaskCommentUseCase;
    private final DownloadTaskAttachmentUseCase downloadTaskAttachmentUseCase;
    private final CurrentUserPort currentUserPort;
    private final TaskAttachmentStoragePort taskAttachmentStoragePort;
    private final SpringDataTaskAttachmentRepository attachmentRepository;
    private final TaskDiscussionAccessService accessService;

    public TaskCommentController(
            CreateTaskCommentUseCase createTaskCommentUseCase,
            GetTaskCommentsUseCase getTaskCommentsUseCase,
            DeleteTaskCommentUseCase deleteTaskCommentUseCase,
            DownloadTaskAttachmentUseCase downloadTaskAttachmentUseCase,
            CurrentUserPort currentUserPort,
            TaskAttachmentStoragePort taskAttachmentStoragePort,
            SpringDataTaskAttachmentRepository attachmentRepository,
            TaskDiscussionAccessService accessService) {
        this.createTaskCommentUseCase = createTaskCommentUseCase;
        this.getTaskCommentsUseCase = getTaskCommentsUseCase;
        this.deleteTaskCommentUseCase = deleteTaskCommentUseCase;
        this.downloadTaskAttachmentUseCase = downloadTaskAttachmentUseCase;
        this.currentUserPort = currentUserPort;
        this.taskAttachmentStoragePort = taskAttachmentStoragePort;
        this.attachmentRepository = attachmentRepository;
        this.accessService = accessService;
    }

    @GetMapping("/{taskId}/comments")
    @PreAuthorize("hasAuthority('TASK_DISCUSSION_READ')")
    public ResponseEntity<ApiResponse<List<TaskCommentResult>>> getTaskComments(@PathVariable Long taskId) {
        List<TaskCommentResult> results = getTaskCommentsUseCase.execute(taskId);
        return ResponseEntity.ok(ApiResponse.success("Lấy dòng thời gian trao đổi thành công", results));
    }

    @PostMapping(value = "/{taskId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('TASK_DISCUSSION_CREATE')")
    public ResponseEntity<ApiResponse<TaskCommentResult>> createComment(
            @PathVariable Long taskId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "mentionedUserIds", required = false) List<Long> mentionedUserIds,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        accessService.requireAccess(taskId, PermissionCode.TASK_DISCUSSION_CREATE);

        Long currentUserId = currentUserPort.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực"));

        List<UploadedAttachmentDto> uploadedFiles = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String storedPath = taskAttachmentStoragePort.storeFile(
                                taskId,
                                file.getOriginalFilename(),
                                file.getInputStream(),
                                file.getSize());
                        uploadedFiles.add(new UploadedAttachmentDto(
                                file.getOriginalFilename(),
                                storedPath,
                                file.getSize(),
                                file.getContentType()));
                    } catch (Exception e) {
                        deleteUploadedFilesQuietly(uploadedFiles);
                        throw new RuntimeException("Không thể tải lên file: " + file.getOriginalFilename(), e);
                    }
                }
            }
        }

        Set<Long> mentions = mentionedUserIds != null ? new HashSet<>(mentionedUserIds) : new HashSet<>();
        CreateTaskCommentCommand command = new CreateTaskCommentCommand(
                taskId,
                currentUserId,
                content != null ? content : "",
                mentions,
                uploadedFiles);

        TaskCommentResult result;
        try {
            result = createTaskCommentUseCase.execute(command);
        } catch (RuntimeException exception) {
            deleteUploadedFilesQuietly(uploadedFiles);
            throw exception;
        }
        return ResponseEntity.ok(ApiResponse.success("Đăng ghi chú trao đổi thành công", result));
    }

    @DeleteMapping("/{taskId}/comments/{commentId}")
    @PreAuthorize("hasAuthority('TASK_DISCUSSION_CREATE')")
    @PreAuthorize("hasAuthority('TASK_DISCUSSION_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId) {
        Long currentUserId = currentUserPort.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực"));

        deleteTaskCommentUseCase.execute(taskId, commentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Xóa trao đổi thành công", null));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @PreAuthorize("hasAuthority('TASK_DISCUSSION_READ')")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable Long attachmentId) {
        TaskAttachmentJpaEntity attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tệp đính kèm với ID: " + attachmentId));
        TaskAttachmentDownloadResult result = downloadTaskAttachmentUseCase.downloadAttachment(attachmentId);
        String encodedFilename = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8).replace("+", "%20");

        accessService.requireAccess(attachment.getTaskId(), PermissionCode.TASK_DISCUSSION_READ);
        InputStream is = taskAttachmentStoragePort.loadFile(attachment.getFilePath());
        String encodedFilename = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getFileType() != null) {
        if (result.fileType() != null) {
            try {
                mediaType = MediaType.parseMediaType(attachment.getFileType());
                mediaType = MediaType.parseMediaType(result.fileType());
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(mediaType)
                .contentLength(attachment.getFileSize())
                .body(new InputStreamResource(is));
                .contentLength(result.fileSize())
                .body(new InputStreamResource(result.contentStream()));
    }

    private void deleteUploadedFilesQuietly(List<UploadedAttachmentDto> uploadedFiles) {
        for (UploadedAttachmentDto uploaded : uploadedFiles) {
            try {
                taskAttachmentStoragePort.deleteFile(uploaded.storedFilePath());
            } catch (RuntimeException ignored) {
                // Preserve the original upload or database failure.
            } catch (RuntimeException e) {
                log.warn("Không thể xóa file rác sau khi thao tác thất bại: {}", uploaded.storedFilePath(), e);
            }
        }
    }
}

