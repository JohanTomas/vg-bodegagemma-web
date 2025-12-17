import { Component, OnInit, Inject, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators, AbstractControl, ValidatorFn, AsyncValidatorFn, ValidationErrors, FormControl } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import Swal from 'sweetalert2';
import { MatPaginator } from '@angular/material/paginator';
import { ErrorStateMatcher } from '@angular/material/core';
import { MyErrorStateMatcher } from '../../category/category-modal/category-modal.component';

@Component({
  selector: 'app-product-modal',
  templateUrl: './product-modal.component.html',
  styleUrls: ['./product-modal.component.scss'],
})
export class ProductModalComponent implements OnInit {
  productForm: FormGroup = new FormGroup({});
  errorMessage: string = '';
  product: any;
  categoryProduct: any[] = []; // Asumiendo que es un arreglo
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  matcher = new MyErrorStateMatcher();

  constructor(
    private dialogRef: MatDialogRef<ProductModalComponent>,
    private fb: FormBuilder,
    private productService: ProductService,
    private categoryService: CategoryService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    this.initProductForm();
    console.log('DATA:', this.data);
    this.product = { ...this.data };
    this.categoryService.activateCategory().subscribe((data: any) => {
      this.categoryProduct = data;
    });
  }

  saveProduct() {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }
    if (this.data) {
      this.updateProduct();
    } else {
      this.registerProduct();
    }
  }

  registerProduct() {
    this.productService.newProduct(this.productForm.value).subscribe((res) => {
      console.log('Respuesta registrar producto:', res);
      this.showInsertNotification();
      this.closeDialog(true);
    });
  }

  showInsertNotification() {
    Swal.fire({
      title: "Producto Insertado!",
      text: "El Producto se ha insertado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.productService.productActualizar.next([]);
    });
  }

  updateProduct() {
    this.productService.updateProductById(this.data.id, this.productForm.value).subscribe((res) => {
      console.log('Respuesta actualizar producto:', res);
      this.showEditNotification();
      this.closeDialog(true);
    });
  }

  showEditNotification() {
    Swal.fire({
      title: "Producto Editado!",
      text: "El Producto se ha editado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.productService.productActualizar.next([]);
    });
  }

  closeDialog(success?: boolean) {
    this.dialogRef.close(success);
  }

  getCategories() {
    this.categoryService.activateCategory().subscribe((res: any) => {
      this.categoryProduct = res.content;
    });
  }

  //VALIDACIONES EN TODOS LOS CAMPOS
  initProductForm() {
    this.productForm = this.fb.group({
      code: ['', [Validators.required, this.codeValidator()], [this.codeExistsValidator()]],
      name: ['', [Validators.required, this.firstLetterCapital], [this.nameExistsValidator()]],
      description: [''],
      categoryProduct: ['', [Validators.required]],
      priceUnit: ['', [Validators.required, this.priceFormatValidator]],
      unitSale: ['', [Validators.required]],
      dateExpiry: ['', [this.futureDateValidator]],
      stock: ['', [Validators.required, this.stockValidator()]], // Agregar el validador de stock aquí
    });

    if (this.data) {
      this.productForm.patchValue(this.data);
      this.productForm.get('name')?.clearAsyncValidators();
      this.productForm.get('code')?.clearAsyncValidators();
    }
  }

  stockValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const unitSale = this.productForm.get('unitSale')?.value;
      const stockValue = control.value;

      // Validar que se haya seleccionado una unidad de venta
      if (!unitSale) {
        return { unitSaleRequired: true };
      }

      // Validar según la unidad de venta seleccionada
      if (unitSale === 'Unidad') {
        // Validar que sea un número entero
        if (!Number.isInteger(stockValue)) {
          return { integerRequired: true };
        }
      } else if (unitSale === 'Kilo') {
        // Validar que sea un número con hasta dos decimales
        if (!/^(\d+(\.\d{1,2})?)$/.test(stockValue.toString())) {
          return { invalidDecimalFormat: true };
        }
      }

      return null; // Válido
    };
  }

  // Validación personalizada para el código
  codeValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (!value || typeof value !== 'string') {
        return null; // Si el campo está vacío o no es una cadena, no valida
      }

      // Validar que empiece con 'P'
      if (!value.startsWith('P')) {
        return { firstLetterNotP: true };
      }

      // Validar longitud total
      if (value.length !== 4) {
        return { invalidLength: true };
      }

      // Validar que los tres caracteres después de 'P' sean números
      const numericPart = value.substring(1);
      if (!/^\d{3}$/.test(numericPart)) {
        return { notThreeDigits: true };
      }

      return null; // Válido
    };
  }

  // Validador asíncrono para verificar si el código ya existe
  codeExistsValidator(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      return this.productService.CheckCodeExists(control.value).pipe(
        map((exists: boolean) => (exists ? { codeExists: true } : null)),
        catchError(() => of(null)) // En caso de error, considera que no hay error
      );
    };
  }

  // Validador asíncrono para verificar si el nombre ya existe
  nameExistsValidator(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      return this.productService.CheckNameExists(control.value).pipe(
        map((exists: boolean) => (exists ? { nameExists: true } : null)),
        catchError(() => of(null)) // En caso de error, considera que no hay error
      );
    };
  }

  // Validación nombre para que ingrese el primer nombre en mayúscula
  firstLetterCapital(control: AbstractControl): ValidationErrors | null {
    const name = control.value;
    if (name && name.charAt(0) !== name.charAt(0).toUpperCase()) {
      return { firstLetterNotCapital: true };
    }
    return null;
  }

  // Validación para precio de venta y stock
  priceFormatValidator(control: FormControl): ValidationErrors | null {
    // Expresión regular para validar el formato del precio
    const priceRegex = /^\d{1,8}(\.\d{1,2})?$/;
    // Validador personalizado para validar el formato del precio
    const valid = priceRegex.test(control.value);
    return valid ? null : { invalidPriceFormat: true };
  }

  // Validación en el campo fecha de expiración
  futureDateValidator(control: FormControl): ValidationErrors | null {
    const currentDate = new Date();
    const selectedDate = control.value ? new Date(control.value) : null;

    // Solo aplica la validación si se ha seleccionado una fecha
    if (selectedDate) {
      return selectedDate > currentDate ? null : { futureDate: true };
    }

    return null; // Si no se ha seleccionado fecha, no aplica la validación
  }

  get form() {
    return this.productForm.controls;
  }
}
