import { apiRequest } from "@/lib/api-client";

export type BackupType = "FULL" | "RESOURCE_PLAN";
export type BackupStatus = "IN_PROGRESS" | "COMPLETED" | "FAILED";
export type BackupFrequency = "DAILY" | "WEEKLY";

export interface BackupItem {
  id: number;
  backupCode: string;
  title: string;
  description?: string;
  backupType: BackupType;
  backupTypeLabel: string;
  fileName: string;
  fileSizeBytes: number;
  formattedFileSize: string;
  checksum?: string;
  status: BackupStatus;
  statusDescription: string;
  isAutomatic: boolean;
  createdBy?: number;
  createdByName?: string;
  createdAt: string;
  completedAt?: string;
  errorMessage?: string;
}

export interface BackupSchedule {
  id: number;
  isEnabled: boolean;
  frequency: BackupFrequency;
  frequencyLabel: string;
  scheduledTime: string;
  dayOfWeek?: string;
  backupType: BackupType;
  backupTypeLabel: string;
  retentionDays: number;
  lastRunAt?: string;
  nextRunAt?: string;
  updatedBy?: number;
  updatedAt: string;
}

export interface BackupSummary {
  totalBackups: number;
  totalFileSizeBytes: number;
  formattedTotalSize: string;
  latestCompletedBackup?: BackupItem;
  schedule?: BackupSchedule;
}

export interface BackupAuditLog {
  id: number;
  userId?: number;
  userEmail?: string;
  action: string;
  actionDescription: string;
  backupId?: number;
  status: string;
  reason?: string;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode?: string;
}

export async function fetchBackups(
  type?: BackupType,
  status?: BackupStatus,
  search?: string
): Promise<BackupItem[]> {
  const params = new URLSearchParams();
  if (type) params.append("type", type);
  if (status) params.append("status", status);
  if (search && search.trim()) params.append("search", search.trim());

  const query = params.toString() ? `?${params.toString()}` : "";
  const res = await apiRequest<ApiResponse<BackupItem[]>>(`/backups${query}`);
  return res.data || [];
}

export async function fetchBackupSummary(): Promise<BackupSummary> {
  const res = await apiRequest<ApiResponse<BackupSummary>>("/backups/summary");
  return res.data;
}

export async function createBackup(
  title: string,
  description?: string,
  backupType: BackupType = "FULL"
): Promise<BackupItem> {
  const res = await apiRequest<ApiResponse<BackupItem>>("/backups", {
    method: "POST",
    body: JSON.stringify({ title, description, backupType }),
  });
  return res.data;
}

export async function restoreBackup(
  backupId: number,
  confirmationCode: string,
  reason: string
): Promise<void> {
  await apiRequest<ApiResponse<void>>(`/backups/${backupId}/restore`, {
    method: "POST",
    body: JSON.stringify({ confirmationCode, reason }),
  });
}

export async function deleteBackup(
  backupId: number,
  reason?: string
): Promise<void> {
  const query = reason ? `?reason=${encodeURIComponent(reason)}` : "";
  await apiRequest<ApiResponse<void>>(`/backups/${backupId}${query}`, {
    method: "DELETE",
  });
}

export async function fetchBackupSchedule(): Promise<BackupSchedule> {
  const res = await apiRequest<ApiResponse<BackupSchedule>>("/backups/schedule");
  return res.data;
}

export async function updateBackupSchedule(
  schedule: Partial<BackupSchedule>
): Promise<BackupSchedule> {
  const res = await apiRequest<ApiResponse<BackupSchedule>>("/backups/schedule", {
    method: "PUT",
    body: JSON.stringify(schedule),
  });
  return res.data;
}

export async function fetchBackupAuditLogs(): Promise<BackupAuditLog[]> {
  const res = await apiRequest<ApiResponse<BackupAuditLog[]>>("/backups/audit-logs");
  return res.data || [];
}

export async function uploadBackupFile(
  file: File,
  title?: string,
  description?: string,
  backupType: BackupType = "FULL"
): Promise<BackupItem> {
  const formData = new FormData();
  formData.append("file", file);
  if (title) formData.append("title", title);
  if (description) formData.append("description", description);
  formData.append("backupType", backupType);

  const res = await apiRequest<ApiResponse<BackupItem>>("/backups/upload", {
    method: "POST",
    body: formData,
  });
  return res.data;
}
