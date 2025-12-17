import { Component, OnInit, Inject, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl, ValidatorFn, AbstractControl, FormGroupDirective, NgForm } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SupplierService } from '../../../core/services/supplier.service';
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
  selector: 'app-supplier-modal',
  templateUrl: './supplier-modal.component.html',
  styleUrl: './supplier-modal.component.scss'
})
export class SupplierModalComponent implements OnInit {
  supplierForm: FormGroup = new FormGroup({});
  errorMessage: string = '';
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  matcher = new MyErrorStateMatcher();

  constructor(
    private dialogRef: MatDialogRef<any>,
    private fb: FormBuilder,
    private supplierService: SupplierService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    this.initSupplierForm();
    console.log('DATA:', this.data);
  }

  saveSupplier() {
    if (this.supplierForm.invalid) {
      this.supplierForm.markAllAsTouched();
      return;
    }
    if (this.data) {
      this.updateSupplier();
    } else {
      this.registerSupplier();
    }
  }

  registerSupplier() {
    this.supplierService.newSupplier(this.supplierForm.value).subscribe((res) => {
      console.log('Respuesta registrar proveedor:', res);
      this.showInsertNotification();
      this.closeDialog(true);
    });
  }

  showInsertNotification() {
    Swal.fire({
      title: "Proveedor Insertado!",
      text: "El Proveedor se ha insertado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.supplierService.supplierActualizar.next([]);
    });
  }

  updateSupplier() {
    this.supplierService
      .updateSupplierById(this.data.id, this.supplierForm.value)
      .subscribe((res) => {
        console.log('Respuesta actualizar proveedor:', res);
        this.showEditNotification();
        this.closeDialog(true);
      });
  }

  showEditNotification() {
    Swal.fire({
      title: "Proveedor Editado!",
      text: "El Proveedor se ha editado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.supplierService.supplierActualizar.next([]);
    });
  }

  closeDialog(success?: boolean) {
    this.dialogRef.close(success);
  }

  //VALIDACIONES EN TODOS LOS CAMPOS
  initSupplierForm() {
    this.supplierForm = this.fb.group({
      ruc: ['', [Validators.required, this.validateRuc.bind(this)]],
      nameCompany: ['', [Validators.required]],
      typeDocument: ['', [Validators.required]],
      numberDocument: ['', {
        validators: [Validators.required],
        asyncValidators: [this.validateNumberDocument.bind(this)],
      }],
      names: ['', [Validators.required, this.validateName.bind(this)]],
      lastName: ['', [Validators.required, this.validateLastName.bind(this)]],
      cellPhone: [null, [this.validateCellPhone.bind(this)]],
      email: [null, [this.validateEmail]],
    });

    if (this.data) {
      this.supplierForm.patchValue(this.data);
      // Quitamos la validación de documento existente cuando se está editando
      this.supplierForm.get('numberDocument')?.setAsyncValidators([]);
    }
  }


  // Validación para número de documento
  async validateNumberDocument(control: AbstractControl) {
    const typeDocument = this.supplierForm.get('typeDocument')?.value;
    const numberDocument = control.value;
    if (!typeDocument) {
      return { typeDocumentRequired: true };
    }
    if (typeDocument === 'DNI' && (numberDocument.toString().length !== 8 || isNaN(numberDocument))) {
      return { invalidDNI: true };
    }
    if (typeDocument === 'CNE' && (numberDocument.toString().length !== 20 || isNaN(numberDocument))) {
      return { invalidCNE: true };
    }
    const isDocumentExists = await this.supplierService.checkDocumentExists(numberDocument).toPromise();
    if (isDocumentExists) {
      return { documentExists: true };
    }
    return null;
  }
  // Validación para nombres
  validateName = (control: FormControl) => {
    const namePattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;
    if (!namePattern.test(control.value)) {
      return { invalidName: true };
    }

    const namesParts = control.value.trim().split(/\s+/);
    for (const namePart of namesParts) {
      if (namePart.length > 0 && namePart[0] !== namePart[0].toUpperCase()) {
        return { firstLetterUpperCase: true };
      }
    }

    return null;
  };

  // Validación para apellidos
  validateLastName = (control: FormControl) => {
    const lastNamePattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;
    if (!lastNamePattern.test(control.value)) {
      return { invalidLastName: true };
    }

    const lastNameParts = control.value.trim().split(/\s+/);
    for (const lastNamePart of lastNameParts) {
      if (lastNamePart.length > 0 && lastNamePart[0] !== lastNamePart[0].toUpperCase()) {
        return { firstLetterUpperCase: true };
      }
    }

    return null;
  };
  // Validación para RUC
  validateRuc(control: AbstractControl) {
    const ruc = control.value;
    if (!ruc || ruc.length !== 11 || isNaN(ruc)) {
      return { invalidRUC: true };
    }
    return null;
  }


  // Validación para número de celular
  validateCellPhone = (control: FormControl) => {
    if (!control.value) {
      return null; // Permite valor nulo
    }

    const cellPhoneStartsWithNine = control.value.startsWith('9');
    const cellPhonePattern = /^[0-9]{9}$/;
    if (!cellPhoneStartsWithNine) {
      return { cellPhoneStartsWithNine: true };
    }
    if (!cellPhonePattern.test(control.value)) {
      return { invalidCellPhone: true };
    }

    return null;
  };
  // Validación para correo electrónico
  validateEmail = (control: FormControl) => {
    if (!control.value) {
      return null; // Permite valor nulo
    }

    const emailPattern = /^[a-zA-Z0-9._,:-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!emailPattern.test(control.value)) {
      return { invalidEmail: true };
    }
    return null;
  };


  get form() {
    return this.supplierForm.controls;
  }

}
