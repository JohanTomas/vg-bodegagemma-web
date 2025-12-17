import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {
  reservationActualizar = new Subject<any[]>();

  constructor(private http: HttpClient) { }

  // Eliminar reserva lógicamente por ID
  disableReservationById(reservationId: number) {
    return this.http.put(`${environment.apiUrl}/api/reservations/delete/${reservationId}`, {});
  }

  // Activar reserva por ID
  activateReservationById(reservationId: number) {
    return this.http.put(`${environment.apiUrl}/api/reservations/activate/${reservationId}`, {});
  }

  // Editar reserva por ID
  updateReservationById(reservationId: number, updatedReservation: any) {
    return this.http.put(`${environment.apiUrl}/api/reservations/${reservationId}`, updatedReservation);
  }

  // Insertar reserva
  newReservation(newReservation: any) {
    return this.http.post(`${environment.apiUrl}/api/reservations`, newReservation);
  }

  // Listar paginador activos
  listPageable(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/reservations/status/A/page?page=${pag}&size=${tam}`);
  }

  // Listar paginador inactivos
  listPageableI(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/reservations/status/I/page?page=${pag}&size=${tam}`);
  }

  // Listado de reservas activas
  listActiveReservations() {
    return this.http.get(`${environment.apiUrl}/api/reservations/status/A`);
  }

  // Listado de reservas inactivas
  listInactiveReservations() {
    return this.http.get(`${environment.apiUrl}/api/reservations/status/I`);
  }

   // Obtener detalle de una reserva por ID
   getReservationById(reservationId: number): Observable<any> {
    return this.http.get(`${environment.apiUrl}/api/reservations/${reservationId}`);
  }

  // REPORTES
  reportActivePDF(reservationId: number) {
    return this.http.get(`${environment.apiUrl}/api/reservations/report/${reservationId}`, { responseType: 'blob' });
  }

  reportActiveExcel(reservationId: number) {
    return this.http.get(`${environment.apiUrl}/api/reservations/report/excel/${reservationId}`, { responseType: 'blob' });
  }
}