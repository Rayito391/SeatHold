import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [CommonModule, ReactiveFormsModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly apiBase = '';
  protected readonly mode = signal<'login' | 'register'>('login');
  protected readonly message = signal<string | null>(null);
  protected readonly token = signal<string | null>(localStorage.getItem('token'));
  protected readonly role = signal<string | null>(localStorage.getItem('role'));
  protected readonly loading = signal(false);

  protected readonly loginForm;
  protected readonly registerForm;

  constructor(
    private readonly fb: FormBuilder,
    private readonly http: HttpClient,
    private readonly router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });

    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    if (this.token()) {
      this.router.navigateByUrl('/home');
    }
  }

  setMode(mode: 'login' | 'register') {
    this.mode.set(mode);
    this.message.set(null);
  }

  submitLogin() {
    if (this.loginForm.invalid) {
      this.loading.set(false);
      this.message.set('Complete los campos de login.');
      return;
    }
    this.loading.set(true);
    const payload = this.loginForm.getRawValue();
    this.http
      .post<ApiResponse<LoginResponse>>(`${this.apiBase}/api/auth/login`, payload)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
      next: (res) => {
        const token = res.data?.token ?? null;
        if (token) {
          localStorage.setItem('token', token);
          this.token.set(token);
          const role = res.data?.role ?? null;
          if (role) {
            localStorage.setItem('role', role);
            this.role.set(role);
          }
          this.message.set('Login correcto.');
          this.router.navigateByUrl('/home');
        } else {
          this.message.set('Login sin token.');
        }
      },
      error: (err) => {
        this.message.set(this.extractError(err));
      }
    });
  }

  submitRegister() {
    if (this.registerForm.invalid) {
      this.loading.set(false);
      this.message.set('Complete los campos de registro.');
      return;
    }
    this.loading.set(true);
    const payload = this.registerForm.getRawValue();
    this.http
      .post<ApiResponse<RegisterResponse>>(`${this.apiBase}/api/auth/register`, payload)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
      next: (res) => {
        if (res.data?.email) {
          this.message.set('Registro correcto. Ahora inicia sesion.');
          this.setMode('login');
        } else {
          this.message.set('Registro sin respuesta.');
        }
      },
      error: (err) => {
        this.message.set(this.extractError(err));
      }
    });
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    this.token.set(null);
    this.role.set(null);
    this.message.set('Sesion cerrada.');
    this.router.navigateByUrl('/');
  }

  private extractError(err: any): string {
    return err?.error?.data?.message || err?.error?.message || 'Error inesperado.';
  }
}

type ApiResponse<T> = {
  meta: { status: string; code: number };
  data: T;
};

type LoginResponse = {
  token: string;
  userId: string;
  email: string;
  firstName: string;
  role: string;
};

type RegisterResponse = {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  message: string;
};
