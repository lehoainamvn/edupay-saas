import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding: 24px; max-width: 1000px; margin: 0 auto;">
      <h2 style="font-size: 24px; font-weight: 700;">Quản lý Học sinh</h2>
      <p style="color: #64748b; margin-top: 4px;">Danh sách học sinh, biệt danh và số điện thoại phụ huynh</p>
    </div>
  `
})
export class StudentsComponent {}
