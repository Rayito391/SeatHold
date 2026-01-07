import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-admin-update',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-update.html',
  styleUrl: './admin-update.css'
})
export class AdminUpdateComponent {
  private readonly apiBase = '';
  protected readonly message = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly events = signal<EventResponse[]>([]);
  protected readonly selectedId = signal<string>('');
  protected readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly http: HttpClient
  ) {
    this.form = this.fb.group({
      title: [''],
      description: [''],
      venue: [''],
      city: [''],
      startsAt: [''],
      endsAt: [''],
      totalCapacity: ['']
    });
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
            this.selectEvent(res.data[0].id);
          }
        },
        error: (err) => this.message.set(this.extractError(err))
      });
  }

  selectEvent(eventId: string) {
    this.selectedId.set(eventId);
    const event = this.events().find((e) => e.id === eventId);
    if (!event) return;
    this.form.patchValue({
      title: event.title,
      description: event.description ?? '',
      venue: event.venue,
      city: event.city,
      startsAt: this.toInputDate(event.startsAt),
      endsAt: event.endsAt ? this.toInputDate(event.endsAt) : '',
      totalCapacity: String(event.totalCapacity)
    });
  }

  updateEvent() {
    const eventId = this.selectedId();
    if (!eventId) {
      this.message.set('Selecciona un evento.');
      return;
    }
    this.loading.set(true);
    const raw = this.form.getRawValue();
    const payload = {
      title: this.emptyToNull(raw.title),
      description: this.emptyToNull(raw.description),
      venue: this.emptyToNull(raw.venue),
      city: this.emptyToNull(raw.city),
      startsAt: this.emptyToNull(raw.startsAt),
      endsAt: this.emptyToNull(raw.endsAt),
      totalCapacity: this.emptyToNumber(raw.totalCapacity)
    };
    this.http
      .put<ApiResponse<any>>(`${this.apiBase}/api/admin/events/${eventId}`, payload, {
        headers: this.authHeaders()
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => this.message.set('Evento actualizado.'),
        error: (err) => this.message.set(this.extractError(err))
      });
  }

  private toInputDate(value: string) {
    return value ? value.slice(0, 16) : '';
  }

  private authHeaders() {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      Authorization: token ? `Bearer ${token}` : ''
    });
  }

  private emptyToNull(value: string | null | undefined) {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    return value;
  }

  private emptyToNumber(value: string | null | undefined) {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
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
