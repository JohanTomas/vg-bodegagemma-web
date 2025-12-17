import { SellerService } from './../../../core/services/seller.service';
import { ClientService } from './../../../core/services/client.service';
import { ProductService } from './../../../core/services/product.service';
import { SaleService } from './../../../core/services/sale.service';
import { Component, OnInit, Inject, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl, ValidatorFn, AbstractControl, FormGroupDirective, NgForm, FormArray } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import Swal from 'sweetalert2';
import { MatPaginator } from '@angular/material/paginator';
import { ErrorStateMatcher } from '@angular/material/core';
import { startWith, map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { PaymentMethodService } from '../../../core/services/payment-method.service';

export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const isSubmitted = form && form.submitted;
    return !!(control && control.invalid && (control.dirty || control.touched || isSubmitted));
  }
}

@Component({
  selector: 'app-sale-modal',
  templateUrl: './sale-modal.component.html',
  styleUrls: ['./sale-modal.component.scss']
})
export class SaleModalComponent implements OnInit {
  saleForm: FormGroup = new FormGroup({});
  errorMessage: string = '';
  sale: any;
  paymentMethod: any[] = [];
  product: any[] = [];
  client: any[] = [];
  seller: any[] = [];
  filteredClients: any[] = [];
  allClients: any[] = [];
  filteredSellers: any[] = [];
  allSellers: any[] = [];
  filteredProducts: any[] = [];
  allProducts: any[] = [];
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  matcher = new MyErrorStateMatcher();

  constructor(
    private dialogRef: MatDialogRef<SaleModalComponent>,
    private fb: FormBuilder,
    private saleService: SaleService,
    private productService: ProductService,
    private clientService: ClientService,
    private sellerService: SellerService,
    private paymentMethodService: PaymentMethodService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    this.initSaleForm();
    console.log('DATA:', this.data);
    this.sale = { ...this.data };
    this.productService.listActiveProducts().subscribe((data: any) => {
      this.product = data;
      this.allProducts = data;
      this.filteredProducts = data;
    });
    this.clientService.listActiveClients().subscribe((data: any) => {
      this.client = data;
      this.allClients = data;
      this.filteredClients = data;
    });
    this.sellerService.listActiveSellers().subscribe((data: any) => {
      this.seller = data;
      this.allSellers = data;
      this.filteredSellers = data;
    });
    this.paymentMethodService.listActivePaymentMethod().subscribe((data: any) => {
      this.paymentMethod = data;
    });

    // Suscripción al cambio en el campo de cliente para actualizar el autocompletado
    this.saleForm.get('client')?.valueChanges.pipe(
      startWith(''),
      map(value => typeof value === 'string' ? value : value.names) // Necesario si se busca por nombre completo
    ).subscribe(clientName => {
      if (typeof clientName === 'string') {
        this.filterClients(clientName); // Filtrar clientes según lo que se escriba
      }
    });

    // Suscripción al cambio en el campo de vendedor para actualizar el autocompletado
    this.saleForm.get('seller')?.valueChanges.pipe(
      startWith(''),
      map(value => typeof value === 'string' ? value : value.names) // Necesario si se busca por nombre completo
    ).subscribe(sellerName => {
      if (typeof sellerName === 'string') {
        this.filterSellers(sellerName); // Filtrar vendedores según lo que se escriba
      }
    });

    // Configurar valueChanges para cada producto en saleDetails
    this.saleDetails.controls.forEach((control, index) => {
      control.get('product')?.valueChanges.pipe(
        startWith(''),
        map(value => typeof value === 'string' ? value : value.name)
      ).subscribe(productName => {
        if (typeof productName === 'string') {
          this.filterProducts(productName);
        }
      });
    });
  }

