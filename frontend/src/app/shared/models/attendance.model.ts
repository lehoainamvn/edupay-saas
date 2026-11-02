export type AttendanceStatus = 'PRESENT' | 'EXCUSED' | 'ABSENT';

export interface DailyAttendanceItem {
  studentId: string;
  fullName: string;
  nickname?: string | null;
  parentPhone?: string | null;
  attendanceId?: string | null;
  status: AttendanceStatus | null;
  note?: string | null;
  isLocked: boolean;
}

export interface DailyAttendance {
  classId: string;
  className: string;
  lessonDate: string; // YYYY-MM-DD
  totalStudents: number;
  markedCount: number;
  presentCount: number;
  excusedCount: number;
  absentCount: number;
  isClassLocked: boolean;
  students: DailyAttendanceItem[];
}

export interface AttendanceItemPayload {
  studentId: string;
  status: AttendanceStatus;
  note?: string | null;
}

export interface AttendanceBatchUpsertPayload {
  classId: string;
  lessonDate: string; // YYYY-MM-DD
  items: AttendanceItemPayload[];
}

export interface AttendanceRecord {
  attendanceId: string;
  status: AttendanceStatus;
  note?: string | null;
}

export interface StudentMonthlyAttendance {
  studentId: string;
  fullName: string;
  nickname?: string | null;
  attendedCount: number;
  excusedCount: number;
  absentCount: number;
  totalLessons: number;
  isLocked: boolean;
  dailyRecords: { [date: string]: AttendanceRecord | undefined };
}

export interface MonthlyAttendanceMatrix {
  classId: string;
  className: string;
  month: string; // YYYY-MM
  lessonDates: string[]; // YYYY-MM-DD[]
  totalClassLessons: number;
  students: StudentMonthlyAttendance[];
}

export interface AttendanceUpdatePayload {
  status: AttendanceStatus;
  note?: string | null;
}

export interface AttendanceResponseItem {
  id: string;
  classId: string;
  studentId: string;
  lessonDate: string;
  status: AttendanceStatus;
  note?: string | null;
}
