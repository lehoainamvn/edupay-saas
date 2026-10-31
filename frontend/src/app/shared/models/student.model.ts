import { FeeType } from './class.model';

export type StudentStatus = 'STUDYING' | 'PAUSED' | 'DROPPED';

export interface StudentItem {
  id: string;
  classId: string;
  className: string;
  classFeeType: FeeType;
  classDefaultFee: number;
  fullName: string;
  nickname?: string;
  parentName?: string;
  parentPhone: string;
  customFee?: number | null;
  effectiveFee: number;
  status: StudentStatus;
  statusLabel?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface StudentRequestPayload {
  classId: string;
  fullName: string;
  nickname?: string;
  parentName?: string;
  parentPhone: string;
  customFee?: number | null;
  status?: StudentStatus;
}

export interface StudentImportResult {
  totalRows: number;
  importedCount: number;
  skippedCount: number;
  importedStudents: StudentItem[];
  rowErrors: string[];
}
