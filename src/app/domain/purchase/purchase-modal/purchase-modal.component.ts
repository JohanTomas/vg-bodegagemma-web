import { SellerService } from '../../../core/services/seller.service';
import { SupplierService } from '../../../core/services/supplier.service';
import { ProductService } from '../../../core/services/product.service';
import { PurchaseService } from '../../../core/services/purchase.service';
import { Component, OnInit, Inject, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl, ValidatorFn, AbstractControl, FormGroupDirective, NgForm, FormArray } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import Swal from 'sweetalert2';
import { MatPaginator } from '@angular/material/paginator';
import { ErrorStateMatcher } from '@angular/material/core';
import { startWith, map } from 'rxjs/operators';
import { PaymentMethodService } from '../../../core/services/payment-method.service';

export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const isSubmitted = form && form.submitted;
    return !!(control && control.invalid && (control.dirty || control.touched || isSubmitted));
  }
}

@Component({
  selector: 'app-purchase-modal',
  templateUrl: './purchase-modal.component.html',
  styleUrl: './purchase-modal.component.scss'
})
export class PurchaseModalComponent implements OnInit {
  purchaseForm: FormGroup = new FormGroup({});
  errorMessage: string = '';
  purchase: any;
  product: any[] = [];
  paymentMethod: any[] = [];
  seller: any[] = [];
  supplier: any[] = [];
  filteredSuppliers: any[] = [];
  allSuppliers: any[] = [];
  filteredSellers: any[] = [];
  allSellers: any[] = [];
  filteredProducts: any[] = [];
  allProducts: any[] = [];
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  matcher = new MyErrorStateMatcher();