  initSaleForm() {
    this.saleForm = this.fb.group({
      client: ['', [Validators.required]],
      seller: ['', [Validators.required]],
      paymentMethod: ['', [Validators.required]],
      saleDetails: this.fb.array([])
    });

    if (this.data) {
      this.saleForm.get('client')?.setValue(this.data.client);
      this.saleForm.get('seller')?.setValue(this.data.seller);

      // Verificar si el método de pago está definido en los datos recibidos
      if (this.data.paymentMethod) {
        this.saleForm.get('paymentMethod')?.setValue(this.data.paymentMethod);
      }

      this.setSaleDetails(this.data.saleDetails);
    }
  }


  get form() {
    return this.saleForm.controls;
  }

  get saleDetails(): FormArray {
    return this.saleForm.get('saleDetails') as FormArray;
  }

  newSaleDetail(): FormGroup {
    return this.fb.group({
      product: ['', [Validators.required]],
      amount: ['', [Validators.required, Validators.min(1)]]
    });
  }

  addProduct(): void {
    const control = this.newSaleDetail();
    this.saleDetails.push(control);

    control.get('product')?.valueChanges.pipe(
      startWith(''),
      map(value => typeof value === 'string' ? value : value.name)
    ).subscribe(productName => {
      if (typeof productName === 'string') {
        this.filterProducts(productName);
      }
    });
  }

  removeProduct(index: number): void {
    this.saleDetails.removeAt(index);
  }

  saveSale() {
    if (this.saleForm.invalid) {
      this.saleForm.markAllAsTouched();
      return;
    }

    // Verificar que haya al menos un detalle de venta
    if (this.saleDetails.length === 0) {
      Swal.fire({
        title: "Error",
        text: "Debe agregar al menos un producto antes de registrar la venta.",
        icon: "error",
        confirmButtonText: "Aceptar"
      });
      return;
    }

    const saleData = this.prepareSaveData();

    if (this.data) {
      this.updateSale();
    } else {
      this.registerSale();
    }
  }

  prepareSaveData(): any {
    const formModel = this.saleForm.value;

    const saleDetailsDeepCopy = formModel.saleDetails.map((detail: any) => ({
      id: detail.id, // Agregar el ID si existe
      product: {
        id: detail.product.id
      },
      amount: detail.amount
    }));

    return {
      client: {
        id: formModel.client.id
      },
      seller: {
        id: formModel.seller.id
      },
      paymentMethod: {
        id: formModel.paymentMethod.id
      },

      saleDetails: saleDetailsDeepCopy
    };
  }

  registerSale() {
    if (this.saleForm.invalid) {
      this.saleForm.markAllAsTouched();
      return;
    }

    const saleData = this.prepareSaveData();

    this.saleService.newSale(this.saleForm.value).subscribe((res) => {
      console.log('Respuesta registrar venta:', res);
      this.clearForm(); // Limpia el formulario
      this.showInsertNotification();
      this.clearProducts();
      this.closeDialog(); // Muestra la notificación
    });
  }

  clearForm() {
    this.saleForm.reset(); // Resetea el formulario
    // Limpiar errores específicos de cliente y vendedor
    this.saleForm.get('client')?.setErrors(null);
    this.saleForm.get('seller')?.setErrors(null);
    this.saleForm.get('paymentMethod')?.setErrors(null);
  }


  updateSale() {
    const saleData = this.prepareSaveData();

    // Encontrar los detalles de venta existentes
    const existingSaleDetails = this.data.saleDetails.map((detail: any) => ({
      id: detail.id,
      product: { id: detail.product.id },
      amount: detail.amount
    }));

    // Actualizar los detalles de venta existentes
    saleData.saleDetails = saleData.saleDetails.map((newDetail: any, index: number) => {
      const existingDetail = existingSaleDetails[index];
      if (existingDetail) {
        return {
          id: existingDetail.id,
          product: { id: newDetail.product.id },
          amount: newDetail.amount
        };
      } else {
        return newDetail;
      }
    });

    this.saleService.updateSaleById(this.data.id, saleData).subscribe((res) => {
      console.log('Respuesta actualizar venta:', res);
      this.showEditNotification();
      this.closeDialog(true);
    });
  }

  showInsertNotification(): void {
    Swal.fire({
      title: "Venta Insertada!",
      text: "La Venta se realizó correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.saleService.saleActualizar.next([]);
    });
  }

