import { apiRequest } from '../api-client';
import type { WorkLogResult } from './work-logs';

export interface ApprovalResult {
  entry: WorkLogResult;
  warnings: string[];
}

export const getPendingApprovals = async (): Promise<WorkLogResult[]> => {
  return await apiRequest('/work-logs/approvals/pending', { method: 'GET' });
};

export const approveTimesheetEntry = async (entryId: number): Promise<ApprovalResult> => {
  return await apiRequest(`/work-logs/approvals/entries/${entryId}/approve`, { method: 'PUT' });
};

export const rejectTimesheetEntry = async (
  entryId: number,
  rejectionReason: string
): Promise<ApprovalResult> => {
  return await apiRequest(`/work-logs/approvals/entries/${entryId}/reject`, {
    method: 'PUT',
    body: JSON.stringify({ rejectionReason }),
  });
};
