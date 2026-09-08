export interface HrProfileData {
    id: string;
    employeeCode: string;
    fullName: string;
    email?: string;
    username?: string;
    password?: string;
    phone?: string;
    orgUnitId?: string;
    department: string;
    professionalRole?: string;
    startDate?: string;
    contractEndDate?: string;
    standardHoursPerWeek: number;
    employeeId?: number;
}

export type HrProfileFormErrors = Partial<Record<keyof HrProfileData, string>>;
