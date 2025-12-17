import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ClientService {
  clientActualizar = new Subject<any[]>();

  constructor(private http: HttpClient) { }

  checkDocumentExists(numberDocument: string): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}/api/v1/clients/exists?numberDocument=${numberDocument}`);
  }
  // Eliminar cliente lógicamente por ID
  disableClientById(clientId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/clients/disable/${clientId}`, {});
  }
  //Activar cliente por ID
  activateClientById(clientId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/clients/activate/${clientId}`, {});
  }
  // Editar cliente por ID
  updateClientById(clientId: number, updatedClient: any) {
    return this.http.put(`${environment.apiUrl}/api/v1/clients/${clientId}`, updatedClient);
  }
  // Insertar cliente
  newClient(newClient: any) {
    return this.http.post(`${environment.apiUrl}/api/v1/clients`, newClient);
  }
  // Listar paginador activos
  listPageable(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/clients/active/pageable?page=${pag}&size=${tam}`);
  }
  // Listar paginador inactivos
  listPageableI(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/clients/inactive/pageable?page=${pag}&size=${tam}`);
  }
  //Listado de clientes activos
  listActiveClients() {
    return this.http.get(`${environment.apiUrl}/api/v1/clients/active`);
  }
  //Listado de cliebtes inactivos
  listInactiveClients() {
    return this.http.get(`${environment.apiUrl}/api/v1/clients/inactive`);
  }
  //REPORTES
  reportActivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/clients/report`,  { responseType: 'blob' });
  }
  reportInactivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/clients/inactive/report`,  { responseType: 'blob' });
  }
  reportActiveExcel() {
    return this.http.get(`${environment.apiUrl}/api/v1/clients/report/excel`,  { responseType: 'blob' });
  }
  reportInactiveExcel() {
    return this.http.get(`${environment.apiUrl}/api/v1/clients/inactive/report/excel`,  { responseType: 'blob' });
  }

}
