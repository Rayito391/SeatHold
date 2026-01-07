import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './events.html',
  styleUrl: './events.css'
})
export class EventsComponent {
  private readonly apiBase = '';
  protected readonly events = signal<EventCard[]>([]);
  protected readonly loading = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly quantities = signal<Record<string, number>>({});
  protected readonly holds = signal<Record<string, HoldState>>({});

  constructor(private readonly http: HttpClient) {
    this.loadEvents();
  }

  loadEvents() {
    this.loading.set(true);
    this.message.set(null);
    this.http
      .get<ApiResponse<Page<EventResponse>>>(`${this.apiBase}/api/events`)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res) => {
          const content = res.data?.content ?? [];
          const cards = content.map((e) => ({
            ...e,
            availableSeats: null
          }));
          this.events.set(cards);
          cards.forEach((event) => this.loadDetail(event.id));
        },
        error: (err) => this.message.set(this.extractError(err))
      });
  }

  loadDetail(eventId: string) {
    this.http
      .get<ApiResponse<EventDetailResponse>>(`${this.apiBase}/api/events/${eventId}`)
      .subscribe({
        next: (res) => {
          const detail = res.data;
          if (!detail) return;
          this.events.set(
            this.events().map((e) =>
              e.id === eventId ? { ...e, availableSeats: detail.availableSeats } : e
            )
          );
        }
      });
  }

  setQuantity(eventId: string, value: string) {
    const parsed = Number(value);
    const qty = Number.isNaN(parsed) ? 1 : Math.max(0, parsed);
    this.quantities.set({ ...this.quantities(), [eventId]: qty });
  }

  getQuantity(eventId: string) {
    return this.quantities()[eventId] ?? 1;
  }

  hold(eventId: string) {
    const quantity = this.getQuantity(eventId);
    if (quantity < 1) {
      this.message.set('Cantidad debe ser mayor a 0.');
      return;
    }
    this.http
      .post<ApiResponse<HoldResponse>>(
        `${this.apiBase}/api/events/${eventId}/holds`,
        { quantity },
        { headers: this.authHeaders() }
      )
      .subscribe({
        next: (res) => {
          const hold = res.data;
          if (!hold) return;
          this.holds.set({
            ...this.holds(),
            [eventId]: {
              reservationId: hold.reservationId,
              status: hold.status,
              expiresAt: hold.expiresAt
            }
          });
          this.loadDetail(eventId);
        },
        error: (err) => this.message.set(this.extractError(err))
      });
  }

  confirm(eventId: string) {
    const hold = this.holds()[eventId];
    if (!hold) return;
    this.http
      .post<ApiResponse<ReservationStatusResponse>>(
        `${this.apiBase}/api/reservations/${hold.reservationId}/confirm`,
        {},
        { headers: this.authHeaders() }
      )
      .subscribe({
        next: () => {
          this.holds.set({ ...this.holds(), [eventId]: undefined as any });
          this.quantities.set({ ...this.quantities(), [eventId]: 1 });
          this.loadDetail(eventId);
        },
        error: (err) => this.message.set(this.extractError(err))
      });
  }

  cancel(eventId: string) {
    const hold = this.holds()[eventId];
    if (!hold) return;
    this.http
      .post<ApiResponse<ReservationStatusResponse>>(
        `${this.apiBase}/api/reservations/${hold.reservationId}/cancel`,
        {},
        { headers: this.authHeaders() }
      )
      .subscribe({
        next: () => {
          this.holds.set({ ...this.holds(), [eventId]: undefined as any });
          this.quantities.set({ ...this.quantities(), [eventId]: 1 });
          this.loadDetail(eventId);
        },
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

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

type EventResponse = {
  id: string;
  status: string;
  title: string;
  description: string;
  venue: string;
  city: string;
  startsAt: string;
  endsAt: string;
  totalCapacity: number;
};

type EventDetailResponse = EventResponse & { availableSeats: number | null };

type HoldResponse = {
  reservationId: string;
  status: string;
  expiresAt: string;
};

type ReservationStatusResponse = {
  reservationId: string;
  status: string;
};

type EventCard = EventResponse & { availableSeats: number | null };

type HoldState = {
  reservationId: string;
  status: string;
  expiresAt: string;
};
