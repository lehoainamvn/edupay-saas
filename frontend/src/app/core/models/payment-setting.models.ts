export interface BankInfo {
  bin: string;
  code: string;
  shortName: string;
  name: string;
}

export interface PaymentSetting {
  id?: string;
  bankBin: string;
  bankName: string;
  accountNumber: string;
  accountName: string;
  transferPrefix: string;
  vietQrSampleUrl?: string;
  configured: boolean;
}

export interface PaymentSettingRequest {
  bankBin: string;
  bankName: string;
  accountNumber: string;
  accountName: string;
  transferPrefix?: string;
}

/**
 * Danh sách các ngân hàng thương mại phổ biến nhất tại Việt Nam (chuẩn Napas247).
 */
export const POPULAR_BANKS: BankInfo[] = [
  { bin: '970422', code: 'MB', shortName: 'MBBank', name: 'Ngân hàng TMCP Quân đội' },
  { bin: '970436', code: 'VCB', shortName: 'Vietcombank', name: 'Ngân hàng Ngoại thương Việt Nam' },
  { bin: '970407', code: 'TCB', shortName: 'Techcombank', name: 'Ngân hàng Kỹ thương Việt Nam' },
  { bin: '970418', code: 'BIDV', shortName: 'BIDV', name: 'Ngân hàng Đầu tư và Phát triển Việt Nam' },
  { bin: '970415', code: 'CTG', shortName: 'VietinBank', name: 'Ngân hàng Công thương Việt Nam' },
  { bin: '970416', code: 'ACB', shortName: 'ACB', name: 'Ngân hàng Á Châu' },
  { bin: '970432', code: 'VPB', shortName: 'VPBank', name: 'Ngân hàng Việt Nam Thịnh Vượng' },
  { bin: '970423', code: 'TPB', shortName: 'TPBank', name: 'Ngân hàng Tiên Phong' },
  { bin: '970403', code: 'STB', shortName: 'Sacombank', name: 'Ngân hàng Sài Gòn Thương Tín' },
  { bin: '970437', code: 'HDB', shortName: 'HDBank', name: 'Ngân hàng Phát triển TP.HCM' },
  { bin: '970441', code: 'VIB', shortName: 'VIB', name: 'Ngân hàng Quốc tế' },
  { bin: '970443', code: 'SHB', shortName: 'SHB', name: 'Ngân hàng Sài Gòn - Hà Nội' },
  { bin: '970405', code: 'VBA', shortName: 'Agribank', name: 'Ngân hàng Nông nghiệp & PTNT' },
  { bin: '970448', code: 'OCB', shortName: 'OCB', name: 'Ngân hàng Phương Đông' },
  { bin: '970426', code: 'MSB', shortName: 'MSB', name: 'Ngân hàng Hàng Hải' },
  { bin: '970440', code: 'SEAB', shortName: 'SeABank', name: 'Ngân hàng Đông Nam Á' }
];

