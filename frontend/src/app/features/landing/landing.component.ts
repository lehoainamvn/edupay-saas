import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent {
  openFaq = signal<number>(0);
  menuOpen = signal<boolean>(false);

  people = ['NT', 'PH', 'ML', 'QV'];

  students = [
    { name: 'Minh Anh', avatar: 'MA', status: 'Có mặt', cls: 'present' },
    { name: 'Gia Huy', avatar: 'GH', status: 'Có phép', cls: 'excused' },
    { name: 'Bảo Ngọc', avatar: 'BN', status: 'Có mặt', cls: 'present' },
    { name: 'Đức Minh', avatar: 'DM', status: 'Vắng', cls: 'absent' }
  ];

  qrIndexes = new Set([0, 1, 2, 7, 9, 13, 14, 16, 20, 22, 24, 26, 28, 30, 32, 34, 36, 40, 42, 43, 44, 46, 48]);
  qrGrid = Array.from({ length: 49 }, (_, i) => i);

  oldWay = [
    'Ghi sổ tay nhầm buổi, thiếu buổi',
    'Nhắn Zalo đòi tiền phụ huynh thấy ngại',
    'Chuyển khoản ghi biệt danh, không biết của ai',
    'Phải soi từng dòng sao kê ngân hàng'
  ];

  newWay = [
    'Điểm danh 30 giây xong cả lớp',
    'Link phiếu thu chuyên nghiệp, không cần mở lời',
    'VietQR tự điền đúng tiền và tên học sinh',
    'Tự động gạch nợ ngay khi nhận tiền'
  ];

  features = [
    {
      id: '01',
      tag: 'ĐIỂM DANH',
      title: 'Điểm danh 1 chạm siêu tốc trên di động',
      copy: 'Thiết kế cho màn hình cảm ứng. Chạm đổi màu Xanh / Vàng / Đỏ, hoàn thành cả lớp trong chưa đầy 30 giây.',
      points: ['Tự nhớ lịch học và danh sách lớp', 'Tính học phí theo số buổi thực tế'],
      visual: 'attendance'
    },
    {
      id: '02',
      tag: 'THANH TOÁN',
      title: 'VietQR Động Napas247 chuẩn định danh',
      copy: 'Chính xác từng đồng, tự động điền cú pháp chuyển khoản. Phụ huynh chỉ việc mở app ngân hàng, quét và xác nhận.',
      points: ['Tiền về thẳng tài khoản hiện có', 'Đối soát đúng học sinh, đúng tháng'],
      visual: 'bank'
    },
    {
      id: '03',
      tag: 'MINH BẠCH',
      title: 'Cổng tra cứu riêng cho Phụ huynh',
      copy: 'Gửi một đường link qua Zalo để phụ huynh xem chi tiết từng ngày con đi học — không cần tải app hay tạo tài khoản.',
      points: ['Cập nhật thời gian thực', 'Bảo mật bằng liên kết định danh riêng'],
      visual: 'portal'
    }
  ];

  plans = [
    {
      name: 'Dùng thử',
      price: '0đ',
      unit: '/ 14 ngày',
      desc: 'Trải nghiệm trọn vẹn trước khi quyết định.',
      features: ['Đầy đủ tính năng', 'Tối đa 2 lớp', 'Tối đa 30 học sinh'],
      popular: false
    },
    {
      name: 'Thuê tháng',
      price: '59.000đ',
      unit: '/ tháng',
      desc: 'Linh hoạt cho giáo viên đang phát triển.',
      features: ['Không giới hạn lớp & học sinh', 'Tự động tính học phí', 'VietQR động định danh'],
      popular: true
    },
    {
      name: 'Thuê năm',
      price: '499.000đ',
      unit: '/ năm',
      desc: 'Chỉ ~41k/tháng, tiết kiệm 29%.',
      features: ['Tất cả tính năng gói tháng', 'Xuất báo cáo Excel / PDF', 'Gạch nợ tự động'],
      popular: false
    }
  ];

  faqs = [
    {
      q: 'Tôi có cần phải mở tài khoản ngân hàng mới không?',
      a: 'Không. Tiền học phí chuyển thẳng 100% vào tài khoản ngân hàng hiện có của thầy cô.'
    },
    {
      q: 'Phụ huynh có cần tải app hay đăng ký tài khoản không?',
      a: 'Hoàn toàn không. Phụ huynh chỉ cần bấm vào link Zalo là xem được phiếu và quét QR ngay.'
    },
    {
      q: 'Dữ liệu học sinh của tôi có được bảo mật không?',
      a: 'Có. Hệ thống Multi-Tenant mã hóa và phân tách độc lập dữ liệu của từng giáo viên.'
    }
  ];

  timelineItems = [
    { date: '03/05', status: 'Có mặt' },
    { date: '07/05', status: 'Có mặt' },
    { date: '10/05', status: 'Có phép' },
    { date: '14/05', status: 'Có mặt' }
  ];

  isQrActive(index: number): boolean {
    return this.qrIndexes.has(index);
  }

  toggleFaq(index: number) {
    this.openFaq.update(current => (current === index ? -1 : index));
  }

  toggleMenu() {
    this.menuOpen.update(v => !v);
  }
}
