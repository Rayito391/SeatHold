import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-reservations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reservations.html',
  styleUrl: './reservations.css'
})
export class ReservationsComponent {
  private readonly apiBase = '';
  protected readonly reservations = signal<ReservationResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly status = signal<string>('');

  constructor(private readonly http: HttpClient) {
    this.loadReservations();
  }

  loadReservations() {
    this.loading.set(true);
    this.message.set(null);
    const filter = this.status();
    const query = filter ? `?status=${encodeURIComponent(filter)}` : '';
    this.http
      .get<ApiResponse<Page<ReservationResponse>>>(
        `${this.apiBase}/api/me/reservations${query}`,
        { headers: this.authHeaders() }
      )
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.reservations.set(res.data?.content ?? []);
        },
        error: (err) => this.message.set(this.extractError(err))
      });
  }

  setStatus(value: string) {
    this.status.set(value);
    this.loadReservations();
  }

  private authHeaders() {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      Authorization: token ? `Bearer ${token}` : ''
    });
  }

  private extractError(err: any): string {
    return err?.error?.data?.message || err?.error?.message || 'Error inesperado.';
  }
}

type ApiResponse<T> = {
  meta: { status: string; code: number };
  data: T;
};

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

type ReservationResponse = {
  reservationId: string;
  eventId: string;
  quantity: number;
  status: string;
  expiresAt: string | null;
  createdAt: string;
};
