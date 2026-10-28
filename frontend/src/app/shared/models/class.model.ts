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
  createdAt?: string;
  updatedAt?: string;
}

export interface ClassRequestPayload {
  name: string;
  description?: string;
  feeType: FeeType;
  defaultFee: number;
}
