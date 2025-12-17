import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SupplierService {
  supplierActualizar = new Subject<any[]>();
  constructor(private http: HttpClient) { }

  checkDocumentExists(numberDocument: string): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}/api/v1/suppliers/exists?numberDocument=${numberDocument}`);
  }
  // Eliminar proveedor lógicamente por ID
  disableSupplierById(supplierId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/suppliers/disable/${supplierId}`, {});
  }
  //Activar proveedor por ID
  activateSupplierById(supplierId: number) {
    return this.http.put(`${environment.apiUrl}/api/v1/suppliers/activate/${supplierId}`, {});
  }
  // Editar proveedor por ID
  updateSupplierById(supplierId: number, updatedSupplier: any) {
    return this.http.put(`${environment.apiUrl}/api/v1/suppliers/${supplierId}`, updatedSupplier);
  }
  // Insertar proveedor
  newSupplier(newSupplier: any) {
    return this.http.post(`${environment.apiUrl}/api/v1/suppliers`, newSupplier);
  }
  // Listar paginador activos
  listPageable(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/suppliers/active/pageable?page=${pag}&size=${tam}`);
  }
  // Listar paginador inactivos
  listPageableI(pag: number, tam: number) {
    return this.http.get(`${environment.apiUrl}/api/v1/suppliers/inactive/pageable?page=${pag}&size=${tam}`);
  }
  //Listado de proveedor activos
  listActiveSuppliers() {
    return this.http.get(`${environment.apiUrl}/api/v1/suppliers/active`);
  }
  //Listado de proveedor inactivos
  listInactiveSuppliers() {
    return this.http.get(`${environment.apiUrl}/api/v1/suppliers/inactive`);
  }

  //REPORTES
  reportActivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/suppliers/report`,  { responseType: 'blob' });
  }
  reportInactivePDF() {
    return this.http.get(`${environment.apiUrl}/api/v1/suppliers/inactive/report`,  { responseType: 'blob' });
  }
  reportActiveExcel() {
    return this.http.get(`${environment.apiUrl}/api/v1/suppliers/report/excel`,  { responseType: 'blob' });
  }
  reportInactiveExcel() {
    return this.http.get(`${environment.apiUrl}/api/v1/suppliers/inactive/report/excel`,  { responseType: 'blob' });
  }

}
