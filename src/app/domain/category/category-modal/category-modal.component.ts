import { Component, OnInit, Inject, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl, ValidatorFn, AbstractControl, FormGroupDirective, NgForm } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CategoryService } from '../../../core/services/category.service';
import Swal from 'sweetalert2';
import { MatPaginator } from '@angular/material/paginator';
import { ErrorStateMatcher } from '@angular/material/core';

export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const isSubmitted = form && form.submitted;
    return !!(control && control.invalid && (control.dirty || control.touched || isSubmitted));
  }
}
@Component({
  selector: 'app-category-modal',
  templateUrl: './category-modal.component.html',
  styleUrl: './category-modal.component.scss'
})
export class CategoryModalComponent implements OnInit {
  categoryForm: FormGroup = new FormGroup({});
  category: any;
  errorMessage: string = '';
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  matcher = new MyErrorStateMatcher();
  

  constructor(
    private dialogRef: MatDialogRef<CategoryModalComponent>,
    private fb: FormBuilder,
    private categoryService: CategoryService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    this.initCategoryForm();
    console.log('DATA:', this.data);
    this.category = { ...this.data };
  }

  saveCategory() {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }
    if (this.data) {
      this.updateCategory();
    } else {
      this.registerCategory();
    }
  }
  registerCategory() {
    this.categoryService.newCategory(this.categoryForm.value).subscribe((res) => {
      console.log('Respuesta registrar categoria:', res);
      this.showInsertNotification();
      this.closeDialog(true);
    });
  }
  showInsertNotification() {
    Swal.fire({
      title: "Categoria Insertado!",
      text: "La Categoria se ha insertado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.categoryService.categoryActualizar.next([]);
    });
  }
  updateCategory() {
    this.categoryService
      .updateCategoryById(this.data.id, this.categoryForm.value)
      .subscribe((res) => {
        console.log('Respuesta actualizar categoria:', res);
        this.showEditNotification();
        this.closeDialog(true);
      });
  }
  showEditNotification() {
    Swal.fire({
      title: "Categoria Editado!",
      text: "La Categoria se ha editado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.categoryService.categoryActualizar.next([]);
    });
  }
  closeDialog(success?: boolean) {
    this.dialogRef.close(success);
  }
  //VALIDACIONES EN TODOS LOS CAMPOS
  initCategoryForm() {
    this.categoryForm = this.fb.group({
      name: ['', {
        validators: [Validators.required, this.firstLetterCapital],
        asyncValidators: [this.validateName.bind(this)],
      }],
      description: ['', [Validators.required]],
    });
    if (this.data) {
      this.categoryForm.patchValue(this.data);
      // Quitamos la validación de documento existente cuando se está editando
      this.categoryForm.get('name')?.setAsyncValidators([]);
    }
  }
  
  //VALIDACION NOMBRE PARA QUE INGRESE EL PRIMER NOMBRE EN MAYUSACULA
  firstLetterCapital(control: AbstractControl) {
    const name = control.value;
    if (name && name.charAt(0) !== name.charAt(0).toUpperCase()) {
      return { firstLetterNotCapital: true };
    }
    return null;
  }
  
  //VALIDACION PARA NOMBRE
  async validateName(control: AbstractControl) {
    const name = control.value;
    const isNameExists = await this.categoryService.checkNameExists(name).toPromise();
    if (isNameExists) {
      return { nameExists: true };
    }
    return null;
  }
  get form() {
    return this.categoryForm.controls;
  }
}
