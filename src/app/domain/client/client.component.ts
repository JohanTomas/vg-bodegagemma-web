import { Component, OnInit, ViewChild } from '@angular/core';
import Swal from 'sweetalert2';
import { ClientService } from '../../core/services/client.service';
import { MatDialog } from '@angular/material/dialog';
import { ClientModalComponent } from './client-modal/client-modal.component';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import 'jspdf-autotable';

@Component({
  selector: 'app-client',
  templateUrl: './client.component.html',
  styleUrl: './client.component.scss',
})
export class ClientComponent implements OnInit {

  public displayedColumns: string[] = ['typeDocument', 'numberDocument', 'names', 'lastName', 'email', 'cellPhone', 'birthdate', 'editar-eliminar'];
  public dataSourceC: MatTableDataSource<any>;
  public clients: any[] = [];
  // Paginador
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  cant: number = 0;
  // Ordenación (Sort)
  @ViewChild(MatSort) sort!: MatSort;
  // Botones de activos e inactivos
  public showInactiveButton: boolean = true;
  public showActiveButton: boolean = false;
  public showingInactiveClients: boolean = false;

  constructor(private clientService: ClientService, private dialog: MatDialog) {
    this.dataSourceC = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    console.log('OnInit');
    this.clientService.clientActualizar.subscribe(() => {
      this.getClients();
      this.dataSourceC.sort = this.sort;
    });
    this.getClients();
  }

  // Lista de clientes activos con paginador
  getClients() {
    this.clientService.listPageable(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.clients = res.content;
        this.dataSourceC.data = this.clients;
        this.dataSourceC.sort = this.sort;
      }
    );
  }

  // Lista de clientes inactivos con paginador
  getClientsInactive() {
    this.clientService.listPageableI(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.clients = res.content;
        this.dataSourceC.data = this.clients;
      }
    );
  }

  // Paginador
  Paginator(e: any) {
    if (this.showingInactiveClients) {
      this.clientService.listPageableI(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.clients = res.content;
          this.dataSourceC.data = this.clients;
        }
      );
    } else {
      this.clientService.listPageable(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.clients = res.content;
          this.dataSourceC.data = this.clients;
        }
      );
    }
  }

  // Cambiar botones y listar clientes activos e inactivos
  toggleButtons() {
    this.showingInactiveClients = !this.showingInactiveClients;
    if (this.showingInactiveClients) {
      this.getClientsInactive();
    } else {
      this.getClients();
    }
    this.showInactiveButton = !this.showInactiveButton;
    this.showActiveButton = !this.showActiveButton;
  }

  // Eliminado lógico de un cliente
  eliminar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de eliminar a este cliente?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, elimínalo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Eliminado!",
          text: "Su cliente ha sido eliminado.",
          icon: "success"
        });
        this.fnEliminar(Id);
      }
    });
  }

  // Función Eliminar
  fnEliminar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;
    this.clientService.disableClientById(Id).subscribe(() => {
      this.clientService.listPageable(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.clients = res.content;
        this.dataSourceC.data = this.clients;
        if (this.clients.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Activar cliente
  activar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de restaurar a este cliente?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, restauralo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Restaurado!",
          text: "Su cliente ha sido restaurado.",
          icon: "success"
        });
        this.fnActivar(Id);
      }
    });
  }

  // Función Activar cliente
  fnActivar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;
    this.clientService.activateClientById(Id).subscribe(() => {
      this.clientService.listPageableI(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.clients = res.content;
        this.dataSourceC.data = this.clients;
        if (this.clients.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Abrir formulario al editar
  openmodal(cliente?: any) {
    this.dialog.open(ClientModalComponent, {
      disableClose: true,
      width: '40rem',
      height: 'auto',
      data: cliente,
    });
  }

  // Filtrado
  filtrar(event: any) {
    const valor = event.target.value;
    this.dataSourceC.filter = valor.trim().toLowerCase();
  }

  // Método para exportar clientes a PDF
  exportPDF() {
    const doc = new jsPDF();
    const columns = ["Tip. Doc.", "Num. Doc.", "Nombre", "Apellido", "Email", "Num. Celular", "Fech. Nac."];
    const rows = this.dataSourceC.data.map(client => [
      client.typeDocument, client.numberDocument, client.names, client.lastName, client.email, client.cellPhone, client.birthdateFormatted
    ]);

    (doc as any).autoTable(columns, rows);
    doc.save('lista_clientes.pdf');
  }

  // Método para exportar clientes a Excel
  exportExcel() {
    const filteredData = this.dataSourceC.data.map(({ typeDocument, numberDocument, names, lastName, email, cellPhone, birthdateFormatted }) => ({
      'Tip. Doc.': typeDocument,
      'Num. Doc.': numberDocument,
      'Nombre': names,
      'Apellido': lastName,
      'Email': email,
      'Num. Celular': cellPhone,
      'Fech. Nac.': birthdateFormatted,
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Clientes');
    XLSX.writeFile(wb, 'Clientes.xlsx');
  }

  // Método para exportar clientes a CSV
  exportCSV() {
    const filteredData = this.dataSourceC.data.map(({ typeDocument, numberDocument, names, lastName, email, cellPhone, birthdateFormatted }) => ({
      'Tip. Doc.': typeDocument,
      'Num. Doc.': numberDocument,
      'Nombre': names,
      'Apellido': lastName,
      'Email': email,
      'Num. Celular': cellPhone,
      'Fech. Nac.': birthdateFormatted,
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const csv = XLSX.utils.sheet_to_csv(ws);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'Clientes.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

}