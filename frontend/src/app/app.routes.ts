import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home';
import { EventsComponent } from './pages/events/events';
import { ReservationsComponent } from './pages/reservations/reservations';
import { AdminCreateComponent } from './pages/admin/admin-create';
import { AdminUpdateComponent } from './pages/admin/admin-update';
import { AdminPublishComponent } from './pages/admin/admin-publish';

export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: 'events', component: EventsComponent },
  { path: 'reservations', component: ReservationsComponent },
  { path: 'admin/create', component: AdminCreateComponent },
  { path: 'admin/update', component: AdminUpdateComponent },
  { path: 'admin/publish', component: AdminPublishComponent },
  { path: '', pathMatch: 'full', redirectTo: 'home' }
];