  showEditNotification(): void {
    Swal.fire({
      title: "Venta Editada!",
      text: "La Venta se ha editado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.saleService.saleActualizar.next([]);
    });
  }

  closeDialog(success?: boolean): void {
    this.dialogRef.close(success);
  }

  calculateTotal(): number {
    const total = this.saleDetails.controls.reduce((total, detail) => {
      const product = detail.get('product')?.value;
      const amount = detail.get('amount')?.value;
      return total + (product?.priceUnit || 0) * (amount || 0);
    }, 0);
    return Number(total.toFixed(2)); // Redondear a dos decimales y convertir de nuevo a número
  }

  calculateItemTotal(detail: AbstractControl): number {
    const product = detail.get('product')?.value;
    const amount = detail.get('amount')?.value;
    const total = (product?.priceUnit || 0) * (amount || 0);
    return Number(total.toFixed(2)); // Redondear a dos decimales
  }

  onProductChange(index: number): void {
    const detail = this.saleDetails.at(index);
    const product = detail.get('product')?.value;
    if (product) {
      this.updateTotal(index);
    }
  }

  updateTotal(index: number): void {
    const detail = this.saleDetails.at(index);
    const product = detail.get('product')?.value;
    let amount = detail.get('amount')?.value;

    if (product && amount !== null && amount !== undefined) {
      const unitSale = product.unitSale.toLowerCase();

      // Verificar la unidad de venta y validar la cantidad
      if (unitSale === 'unidad' && !Number.isInteger(amount)) {
        detail.get('amount')?.setErrors({ 'invalidAmount': true });
      } else if (amount > product.stock) {
        detail.get('amount')?.setErrors({ 'exceededStock': true });
      } else {
        detail.get('amount')?.setErrors(null);
        detail.get('total')?.setValue((product.priceUnit || 0) * amount);
      }

      // Actualizar el total general de la venta
      const total = this.calculateTotal();
      this.saleForm.get('total')?.setValue(total);
    }
  }



  setSaleDetails(saleDetails: any[]): void {
    const detailsFormArray = this.saleForm.get('saleDetails') as FormArray;
    saleDetails.forEach((detail: any) => {
      detailsFormArray.push(
        this.fb.group({
          id: [detail.id], // Agregar el ID del detalle de venta
          product: [detail.product, Validators.required],
          amount: [detail.amount, [Validators.required, Validators.min(1)]]
        })
      );
    });
  }

  clearProducts() {
    // Eliminar todos los detalles de venta
    while (this.saleDetails.length !== 0) {
      this.removeProduct(0);
    }
  }

  // Método para mostrar el nombre completo del cliente en el autocompletado
  displayClient(client: any): string {
    return client && client.names && client.lastName ? `${client.names} ${client.lastName}` : '';
  }

  // Método para filtrar clientes según lo que se escribe en el campo
  filterClients(value: string): void {
    const filterValue = value.toLowerCase();
    this.filteredClients = this.allClients.filter(c =>
      c.names.toLowerCase().includes(filterValue) || c.lastName.toLowerCase().includes(filterValue)
    );
  }

  // Método para mostrar el nombre completo del vendedor en el autocompletado
  displaySeller(seller: any): string {
    return seller && seller.names && seller.lastName ? `${seller.names} ${seller.lastName}` : '';
  }

  // Método para filtrar vendedores según lo que se escribe en el campo
  filterSellers(value: string): void {
    const filterValue = value.toLowerCase();
    this.filteredSellers = this.allSellers.filter(s =>
      s.names.toLowerCase().includes(filterValue) || s.lastName.toLowerCase().includes(filterValue)
    );
  }

  filterProducts(value: string): void {
    const filterValue = value.toLowerCase();
    this.filteredProducts = this.allProducts.filter(p =>
      p.name.toLowerCase().includes(filterValue)
    );
  }

  displayProduct(product: any): string {
    return product && product.name ? product.name : '';
  }

}
