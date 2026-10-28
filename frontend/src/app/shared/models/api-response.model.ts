export interface ApiResponse<T> {
  success: boolean;
  message: string;
  errorCode?: string;
  data: T;
  errors?: string[];
  timestamp: string;
}
