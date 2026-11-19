import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Mesa, Producto } from '../models/restaurant.models';

interface ReservaNueva {
  mesaId: number;
  nombreContacto: string;
  telefonoContacto?: string;
  personas: number;
  /** ISO 8601 con zona horaria, p. ej. 2026-12-01T20:30:00-06:00. */
  inicio: string;
  fin: string;
}

interface ReservaCreada { id: number; estado: string; mensaje: string; }

/** HTTP público; no guardar aquí la llave administrativa. */
@Injectable({ providedIn: 'root' })
export class MesaCeroApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  obtenerCatalogo(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.base}/catalogo`);
  }

  obtenerMesas(): Observable<Mesa[]> {
    return this.http.get<Mesa[]>(`${this.base}/estado-mesas`);
  }

  registrarReserva(datos: ReservaNueva): Observable<ReservaCreada> {
    return this.http.post<ReservaCreada>(`${this.base}/reservas`, datos);
  }
}
