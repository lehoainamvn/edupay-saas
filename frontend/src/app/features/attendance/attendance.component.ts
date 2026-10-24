import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

interface StudentItem {
  id: string;
  name: string;
  nickname: string;
  status: 'PRESENT' | 'EXCUSED' | 'ABSENT';
}

@Component({
  selector: 'app-attendance',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding: 16px; max-width: 600px; margin: 0 auto;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <div>
          <h2 style="font-size: 20px; font-weight: 700;">Điểm danh Lớp Toán 9A</h2>
          <p style="font-size: 13px; color: #64748b;">Hôm nay: 24/10/2026</p>
        </div>
        <span style="background: #dbeafe; color: #1e40af; padding: 4px 10px; border-radius: 999px; font-size: 13px; font-weight: 600;">
          {{ presentCount() }}/{{ students().length }} có mặt
        </span>
      </div>

      <div style="display: flex; flex-direction: column; gap: 10px;">
        @for (s of students(); track s.id) {
          <div style="background: white; border: 1px solid #e2e8f0; border-radius: 12px; padding: 12px 16px; display: flex; justify-content: space-between; align-items: center;">
            <div>
              <div style="font-weight: 600; font-size: 15px;">{{ s.name }}</div>
              <div style="font-size: 12px; color: #64748b; font-style: italic;">{{ s.nickname }}</div>
            </div>

            <div style="display: flex; gap: 6px;">
              <button 
                (click)="setStatus(s, 'PRESENT')"
                [style.background]="s.status === 'PRESENT' ? '#22c55e' : '#f1f5f9'"
                [style.color]="s.status === 'PRESENT' ? 'white' : '#475569'"
                style="border: none; border-radius: 8px; padding: 8px 12px; font-weight: 600; font-size: 13px; cursor: pointer;">
                Có mặt
              </button>
              <button 
                (click)="setStatus(s, 'EXCUSED')"
                [style.background]="s.status === 'EXCUSED' ? '#eab308' : '#f1f5f9'"
                [style.color]="s.status === 'EXCUSED' ? 'white' : '#475569'"
                style="border: none; border-radius: 8px; padding: 8px 12px; font-weight: 600; font-size: 13px; cursor: pointer;">
                Phép
              </button>
              <button 
                (click)="setStatus(s, 'ABSENT')"
                [style.background]="s.status === 'ABSENT' ? '#ef4444' : '#f1f5f9'"
                [style.color]="s.status === 'ABSENT' ? 'white' : '#475569'"
                style="border: none; border-radius: 8px; padding: 8px 12px; font-weight: 600; font-size: 13px; cursor: pointer;">
                Vắng
              </button>
            </div>
          </div>
        }
      </div>

      <div style="margin-top: 20px;">
        <button style="width: 100%; padding: 14px; background: #2563eb; color: white; border: none; border-radius: 10px; font-size: 16px; font-weight: 700; cursor: pointer;">
          Lưu buổi học
        </button>
      </div>
    </div>
  `
})
export class AttendanceComponent {
  students = signal<StudentItem[]>([
    { id: '1', name: 'Nguyễn Hoàng Nam', nickname: 'Bé Bống', status: 'PRESENT' },
    { id: '2', name: 'Trần Minh Quân', nickname: 'Tít', status: 'PRESENT' },
    { id: '3', name: 'Lê Thuỳ Dung', nickname: 'Nhím', status: 'EXCUSED' },
    { id: '4', name: 'Phạm Đức Anh', nickname: 'Cún', status: 'PRESENT' }
  ]);

  presentCount = () => this.students().filter(s => s.status === 'PRESENT').length;

  setStatus(student: StudentItem, status: 'PRESENT' | 'EXCUSED' | 'ABSENT') {
    this.students.update(list => list.map(s => s.id === student.id ? { ...s, status } : s));
  }
}
