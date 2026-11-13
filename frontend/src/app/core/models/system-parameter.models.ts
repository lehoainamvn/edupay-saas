export type ParameterGroup = 'ALL' | 'SYSTEM' | 'TUITION' | 'NOTIFICATION' | 'SECURITY';
export type ParameterDataType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON';

export interface SystemParameterItem {
  id: string;
  paramKey: string;
  paramValue: string;
  paramGroup: string;
  dataType: ParameterDataType;
  displayName: string;
  description: string;
  isSystem: boolean;
  updatedAt: string;
}

export interface CreateSystemParameterRequest {
  paramKey: string;
  paramValue: string;
  paramGroup: string;
  dataType: ParameterDataType;
  displayName: string;
  description?: string;
}

export interface UpdateSystemParameterRequest {
  paramValue: string;
  displayName?: string;
  description?: string;
}

export interface ParameterGroupMeta {
  code: ParameterGroup;
  label: string;
  description: string;
}

export const PARAMETER_GROUPS: ParameterGroupMeta[] = [
  { code: 'ALL', label: 'Tất cả nhóm', description: 'Toàn bộ tham số cấu hình của hệ thống' },
  { code: 'SYSTEM', label: 'Hệ thống', description: 'Cấu hình chung, tên hiển thị và hạn mức lớp' },
  { code: 'TUITION', label: 'Học phí', description: 'Hạn đóng, ngày chốt phiếu và quy tắc quá hạn' },
  { code: 'NOTIFICATION', label: 'Thông báo', description: 'Nhắc nợ tự động và mẫu tin nhắn VietQR' },
  { code: 'SECURITY', label: 'Bảo mật', description: 'Số lần đăng nhập sai, khóa tài khoản và phiên làm việc' },
];
