import { Component, OnInit, ViewChild } from '@angular/core';
import Swal from 'sweetalert2';
import { SupplierService } from './../../core/services/supplier.service';
import { MatDialog } from '@angular/material/dialog';
import { SupplierModalComponent } from './supplier-modal/supplier-modal.component';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import 'jspdf-autotable';

@Component({
  selector: 'app-supplier',
  templateUrl: './supplier.component.html',
  styleUrl: './supplier.component.scss'
})
export class SupplierComponent implements OnInit {
  public displayedColumns: string[] = ['ruc', 'nameCompany', 'typeDocument', 'numberDocument', 'names', 'lastName', 'email', 'cellPhone', 'editar-eliminar'];
  public dataSourceS: MatTableDataSource<any>;
  public suppliers: any[] = [];
  // Paginador
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  cant: number = 0;
  // Ordenación (Sort)
  @ViewChild(MatSort) sort!: MatSort;
  // Botones de activos e inactivos
  public showInactiveButton: boolean = true;
  public showActiveButton: boolean = false;
  public showingInactiveSuppliers: boolean = false;

  constructor(private supplierService: SupplierService, private dialog: MatDialog) {
    this.dataSourceS = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    console.log('OnInit');
    this.supplierService.supplierActualizar.subscribe(() => {
      this.getSuppliers();
      this.dataSourceS.sort = this.sort;
    });
    this.getSuppliers();
  }

  // Lista de proveedores activos con paginador
  getSuppliers() {
    this.supplierService.listPageable(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.suppliers = res.content;
        this.dataSourceS.data = this.suppliers;
        this.dataSourceS.sort = this.sort;
      }
    );
  }

  // Lista de proveedores inactivos con paginador
  getSuppliersInactive() {
    this.supplierService.listPageableI(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.suppliers = res.content;
        this.dataSourceS.data = this.suppliers;
      }
    );
  }

  // Paginador
  Paginator(e: any) {
    if (this.showingInactiveSuppliers) {
      this.supplierService.listPageableI(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.suppliers = res.content;
          this.dataSourceS.data = this.suppliers;
        }
      );
    } else {
      this.supplierService.listPageable(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.suppliers = res.content;
          this.dataSourceS.data = this.suppliers;
        }
      );
    }
  }

  // Cambiar botones y listar proveedores activos e inactivos
  toggleButtons() {
    this.showingInactiveSuppliers = !this.showingInactiveSuppliers;
    if (this.showingInactiveSuppliers) {
      this.getSuppliersInactive();
    } else {
      this.getSuppliers();
    }
    this.showInactiveButton = !this.showInactiveButton;
    this.showActiveButton = !this.showActiveButton;
  }

  // Eliminado lógico de un proveedor
  eliminar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de eliminar a este proveedor?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, elimínalo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Eliminado!",
          text: "Su proveedor ha sido eliminado.",
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

    this.supplierService.disableSupplierById(Id).subscribe(() => {
      this.supplierService.listPageable(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.suppliers = res.content;
        this.dataSourceS.data = this.suppliers;

        if (this.suppliers.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Activar proveedor
  activar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de restaurar a este proveedor?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, restauralo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Restaurado!",
          text: "Su proveedor ha sido restaurado.",
          icon: "success"
        });
        this.fnActivar(Id);
      }
    });
  }

  // Función Activar proveedor
  fnActivar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;

    this.supplierService.activateSupplierById(Id).subscribe(() => {
      this.supplierService.listPageableI(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.suppliers = res.content;
        this.dataSourceS.data = this.suppliers;

        if (this.suppliers.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Abrir formulario al editar
  openmodal(proveedor?: any) {
    this.dialog.open(SupplierModalComponent, {
      disableClose: true,
      width: '40rem',
      height: 'auto',
      data: proveedor,
    });
  }

  // Filtrado
  filtrar(event: any) {
    const valor = event.target.value;
    this.dataSourceS.filter = valor.trim().toLowerCase();
  }

  // Método para exportar proveedores a PDF
  exportPDF() {
    const doc = new jsPDF();
    const columns = ["RUC", "Nombre Empresa", "Tipo Doc.", "Num. Doc.", "Nombre", "Apellido", "Email", "Num. Celular"];
    const rows = this.dataSourceS.data.map(supplier => [
      supplier.ruc, supplier.nameCompany, supplier.typeDocument, supplier.numberDocument, supplier.names, supplier.lastName, supplier.email, supplier.cellPhone
    ]);

    (doc as any).autoTable(columns, rows);
    doc.save('lista_proveedores.pdf');
  }

  // Método para exportar proveedores a Excel
  exportExcel() {
    const filteredData = this.dataSourceS.data.map(({ ruc, nameCompany, typeDocument, numberDocument, names, lastName, email, cellPhone }) => ({
      'RUC': ruc,
      'Nombre Empresa': nameCompany,
      'Tipo de Documento': typeDocument,
      'Número de Documento': numberDocument,
      'Nombres': names,
      'Apellidos': lastName,
      'Correo Electrónico': email,
      'Teléfono Celular': cellPhone
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Proveedores');
    XLSX.writeFile(wb, 'Proveedores.xlsx');
  }

  // Método para exportar proveedores a CSV
  exportCSV() {
    const filteredData = this.dataSourceS.data.map(({ ruc, nameCompany, typeDocument, numberDocument, names, lastName, email, cellPhone }) => ({
      'RUC': ruc,
      'Nombre Empresa': nameCompany,
      'Tipo de Documento': typeDocument,
      'Número de Documento': numberDocument,
      'Nombres': names,
      'Apellidos': lastName,
      'Correo Electrónico': email,
      'Teléfono Celular': cellPhone
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const csv = XLSX.utils.sheet_to_csv(ws);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'Proveedores.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}