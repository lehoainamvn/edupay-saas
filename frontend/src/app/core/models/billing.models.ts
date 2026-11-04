export type InvoiceStatus = 'PENDING' | 'PAID' | 'CANCELLED';
export type FeeType = 'PER_LESSON' | 'MONTHLY_PACKAGE';

export interface BillingPreviewItem {
  studentId: string;
  fullName: string;
  nickname?: string;
  parentPhone?: string;
  attendedCount: number;
  unitPrice: number;
  baseAmount: number;
  extraFee: number;
  discount: number;
  totalAmount: number;
  isLocked: boolean;
  existingInvoiceId?: string;
  existingInvoiceStatus?: InvoiceStatus;
  existingTransferCode?: string;
  existingPublicHash?: string;
}

export interface BillingPreview {
  classId: string;
  className: string;
  feeType: FeeType;
  billingMonth: string;
  totalStudents: number;
  totalAttendedCount: number;
  totalCalculatedAmount: number;
  hasBankConfigured: boolean;
  bankName?: string;
  accountNumber?: string;
  accountName?: string;
  items: BillingPreviewItem[];
}

export interface InvoiceItemGenerateRequest {
  studentId: string;
  attendedCount: number;
  unitPrice: number;
  extraFee?: number;
  discount?: number;
  note?: string;
}

export interface InvoiceBatchGenerateRequest {
  classId: string;
  billingMonth: string;
  items: InvoiceItemGenerateRequest[];
}

export interface InvoicePaymentManualRequest {
  paidMethod: string;
  note?: string;
}

export interface Invoice {
  id: string;
  classId: string;
  className?: string;
  studentId: string;
  studentName?: string;
  nickname?: string;
  parentPhone?: string;
  billingMonth: string;
  attendedCount: number;
  unitPrice: number;
  extraFee: number;
  discount: number;
  totalAmount: number;
  transferCode: string;
  publicHash: string;
  publicUrl: string;
  vietQrUrl?: string;
  status: InvoiceStatus;
  paidAt?: string;
  paidMethod?: string;
  note?: string;
  createdAt: string;
}

export interface PublicInvoice {
  publicHash: string;
  className: string;
  studentName: string;
  nickname?: string;
  billingMonth: string;
  attendedCount: number;
  unitPrice: number;
  extraFee: number;
  discount: number;
  totalAmount: number;
  transferCode: string;
  status: InvoiceStatus;
  paidAt?: string;
  note?: string;
  bankBin?: string;
  bankName?: string;
  accountNumber?: string;
  accountName?: string;
  vietQrUrl?: string;
  attendedDates?: string[];
}