  constructor(
    private dialogRef: MatDialogRef<PurchaseModalComponent>,
    private fb: FormBuilder,
    private purchaseService: PurchaseService,
    private productService: ProductService,
    private supplierService: SupplierService,
    private sellerService: SellerService,
    private paymentMethodService: PaymentMethodService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    this.initpurchaseForm();
    console.log('DATA:', this.data);
    this.purchase = { ...this.data };
    this.productService.listActiveProducts().subscribe((data: any) => {
      this.product = data;
      this.allProducts = data;
      this.filteredProducts = data;
    });
    this.supplierService.listActiveSuppliers().subscribe((data: any) => {
      this.supplier = data;
      this.allSuppliers = data;
      this.filteredSuppliers = data;
    });
    this.sellerService.listActiveSellers().subscribe((data: any) => {
      this.seller = data;
      this.allSellers = data;
      this.filteredSellers = data;
    });
    this.paymentMethodService.listActivePaymentMethod().subscribe((data: any) => {
      this.paymentMethod = data;
    });

    // Suscripción al cambio en el campo de suppliere para actualizar el autocompletado
    this.purchaseForm.get('supplier')?.valueChanges.pipe(
      startWith(''),
      map(value => typeof value === 'string' ? value : value.names) // Necesario si se busca por nombre completo
    ).subscribe(supplierName => {
      if (typeof supplierName === 'string') {
        this.filterSuppliers(supplierName); // Filtrar supplieres según lo que se escriba
      }
    });

    // Suscripción al cambio en el campo de vendedor para actualizar el autocompletado
    this.purchaseForm.get('seller')?.valueChanges.pipe(
      startWith(''),
      map(value => typeof value === 'string' ? value : value.names) // Necesario si se busca por nombre completo
    ).subscribe(sellerName => {
      if (typeof sellerName === 'string') {
        this.filterSellers(sellerName); // Filtrar vendedores según lo que se escriba
      }
    });

    // Configurar valueChanges para cada producto en saleDetails
    this.purchaseDetails.controls.forEach((control, index) => {
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

  initpurchaseForm() {
    this.purchaseForm = this.fb.group({
      supplier: ['', [Validators.required]],
      seller: ['', [Validators.required]],
      paymentMethod: ['', [Validators.required]],
      purchaseDetails: this.fb.array([])
    });

    if (this.data) {
      this.purchaseForm.get('supplier')?.setValue(this.data.supplier);
      this.purchaseForm.get('seller')?.setValue(this.data.seller);

      // Verificar si el método de pago está definido en los datos recibidos
      if (this.data.paymentMethod) {
        this.purchaseForm.get('paymentMethod')?.setValue(this.data.paymentMethod);
      }
      this.setpurchaseDetails(this.data.purchaseDetails);
    }
  }
  get form() {
    return this.purchaseForm.controls;
  }
  get purchaseDetails(): FormArray {
    return this.purchaseForm.get('purchaseDetails') as FormArray;
  }

  newpurchaseDetail(): FormGroup {
    return this.fb.group({
      product: ['', [Validators.required]],
      priceUnit: ['', [Validators.required, this.priceFormatValidator]],
      amount: ['', [Validators.required, Validators.min(1)]]
    });
  }
  //VALIDACION PARA PRECIO DE VENTA y STOCK
  priceFormatValidator(control: FormControl) {
    // Expresión regular para validar el formato del precio
    const priceRegex = /^\d{1,8}(\.\d{1,2})?$/;
    // Validador personalizado para validar el formato del precio
    const valid = priceRegex.test(control.value);
    return valid ? null : { invalidPriceFormat: true };
  }

  addProduct(): void {
    const control = this.newpurchaseDetail();
    this.purchaseDetails.push(control);

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
    this.purchaseDetails.removeAt(index);
  }

  savepurchase() {
    if (this.purchaseForm.invalid) {
      this.purchaseForm.markAllAsTouched();
      return;
    }

    // Verificar que haya al menos un detalle de Compra
    if (this.purchaseDetails.length === 0) {
      Swal.fire({
        title: "Error",
        text: "Debe agregar al menos un producto antes de registrar la compra.",
        icon: "error",
        confirmButtonText: "Aceptar"
      });
      return;
    }

    const purchaseData = this.prepareSaveData();

    if (this.data) {
      this.updatePurchase();
    } else {
      this.registerPurchase();
    }
  }


  prepareSaveData(): any {
    const formModel = this.purchaseForm.value;

    const purchaseDetailsDeepCopy = formModel.purchaseDetails.map((detail: any) => ({
      id: detail.id, // Agregar el ID si existe
      product: {
        id: detail.product.id
      },
      priceUnit: detail.priceUnit,
      amount: detail.amount
    }));

    return {
      supplier: {
        id: formModel.supplier.id
      },
      seller: {
        id: formModel.seller.id
      },
      paymentMethod: {
        id: formModel.paymentMethod.id
      },
      purchaseDetails: purchaseDetailsDeepCopy
    };
  }


  registerPurchase() {
    if (this.purchaseForm.invalid) {
      this.purchaseForm.markAllAsTouched();
      return;
    }

    const purchaseData = this.prepareSaveData();

    this.purchaseService.newPurchase(this.purchaseForm.value).subscribe((res) => {
      console.log('Respuesta registrar Compra:', res);
      this.clearForm(); // Limpia el formulario
      this.showInsertNotification();
      this.clearProducts();
      this.closeDialog();// Muestra la notificación
    });
  }

  clearForm() {
    this.purchaseForm.reset(); // Resetea el formulario
  }


  updatePurchase() {
    const purchaseData = this.prepareSaveData();

    // Encontrar los detalles de Compra existentes
    const existingpurchaseDetails = this.data.purchaseDetails.map((detail: any) => ({
      id: detail.id,
      product: { id: detail.product.id },
      priceUnit: detail.priceUnit,
      amount: detail.amount
    }));

    // Actualizar los detalles de Compra existentes
    purchaseData.purchaseDetails = purchaseData.purchaseDetails.map((newDetail: any, index: number) => {
      const existingDetail = existingpurchaseDetails[index];
      if (existingDetail) {
        return {
          id: existingDetail.id,
          product: { id: newDetail.product.id },
          priceUnit: newDetail.priceUnit,
          amount: newDetail.amount
        };
      } else {
        return newDetail;
      }
    });

    this.purchaseService.updatePurchaseById(this.data.id, purchaseData).subscribe((res) => {
      console.log('Respuesta actualizar Compra:', res);
      this.showEditNotification();
      this.closeDialog(true);
    });
  }


  showInsertNotification(): void {
    Swal.fire({
      title: "Compra Insertada!",
      text: "La Compra se realizó correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.purchaseService.purchaseActualizar.next([]);
    });
  }

  showEditNotification(): void {
    Swal.fire({
      title: "Compra Editada!",
      text: "La Compra se ha editado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.purchaseService.purchaseActualizar.next([]);
    });
  }

  closeDialog(success?: boolean): void {
    this.dialogRef.close(success);
  }

  calculateTotal(): number {
    const total = this.purchaseDetails.controls.reduce((total, detail) => {
      const priceUnit = detail.get('priceUnit')?.value;
      const amount = detail.get('amount')?.value;
      return total + (priceUnit || 0) * (amount || 0);
    }, 0);
    return Number(total.toFixed(2)); // Redondear a dos decimales y convertir de nuevo a número
  }


  calculateItemTotal(detail: AbstractControl): number {
    const priceUnit = detail.get('priceUnit')?.value;
    const amount = detail.get('amount')?.value;
    const total = (priceUnit || 0) * (amount || 0);
    return Number(total.toFixed(2)); // Redondear a dos decimales
  }

  onProductChange(index: number): void {
    const detail = this.purchaseDetails.at(index);
    const product = detail.get('product')?.value;
    if (product) {
      this.updateTotal(index);
    }
  }


  updateTotal(index: number): void {
    const detail = this.purchaseDetails.at(index);
    const product = detail.get('product')?.value;
    const priceUnit = detail.get('priceUnit')?.value;
    let amount = detail.get('amount')?.value;

    if (product && amount !== null && amount !== undefined) {
      const unitSale = product.unitSale.toLowerCase();

      // Verificar la unidad de venta y validar la cantidad
      if (unitSale === 'unidad' && !Number.isInteger(amount)) {
        detail.get('amount')?.setErrors({ 'invalidAmount': true });
      } else {
        detail.get('amount')?.setErrors(null);
        detail.get('total')?.setValue((priceUnit || 0) * amount);
      }

      // Actualizar el total general de la venta
      const total = this.calculateTotal();
      this.purchaseForm.get('total')?.setValue(total);
    }
  }


  setpurchaseDetails(purchaseDetails: any[]): void {
    const detailsFormArray = this.purchaseForm.get('purchaseDetails') as FormArray;
    purchaseDetails.forEach((detail: any) => {
      detailsFormArray.push(
        this.fb.group({
          id: [detail.id], // Agregar el ID del detalle de Compra
          product: [detail.product, Validators.required],
          priceUnit: [detail.priceUnit, [Validators.required]],
          amount: [detail.amount, [Validators.required, Validators.min(1)]]
        })
      );
    });
  }

  clearProducts() {
    // Eliminar todos los detalles de Compra
    while (this.purchaseDetails.length !== 0) {
      this.removeProduct(0);
    }
  }

  // Método para mostrar el nombre completo del suppliere en el autocompletado
  displaySupplier(supplier: any): string {
    return supplier && supplier.names && supplier.lastName ? `${supplier.names} ${supplier.lastName}` : '';
  }

  // Método para filtrar supplieres según lo que se escribe en el campo
  filterSuppliers(value: string): void {
    const filterValue = value.toLowerCase();
    this.filteredSuppliers = this.allSuppliers.filter(c =>
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
