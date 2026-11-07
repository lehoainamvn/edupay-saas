export type ParameterGroup = 'ALL' | 'SYSTEM' | 'TUITION' | 'NOTIFICATION';
export type ParameterDataType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON';

export interface SystemParameterItem {
  id: string;
  paramKey: string;
  paramValue: string;
  paramGroup: 'SYSTEM' | 'TUITION' | 'NOTIFICATION';
  dataType: ParameterDataType;
  displayName: string;
  description: string;
  isSystem: boolean;
  updatedAt: string;
}

export interface UpdateSystemParameterRequest {
  paramValue: string;
}

export interface ParameterGroupMeta {
  code: ParameterGroup;
  label: string;
  description: string;
}

export const PARAMETER_GROUPS: ParameterGroupMeta[] = [
  { code: 'ALL', label: 'Tất cả nhóm', description: 'Toàn bộ tham số cấu hình của trung tâm' },
  { code: 'SYSTEM', label: 'Hệ thống', description: 'Bảo mật, phiên đăng nhập và quyền giáo viên' },
  { code: 'TUITION', label: 'Học phí', description: 'Hạn đóng, ngày chốt phiếu và quy tắc quá hạn' },
  { code: 'NOTIFICATION', label: 'Thông báo', description: 'Nhắc nợ tự động và mẫu tin nhắn VietQR' }
];

