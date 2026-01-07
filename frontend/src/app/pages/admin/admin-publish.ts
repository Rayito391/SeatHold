import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-admin-publish',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-publish.html',
  styleUrl: './admin-publish.css'
})
export class AdminPublishComponent {
  private readonly apiBase = '';
  protected readonly message = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly events = signal<EventResponse[]>([]);
  protected readonly selectedId = signal<string>('');

  constructor(private readonly http: HttpClient) {
    this.loadEvents();
  }

  loadEvents() {
    this.loading.set(true);
    this.http
      .get<ApiResponse<EventResponse[]>>(`${this.apiBase}/api/admin/events`, {
        headers: this.authHeaders()
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          this.events.set(res.data ?? []);
          if (res.data && res.data.length > 0) {
            this.selectedId.set(res.data[0].id);
          }
        },
        error: (err) => this.message.set(this.extractError(err))
      });
  }

  publishEvent() {
    const eventId = this.selectedId();
    if (!eventId) {
      this.message.set('Selecciona un evento.');
      return;
    }
    this.loading.set(true);
    this.http
      .post<ApiResponse<any>>(`${this.apiBase}/api/admin/events/${eventId}/publish`, {}, {
        headers: this.authHeaders()
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => this.message.set('Evento publicado.'),
        error: (err) => this.message.set(this.extractError(err))
      });
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

type EventResponse = {
  id: string;
  status: string;
  title: string;
  description: string | null;
  venue: string;
  city: string;
  startsAt: string;
  endsAt: string | null;
  totalCapacity: number;
};
