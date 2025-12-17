import { ReservationModalComponent } from './reservation-modal/reservation-modal.component';
import { ReservationService } from './../../core/services/reservation.service';
import { Component, OnInit, ViewChild } from '@angular/core';
import Swal from 'sweetalert2';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import 'jspdf-autotable';

@Component({
  selector: 'app-reservation',
  templateUrl: './reservation.component.html',
  styleUrl: './reservation.component.scss'
})
export class ReservationComponent implements OnInit {
  public displayedColumns: string[] = ['id', 'client', 'seller', 'paymentMethod', 'date', 'total', 'editar-eliminar'];
  public dataSourceR: MatTableDataSource<any>;
  public reservations: any[] = [];
  // Paginador
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  cant: number = 0;
  // Ordenación (Sort)
  @ViewChild(MatSort) sort!: MatSort;
  // Botones de activos e inactivos
  public showInactiveButton: boolean = true;
  public showActiveButton: boolean = false;
  public showingInactiveReservations: boolean = false;

  constructor(private reservationService: ReservationService, private dialog: MatDialog) {
    this.dataSourceR = new MatTableDataSource<any>([]);
  }

  ngOnInit(): void {
    console.log('OnInit');
    this.reservationService.reservationActualizar.subscribe(() => {
      this.getReservations();
      this.dataSourceR.sort = this.sort;
    });
    this.getReservations();
  }

