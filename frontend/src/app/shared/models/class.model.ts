export type FeeType = 'PER_LESSON' | 'MONTHLY_PACKAGE';

export interface ClassItem {
  id: string;
  name: string;
  description?: string;
  feeType: FeeType;
  feeTypeLabel?: string;
  defaultFee: number;
  isActive: boolean;
  studentCount: number;
  scheduleDays?: string;
  startTime?: string;
  endTime?: string;
  teacherId?: string;
  teacherName?: string;
  teacherPhone?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ClassRequestPayload {
  name: string;
  description?: string;
  feeType: FeeType;
  defaultFee: number;
  scheduleDays?: string;
  startTime?: string;
  endTime?: string;
  teacherId?: string;
}
