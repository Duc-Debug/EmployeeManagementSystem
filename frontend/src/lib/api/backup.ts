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

function extractData<T>(res: unknown): T {
  if (!res) return res as T;
  if (typeof res === "object" && res !== null && "data" in res) {
    const data = (res as { data: T }).data;
    if (data !== undefined) return data;
  }
  return res as T;
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
  const res = await apiRequest<ApiResponse<BackupItem[]> | BackupItem[]>(`/backups${query}`);
  return extractData<BackupItem[]>(res) || [];
}

export async function fetchBackupSummary(): Promise<BackupSummary> {
  const res = await apiRequest<ApiResponse<BackupSummary> | BackupSummary>("/backups/summary");
  return extractData<BackupSummary>(res);
}

export async function createBackup(
  title: string,
  description?: string,
  backupType: BackupType = "FULL"
): Promise<BackupItem> {
  const res = await apiRequest<ApiResponse<BackupItem> | BackupItem>("/backups", {
    method: "POST",
    body: JSON.stringify({ title, description, backupType }),
  });
  return extractData<BackupItem>(res);
}

export async function restoreBackup(
  backupId: number,
  confirmationCode: string,
  reason: string
): Promise<void> {
  await apiRequest<ApiResponse<void> | void>(`/backups/${backupId}/restore`, {
    method: "POST",
    body: JSON.stringify({ confirmationCode, reason }),
  });
}

export async function deleteBackup(
  backupId: number,
  reason?: string
): Promise<void> {
  const query = reason ? `?reason=${encodeURIComponent(reason)}` : "";
  await apiRequest<ApiResponse<void> | void>(`/backups/${backupId}${query}`, {
    method: "DELETE",
  });
}

export async function fetchBackupSchedule(): Promise<BackupSchedule> {
  const res = await apiRequest<ApiResponse<BackupSchedule> | BackupSchedule>("/backups/schedule");
  return extractData<BackupSchedule>(res);
}

export async function updateBackupSchedule(
  schedule: Partial<BackupSchedule>
): Promise<BackupSchedule> {
  const res = await apiRequest<ApiResponse<BackupSchedule> | BackupSchedule>("/backups/schedule", {
    method: "PUT",
    body: JSON.stringify(schedule),
  });
  return extractData<BackupSchedule>(res);
}

export async function fetchBackupAuditLogs(): Promise<BackupAuditLog[]> {
  const res = await apiRequest<ApiResponse<BackupAuditLog[]> | BackupAuditLog[]>("/backups/audit-logs");
  return extractData<BackupAuditLog[]>(res) || [];
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

  const res = await apiRequest<ApiResponse<BackupItem> | BackupItem>("/backups/upload", {
    method: "POST",
    body: formData,
  });
  return extractData<BackupItem>(res);
}
