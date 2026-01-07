import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-admin-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-create.html',
  styleUrl: './admin-create.css'
})
export class AdminCreateComponent {
  private readonly apiBase = '';
  protected readonly message = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly http: HttpClient
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required]],
      description: [''],
      venue: ['', [Validators.required]],
      city: ['', [Validators.required]],
      startsAt: ['', [Validators.required]],
      endsAt: [''],
      totalCapacity: [1, [Validators.required, Validators.min(1)]]
    });
  }

  createEvent() {
    if (this.form.invalid) {
      this.message.set('Completa los campos requeridos.');
      return;
    }
    this.loading.set(true);
    const payload = this.form.getRawValue();
    this.http
      .post<ApiResponse<any>>(`${this.apiBase}/api/admin/events`, payload, {
        headers: this.authHeaders()
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.message.set('Evento creado.');
          this.form.reset({
            title: '',
            description: '',
            venue: '',
            city: '',
            startsAt: '',
            endsAt: '',
            totalCapacity: 1
          });
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
