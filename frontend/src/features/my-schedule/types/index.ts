export type ConfirmationStatus = "NOT_CONFIRMED" | "CONFIRMED" | "STALE" | "HAS_FEEDBACK";

export interface AllocationItem {
  allocation_id: number;
  project_id: number;
  project_name: string;
  project_status: string;
  allocated_hours: number;
}

export interface WeeklySchedule {
  week_start_date: string;
  total_hours: number;
  confirmation_status: ConfirmationStatus;
  confirmed_at: string | null;
  feedback_note?: string | null;
  feedback_at?: string | null;
  allocations: AllocationItem[];
}

export interface MyAllocationsResponse {
  weeks: WeeklySchedule[];
}

export interface ConfirmScheduleResponse {
  week_start_date: string;
  confirmed_at: string;
  confirmation_status: ConfirmationStatus;
  already_confirmed: boolean;
  previous_confirmation_was_stale?: boolean;
}

export interface ProvideFeedbackResponse {
  week_start_date: string;
  confirmation_status: ConfirmationStatus;
  feedback_note: string;
  feedback_at: string;
  message: string;
}

export interface MyAllocationsErrorResponse {
  timestamp: string;
  status: number;
  error_code: string;
  message: string;
}