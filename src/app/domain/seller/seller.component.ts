import { Component, OnInit, ViewChild } from '@angular/core';
import Swal from 'sweetalert2';
import { SellerService } from './../../core/services/seller.service';
import { MatDialog } from '@angular/material/dialog';
import { SellerModalComponent } from './seller-modal/seller-modal.component';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import 'jspdf-autotable';

@Component({
  selector: 'app-seller',
  templateUrl: './seller.component.html',
  styleUrl: './seller.component.scss'
})
export class SellerComponent implements OnInit {
  public displayedColumns: string[] = ['typeDocument', 'numberDocument', 'names', 'lastName', 'email', 'cellPhone', 'salary', 'rol', 'user', 'password', 'editar-eliminar'];
  public dataSourceV: MatTableDataSource<any>;
  public sellers: any[] = [];
  // Paginador
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  cant: number = 0;
  // Ordenación (Sort)
  @ViewChild(MatSort) sort!: MatSort;
  // Botones de activos e inactivos
  public showInactiveButton: boolean = true;
  public showActiveButton: boolean = false;
  public showingInactiveSellers: boolean = false;

  constructor(private sellerService: SellerService, private dialog: MatDialog) {
    this.dataSourceV = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    console.log('OnInit');
    this.sellerService.sellerActualizar.subscribe(() => {
      this.getSellers();
      this.dataSourceV.sort = this.sort;
    });
    this.getSellers();
  }

  // Lista de vendedores activos con paginador
  getSellers() {
    this.sellerService.listPageable(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.sellers = res.content;
        this.dataSourceV.data = this.sellers;
        this.dataSourceV.sort = this.sort;
      }
    );
  }

  // Lista de vendedores inactivos con paginador
  getSellersInactive() {
    this.sellerService.listPageableI(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.sellers = res.content;
        this.dataSourceV.data = this.sellers;
      }
    );
  }

  // Paginador
  Paginator(e: any) {
    if (this.showingInactiveSellers) {
      this.sellerService.listPageableI(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.sellers = res.content;
          this.dataSourceV.data = this.sellers;
        }
      );
    } else {
      this.sellerService.listPageable(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.sellers = res.content;
          this.dataSourceV.data = this.sellers;
        }
      );
    }
  }

  // Cambiar botones y listar vendedores activos e inactivos
  toggleButtons() {
    this.showingInactiveSellers = !this.showingInactiveSellers;
    if (this.showingInactiveSellers) {
      this.getSellersInactive();
    } else {
      this.getSellers();
    }
    this.showInactiveButton = !this.showInactiveButton;
    this.showActiveButton = !this.showActiveButton;
  }

  // Eliminado lógico de un vendedor
  eliminar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de eliminar a este vendedor?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, elimínalo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Eliminado!",
          text: "Su vendedor ha sido eliminado.",
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

    this.sellerService.disableSellerById(Id).subscribe(() => {
      this.sellerService.listPageable(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.sellers = res.content;
        this.dataSourceV.data = this.sellers;

        if (this.sellers.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Activar vendedor
  activar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de restaurar a este vendedor?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, restauralo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Restaurado!",
          text: "Su vendedor ha sido restaurado.",
          icon: "success"
        });
        this.fnActivar(Id);
      }
    });
  }

  // Función Activar vendedor
  fnActivar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;

    this.sellerService.activateSellerById(Id).subscribe(() => {
      this.sellerService.listPageableI(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.sellers = res.content;
        this.dataSourceV.data = this.sellers;

        if (this.sellers.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Abrir formulario al editar
  openmodal(vendedor?: any) {
    this.dialog.open(SellerModalComponent, {
      disableClose: true,
      width: '40rem',
      height: 'auto',
      data: vendedor,
    });
  }

  // Filtrado
  filtrar(event: any) {
    const valor = event.target.value;
    this.dataSourceV.filter = valor.trim().toLowerCase();
  }

  // Método para exportar vendedores a PDF
  exportPDF() {
    const doc = new jsPDF();
    const columns = ["Tip. Doc.", "Num. Doc.", "Nombre", "Apellido", "Email", "Num. Celular", "Salario", "Rol", "Usuario", "Contraseña"];
    const rows = this.dataSourceV.data.map(seller => [
      seller.typeDocument, seller.numberDocument, seller.names, seller.lastName, seller.email, seller.cellPhone, seller.salary, seller.sellerRol, seller.sellerUser, seller.sellerPassword
    ]);

    (doc as any).autoTable(columns, rows);
    doc.save('lista_vendedores.pdf');
  }

  // Método para exportar vendedores a Excel
  exportExcel() {
    const filteredData = this.dataSourceV.data.map(({ typeDocument, numberDocument, names, lastName, email, cellPhone, salary, sellerRol, sellerUser, sellerPassword }) => ({
      'Tipo de Documento': typeDocument,
      'Número de Documento': numberDocument,
      'Nombres': names,
      'Apellidos': lastName,
      'Correo Electrónico': email,
      'Teléfono Celular': cellPhone,
      'Salario': salary,
      'Rol': sellerRol,
      'Usuario': sellerUser,
      'Contraseña': sellerPassword
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Vendedores');
    XLSX.writeFile(wb, 'Vendedores.xlsx');
  }

  // Método para exportar vendedores a CSV
  exportCSV() {
    const filteredData = this.dataSourceV.data.map(({ typeDocument, numberDocument, names, lastName, email, cellPhone, salary, sellerRol, sellerUser, sellerPassword }) => ({
      'Tipo de Documento': typeDocument,
      'Número de Documento': numberDocument,
      'Nombres': names,
      'Apellidos': lastName,
      'Correo Electrónico': email,
      'Teléfono Celular': cellPhone,
      'Salario': salary,
      'Rol': sellerRol,
      'Usuario': sellerUser,
      'Contraseña': sellerPassword
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const csv = XLSX.utils.sheet_to_csv(ws);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'Vendedores.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}