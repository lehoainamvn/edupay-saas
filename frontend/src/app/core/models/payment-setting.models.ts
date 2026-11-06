export interface BankInfo {
  id?: number;
  bin: string;
  code: string;
  shortName: string;
  name: string;
  logo?: string;
  transferSupported?: boolean | number;
  lookupSupported?: boolean | number;
}

export interface PaymentSetting {
  id?: string;
  bankBin: string;
  bankName: string;
  accountNumber: string;
  accountName: string;
  transferPrefix: string;
  sampleTransferContent?: string;
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
 * Danh sách 56 ngân hàng & ví điện tử chuẩn Napas247 (VietQR API v2)
 * Sử dụng làm cache ban đầu và danh sách dự phòng khi mạng offline.
 */
export const POPULAR_BANKS: BankInfo[] = [
  { bin: '970422', code: 'MB', shortName: 'MBBank', name: 'Ngân hàng TMCP Quân đội', logo: 'https://api.vietqr.io/img/MB.png' },
  { bin: '970436', code: 'VCB', shortName: 'Vietcombank', name: 'Ngân hàng TMCP Ngoại thương Việt Nam', logo: 'https://api.vietqr.io/img/VCB.png' },
  { bin: '970407', code: 'TCB', shortName: 'Techcombank', name: 'Ngân hàng TMCP Kỹ thương Việt Nam', logo: 'https://api.vietqr.io/img/TCB.png' },
  { bin: '970418', code: 'BIDV', shortName: 'BIDV', name: 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam', logo: 'https://api.vietqr.io/img/BIDV.png' },
  { bin: '970415', code: 'ICB', shortName: 'VietinBank', name: 'Ngân hàng TMCP Công thương Việt Nam', logo: 'https://api.vietqr.io/img/ICB.png' },
  { bin: '970416', code: 'ACB', shortName: 'ACB', name: 'Ngân hàng TMCP Á Châu', logo: 'https://api.vietqr.io/img/ACB.png' },
  { bin: '970432', code: 'VPB', shortName: 'VPBank', name: 'Ngân hàng TMCP Việt Nam Thịnh Vượng', logo: 'https://api.vietqr.io/img/VPB.png' },
  { bin: '970423', code: 'TPB', shortName: 'TPBank', name: 'Ngân hàng TMCP Tiên Phong', logo: 'https://api.vietqr.io/img/TPB.png' },
  { bin: '970403', code: 'STB', shortName: 'Sacombank', name: 'Ngân hàng TMCP Sài Gòn Thương Tín', logo: 'https://api.vietqr.io/img/STB.png' },
  { bin: '970437', code: 'HDB', shortName: 'HDBank', name: 'Ngân hàng TMCP Phát triển TP.HCM', logo: 'https://api.vietqr.io/img/HDB.png' },
  { bin: '970441', code: 'VIB', shortName: 'VIB', name: 'Ngân hàng TMCP Quốc tế', logo: 'https://api.vietqr.io/img/VIB.png' },
  { bin: '970443', code: 'SHB', shortName: 'SHB', name: 'Ngân hàng TMCP Sài Gòn - Hà Nội', logo: 'https://api.vietqr.io/img/SHB.png' },
  { bin: '970405', code: 'VBA', shortName: 'Agribank', name: 'Ngân hàng Nông nghiệp & PTNT Việt Nam', logo: 'https://api.vietqr.io/img/VBA.png' },
  { bin: '970448', code: 'OCB', shortName: 'OCB', name: 'Ngân hàng TMCP Phương Đông', logo: 'https://api.vietqr.io/img/OCB.png' },
  { bin: '970426', code: 'MSB', shortName: 'MSB', name: 'Ngân hàng TMCP Hàng Hải', logo: 'https://api.vietqr.io/img/MSB.png' },
  { bin: '970440', code: 'SEAB', shortName: 'SeABank', name: 'Ngân hàng TMCP Đông Nam Á', logo: 'https://api.vietqr.io/img/SEAB.png' },
  { bin: '970449', code: 'LPB', shortName: 'LPBank', name: 'Ngân hàng TMCP Lộc Phát Việt Nam', logo: 'https://api.vietqr.io/img/LPB.png' },
  { bin: '970431', code: 'EIB', shortName: 'Eximbank', name: 'Ngân hàng TMCP Xuất Nhập khẩu Việt Nam', logo: 'https://api.vietqr.io/img/EIB.png' },
  { bin: '970429', code: 'SCB', shortName: 'SCB', name: 'Ngân hàng TMCP Sài Gòn', logo: 'https://api.vietqr.io/img/SCB.png' },
  { bin: '970409', code: 'BAB', shortName: 'Bac A Bank', name: 'Ngân hàng TMCP Bắc Á', logo: 'https://api.vietqr.io/img/BAB.png' },
  { bin: '970428', code: 'NAB', shortName: 'Nam A Bank', name: 'Ngân hàng TMCP Nam Á', logo: 'https://api.vietqr.io/img/NAB.png' },
  { bin: '970412', code: 'PVCB', shortName: 'PVcomBank', name: 'Ngân hàng TMCP Đại Chúng Việt Nam', logo: 'https://api.vietqr.io/img/PVCB.png' },
  { bin: '970400', code: 'SGB', shortName: 'Saigonbank', name: 'Ngân hàng TMCP Sài Gòn Công Thương', logo: 'https://api.vietqr.io/img/SGB.png' },
  { bin: '970433', code: 'VBB', shortName: 'VietBank', name: 'Ngân hàng TMCP Việt Nam Thương Tín', logo: 'https://api.vietqr.io/img/VBB.png' },
  { bin: '970438', code: 'BVB', shortName: 'BaoViet Bank', name: 'Ngân hàng TMCP Bảo Việt', logo: 'https://api.vietqr.io/img/BVB.png' },
  { bin: '970452', code: 'KLB', shortName: 'Kienlongbank', name: 'Ngân hàng TMCP Kiên Long', logo: 'https://api.vietqr.io/img/KLB.png' },
  { bin: '970454', code: 'VCCB', shortName: 'BVBank', name: 'Ngân hàng TMCP Bản Việt', logo: 'https://api.vietqr.io/img/VCCB.png' },
  { bin: '970408', code: 'GPB', shortName: 'GPBank', name: 'Ngân hàng Thương mại TNHH MTV Dầu Khí Toàn Cầu', logo: 'https://api.vietqr.io/img/GPB.png' },
  { bin: '970414', code: 'OJB', shortName: 'OceanBank', name: 'Ngân hàng Thương mại TNHH MTV Đại Dương', logo: 'https://api.vietqr.io/img/OJB.png' },
  { bin: '970444', code: 'CBB', shortName: 'CBBank', name: 'Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam', logo: 'https://api.vietqr.io/img/CBB.png' },
  { bin: '970425', code: 'ABB', shortName: 'ABBANK', name: 'Ngân hàng TMCP An Bình', logo: 'https://api.vietqr.io/img/ABB.png' },
  { bin: '970430', code: 'PGB', shortName: 'PGBank', name: 'Ngân hàng TMCP Thịnh vượng và Phát triển', logo: 'https://api.vietqr.io/img/PGB.png' },
  { bin: '970442', code: 'SHBVN', shortName: 'Shinhan Bank', name: 'Ngân hàng TNHH MTV Shinhan Việt Nam', logo: 'https://api.vietqr.io/img/SHBVN.png' },
  { bin: '970457', code: 'WOO', shortName: 'Woori Bank', name: 'Ngân hàng TNHH MTV Woori Việt Nam', logo: 'https://api.vietqr.io/img/WOO.png' },
  { bin: '970410', code: 'SCVN', shortName: 'Standard Chartered', name: 'Ngân hàng TNHH MTV Standard Chartered Việt Nam', logo: 'https://api.vietqr.io/img/SCVN.png' },
  { bin: '458761', code: 'HSBC', shortName: 'HSBC', name: 'Ngân hàng TNHH MTV HSBC Việt Nam', logo: 'https://api.vietqr.io/img/HSBC.png' },
  { bin: '533948', code: 'CITIBANK', shortName: 'Citibank', name: 'Ngân hàng Citibank Việt Nam', logo: 'https://api.vietqr.io/img/CITIBANK.png' },
  { bin: '970439', code: 'PBVN', shortName: 'Public Bank', name: 'Ngân hàng TNHH MTV Public Việt Nam', logo: 'https://api.vietqr.io/img/PBVN.png' },
  { bin: '970446', code: 'HLBVN', shortName: 'Hong Leong', name: 'Ngân hàng TNHH MTV Hong Leong Việt Nam', logo: 'https://api.vietqr.io/img/HLBVN.png' },
  { bin: '422589', code: 'CIMB', shortName: 'CIMB', name: 'Ngân hàng TNHH MTV CIMB Việt Nam', logo: 'https://api.vietqr.io/img/CIMB.png' },
  { bin: '970458', code: 'UOB', shortName: 'UOB', name: 'Ngân hàng United Overseas Bank Việt Nam', logo: 'https://api.vietqr.io/img/UOB.png' },
  { bin: '970434', code: 'IVB', shortName: 'Indovina Bank', name: 'Ngân hàng TNHH Indovina', logo: 'https://api.vietqr.io/img/IVB.png' },
  { bin: '970421', code: 'VRB', shortName: 'VRB', name: 'Ngân hàng Liên doanh Việt - Nga', logo: 'https://api.vietqr.io/img/VRB.png' },
  { bin: '970456', code: 'IBKHN', shortName: 'IBK Hà Nội', name: 'Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh Hà Nội', logo: 'https://api.vietqr.io/img/IBK.png' },
  { bin: '970455', code: 'IBKHCM', shortName: 'IBK TP.HCM', name: 'Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh TP.HCM', logo: 'https://api.vietqr.io/img/IBK.png' },
  { bin: '801011', code: 'NHB', shortName: 'Nonghyup', name: 'Ngân hàng Nonghyup - Chi nhánh Hà Nội', logo: 'https://api.vietqr.io/img/NHB.png' },
  { bin: '970462', code: 'KBHN', shortName: 'Kookmin Hà Nội', name: 'Ngân hàng Kookmin - Chi nhánh Hà Nội', logo: 'https://api.vietqr.io/img/KBHN.png' },
  { bin: '970463', code: 'KBHCM', shortName: 'Kookmin TP.HCM', name: 'Ngân hàng Kookmin - Chi nhánh TP.HCM', logo: 'https://api.vietqr.io/img/KBHCM.png' },
  { bin: '970466', code: 'KEBHANAHN', shortName: 'KEB Hana HN', name: 'Ngân hàng KEB Hana - Chi nhánh Hà Nội', logo: 'https://api.vietqr.io/img/KEBHANAHN.png' },
  { bin: '970467', code: 'KEBHANAHCM', shortName: 'KEB Hana HCM', name: 'Ngân hàng KEB Hana - Chi nhánh TP.HCM', logo: 'https://api.vietqr.io/img/KEBHANAHCM.png' },
  { bin: '668888', code: 'KBANK', shortName: 'KBank', name: 'Ngân hàng Kasikornbank - Chi nhánh TP.HCM', logo: 'https://api.vietqr.io/img/KBANK.png' },
  { bin: '970445', code: 'COOPBANK', shortName: 'Co-opBank', name: 'Ngân hàng Hợp tác xã Việt Nam', logo: 'https://api.vietqr.io/img/COOPBANK.png' },
  { bin: '963388', code: 'TIMO', shortName: 'Timo', name: 'Ngân hàng số Timo by BVBank', logo: 'https://api.vietqr.io/img/TIMO.png' },
  { bin: '546034', code: 'CAKE', shortName: 'Cake', name: 'Ngân hàng số Cake by VPBank', logo: 'https://api.vietqr.io/img/CAKE.png' },
  { bin: '971005', code: 'VTLMONEY', shortName: 'Viettel Money', name: 'Tổng Công ty Dịch vụ Số Viettel', logo: 'https://api.vietqr.io/img/VIETTELMONEY.png' },
  { bin: '971011', code: 'VNPTMONEY', shortName: 'VNPT Money', name: 'Tập đoàn Bưu chính Viễn thông Việt Nam', logo: 'https://api.vietqr.io/img/VNPTMONEY.png' }
];