  // Lista de reservas activas con paginador
  getReservations() {
    this.reservationService.listPageable(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.reservations = res.content;
        this.dataSourceR.data = this.reservations;
        this.dataSourceR.sort = this.sort;
      }
    );
  }

  // Lista de reservas inactivas con paginador
  getReservationsInactive() {
    this.reservationService.listPageableI(0, 10).subscribe(
      (res: any) => {
        console.log(res);
        this.cant = res.totalElements;
        this.reservations = res.content;
        this.dataSourceR.data = this.reservations;
      }
    );
  }

  // Paginador
  Paginator(e: any) {
    if (this.showingInactiveReservations) {
      this.reservationService.listPageableI(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.reservations = res.content;
          this.dataSourceR.data = this.reservations;
        }
      );
    } else {
      this.reservationService.listPageable(e.pageIndex, e.pageSize).subscribe(
        (res: any) => {
          console.log(res);
          this.cant = res.totalElements;
          this.reservations = res.content;
          this.dataSourceR.data = this.reservations;
        }
      );
    }
  }

  // Cambiar botones y listar reservas activas e inactivas
  toggleButtons() {
    this.showingInactiveReservations = !this.showingInactiveReservations;
    if (this.showingInactiveReservations) {
      this.getReservationsInactive();
    } else {
      this.getReservations();
    }
    this.showInactiveButton = !this.showInactiveButton;
    this.showActiveButton = !this.showActiveButton;
  }

  // Eliminado lógico de una reserva
  eliminar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de eliminar esta reserva?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, elimínalo!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Eliminado!",
          text: "Su reserva ha sido eliminada.",
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

    this.reservationService.disableReservationById(Id).subscribe(() => {
      this.reservationService.listPageable(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.reservations = res.content;
        this.dataSourceR.data = this.reservations;

        if (this.reservations.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Activar reserva
  activar(Id: number) {
    Swal.fire({
      title: "¿Estás seguro de restaurar esta reserva?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#3085d6",
      cancelButtonColor: "#d33",
      confirmButtonText: "Sí, restaurala!"
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire({
          title: "¡Restaurado!",
          text: "Su reserva ha sido restaurada.",
          icon: "success"
        });
        this.fnActivar(Id);
      }
    });
  }

  // Función Activar reserva
  fnActivar(Id: number) {
    const pageIndex = this.paginator.pageIndex;
    const pageSize = this.paginator.pageSize;

    this.reservationService.activateReservationById(Id).subscribe(() => {
      this.reservationService.listPageableI(pageIndex, pageSize).subscribe((res: any) => {
        this.cant = res.totalElements;
        this.reservations = res.content;
        this.dataSourceR.data = this.reservations;

        if (this.reservations.length === 0 && pageIndex > 0) {
          this.paginator.previousPage();
        }
      });
    });
  }

  // Abrir formulario al editar
  openmodal(reservation?: any) {
    this.dialog.open(ReservationModalComponent, {
      disableClose: true,
      width: '1200px',
      height: 'auto',
      data: reservation,
    });
  }

  // Filtrado
  filtrar(event: any) {
    const valor = event.target.value;
    this.dataSourceR.filter = valor.trim().toLowerCase();
  }

  // Método para exportar reservas a PDF
  exportPDF() {
    const doc = new jsPDF();
    const columns = ["ID", "Cliente", "Vendedor", "Método de Pago", "Fecha", "Total"];
    const rows = this.dataSourceR.data.map(reservation => [
      reservation.id, reservation.clientNames, reservation.sellerNames, reservation.paymentMethod.name, reservation.formattedDateTime, reservation.totalReservation
    ]);

    (doc as any).autoTable(columns, rows);
    doc.save('lista_reservas.pdf');
  }

  // Método para exportar reservas a Excel
  exportExcel() {
    const filteredData = this.dataSourceR.data.map(({ id, clientNames, sellerNames, paymentMethod, formattedDateTime, totalReservation }) => ({
      'ID': id,
      'Cliente': clientNames,
      'Vendedor': sellerNames,
      'Método de Pago': paymentMethod.name,
      'Fecha': formattedDateTime,
      'Total': totalReservation
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Reservas');
    XLSX.writeFile(wb, 'Reservas.xlsx');
  }

  // Método para exportar reservas a CSV
  exportCSV() {
    const filteredData = this.dataSourceR.data.map(({ id, clientNames, sellerNames, paymentMethod, formattedDateTime, totalReservation }) => ({
      'ID': id,
      'Cliente': clientNames,
      'Vendedor': sellerNames,
      'Método de Pago': paymentMethod.name,
      'Fecha': formattedDateTime,
      'Total': totalReservation
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(filteredData);
    const csv = XLSX.utils.sheet_to_csv(ws);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'Reservas.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  // Ver detalle de la reserva
  verDetalle(Id: number) {
    this.reservationService.getReservationById(Id).subscribe((reservation: any) => {
      // Calcular los subtotales
      reservation.reservationDetails.forEach((detail: any) => {
        detail.subtotal = this.calculateItemTotal(detail); // Usar la función calculateItemTotal para calcular el subtotal
      });

      Swal.fire({
        title: '<strong>Detalle de la Reserva</strong>',
        html: `
          <div id="reservation-detail" style="text-align: left; font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px; background-color: #f9f9f9;">
            <h3 style="text-align: center; margin-bottom: 20px;">Factura Electrónica</h3>
            <p><strong>ID:</strong> ${reservation.id}</p>
            <p><strong>Cliente:</strong> ${reservation.clientNames}</p>
            <p><strong>Vendedor:</strong> ${reservation.sellerNames}</p>
            <p><strong>Método de Pago:</strong> ${reservation.paymentMethod.name}</p>
            <p><strong>Fecha:</strong> ${reservation.formattedDateTime}</p>
            <p><strong>Total:</strong> S/.${reservation.totalReservation}</p>
            <hr style="margin: 20px 0;">
            <h4>Productos</h4>
            <table style="width: 100%; border-collapse: collapse;">
              <thead>
                <tr>
                  <th style="border: 1px solid #ddd; padding: 8px;">Producto</th>
                  <th style="border: 1px solid #ddd; padding: 8px;">Precio</th>
                  <th style="border: 1px solid #ddd; padding: 8px;">Cantidad</th>
                  <th style="border: 1px solid #ddd; padding: 8px;">Unidad de Venta</th>
                  <th style="border: 1px solid #ddd; padding: 8px;">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                ${reservation.reservationDetails.map((detail: { product: { name: any; priceUnit: any; unitSale: any; }; amount: any; subtotal: any; }) => `
                  <tr>
                    <td style="border: 1px solid #ddd; padding: 8px;">${detail.product.name}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">S/.${detail.product.priceUnit.toFixed(2)}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">${detail.amount}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">${detail.product.unitSale}</td>
                    <td style="border: 1px solid #ddd; padding: 8px;">S/.${detail.subtotal.toFixed(2)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
            <hr style="margin: 20px 0;">
            <p style="text-align: center; font-size: 12px; color: #888;">Gracias por su compra</p>
          </div>
        `,
        icon: 'info',
        showCancelButton: true,
        confirmButtonText: 'Cerrar',
        cancelButtonText: 'Descargar PDF',
        showDenyButton: true,
        denyButtonText: 'Descargar Excel',
        customClass: {
          popup: 'swal-wide',
          title: 'swal-title',
          confirmButton: 'swal-button',
          cancelButton: 'swal-button',
          denyButton: 'swal-button'
        },
        width: '800px' // Aumentar el ancho del modal
      }).then((result) => {
        if (result.isDismissed && result.dismiss === Swal.DismissReason.cancel) {
          this.downloadPDF(reservation);
        } else if (result.isDenied) {
          this.downloadExcel(reservation);
        }
      });
    });
  }

  // Función para calcular el subtotal de un producto
  calculateItemTotal(detail: any): number {
    const product = detail.product;
    const amount = detail.amount;
    const total = (product?.priceUnit || 0) * (amount || 0);
    return Number(total.toFixed(2)); // Redondear a dos decimales
  }

  // Descargar detalle en PDF
  downloadPDF(reservation: any) {
    const doc = new jsPDF();
    doc.setFontSize(18);
    doc.setTextColor(40, 40, 40);
    doc.text('Factura Electrónica', 105, 20, { align: 'center' });

    doc.setFontSize(12);
    doc.setTextColor(0, 102, 204);
    doc.text('Nombre del Cliente:', 20, 40);
    doc.setTextColor(0, 0, 0);
    doc.text(`${reservation.clientNames}`, 70, 40);

    doc.setTextColor(0, 102, 204);
    doc.text('Nombre del Vendedor:', 20, 50);
    doc.setTextColor(0, 0, 0);
    doc.text(`${reservation.sellerNames}`, 70, 50);

    doc.setTextColor(0, 102, 204);
    doc.text('Método de Pago:', 20, 60);
    doc.setTextColor(0, 0, 0);
    doc.text(`${reservation.paymentMethod.name}`, 70, 60);

    // Mover la fecha a la parte superior derecha
    doc.setTextColor(0, 0, 0);
    doc.text(`Fecha: ${reservation.formattedDateTime}`, 200, 30, { align: 'right' });

    doc.setFontSize(14);
    doc.setTextColor(40, 40, 40);
    doc.text('Detalle de la Reserva', 105, 90, { align: 'center' });

    const columns = ["Producto", "Precio", "Cantidad", "Unidad de Venta", "Subtotal"];
    const rows = reservation.reservationDetails.map((detail: any) => [
      detail.product.name,
      `S/.${detail.product.priceUnit.toFixed(2)}`,
      detail.amount,
      detail.product.unitSale,
      `S/.${detail.subtotal.toFixed(2)}`
    ]);

    (doc as any).autoTable({
      startY: 100,
      head: [columns],
      body: rows,
      theme: 'grid',
      headStyles: { fillColor: [22, 160, 133] },
      styles: { halign: 'center' }
    });

    const finalY = (doc as any).lastAutoTable.finalY || 10;
    doc.setFontSize(12);
    doc.setTextColor(0, 0, 0);
    doc.text('Gracias por su compra', 105, finalY + 20, { align: 'center' });

    // Cuadro para el total
    doc.setFillColor(255, 255, 255);
    doc.setDrawColor(0, 0, 0);
    doc.rect(140, finalY + 10, 60, 10, 'FD');
    doc.setTextColor(0, 0, 0);
    doc.text(`Total: S/.${reservation.totalReservation}`, 170, finalY + 18, { align: 'right' });

    doc.save('detalle_reserva.pdf');
  }

  // Descargar detalle en Excel
  downloadExcel(reservation: any) {
    const data = reservation.reservationDetails.map((detail: any) => ({
      'Cliente': reservation.clientNames,
      'Vendedor': reservation.sellerNames,
      'Método de Pago': reservation.paymentMethod.name,
      'Fecha': reservation.formattedDateTime,
      'Producto': detail.product.name,
      'Precio': detail.product.priceUnit.toFixed(2),
      'Cantidad': detail.amount,
      'Unidad de Venta': detail.product.unitSale,
      'Subtotal': detail.subtotal.toFixed(2),
      'Total': reservation.totalReservation
    }));

    const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(data);
    const wb: XLSX.WorkBook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Detalle de Reserva');

    // Aplicar estilos al archivo Excel
    const wscols = [
      { wch: 20 }, // Cliente
      { wch: 20 }, // Vendedor
      { wch: 20 }, // Método de Pago
      { wch: 15 }, // Fecha
      { wch: 20 }, // Producto
      { wch: 10 }, // Precio
      { wch: 10 }, // Cantidad
      { wch: 15 }, // Unidad de Venta
      { wch: 15 }, // Subtotal
      { wch: 10 }  // Total
    ];
    ws['!cols'] = wscols;

    // Centrar los títulos
    const merge = [
      { s: { r: 0, c: 0 }, e: { r: 0, c: 9 } }, // Título
      { s: { r: 1, c: 0 }, e: { r: 1, c: 9 } }, // Subtítulo
    ];
    ws['!merges'] = merge;

    XLSX.writeFile(wb, 'detalle_reserva.xlsx');
  }
}