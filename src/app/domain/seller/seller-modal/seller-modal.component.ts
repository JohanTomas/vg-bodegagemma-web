import { Component, OnInit, Inject, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl, ValidatorFn, AbstractControl, FormGroupDirective, NgForm } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SellerService } from '../../../core/services/seller.service';
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
  selector: 'app-seller-modal',
  templateUrl: './seller-modal.component.html',
  styleUrl: './seller-modal.component.scss'
})
export class SellerModalComponent implements OnInit {
  sellerForm: FormGroup = new FormGroup({});
  errorMessage: string = '';
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  matcher = new MyErrorStateMatcher();

  constructor(
    private dialogRef: MatDialogRef<any>,
    private fb: FormBuilder,
    private sellerService: SellerService,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) { }

  ngOnInit(): void {
    this.initSellerForm();
    console.log('DATA:', this.data);
  }

  saveSeller() {
    if (this.sellerForm.invalid) {
      this.sellerForm.markAllAsTouched();
      return;
    }
    if (this.data) {
      this.updateSeller();
    } else {
      this.registerSeller();
    }
  }

  registerSeller() {
    this.sellerService.newSeller(this.sellerForm.value).subscribe((res) => {
      console.log('Respuesta registrar Vendedor:', res);
      this.showInsertNotification();
      this.closeDialog(true);
    });
  }

  showInsertNotification() {
    Swal.fire({
      title: "Vendedor Insertado!",
      text: "El vendedor se ha insertado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.sellerService.sellerActualizar.next([]);
    });
  }

  updateSeller() {
    this.sellerService
      .updateSellerById(this.data.id, this.sellerForm.value)
      .subscribe((res) => {
        console.log('Respuesta actualizar vendedor:', res);
        this.showEditNotification();
        this.closeDialog(true);
      });
  }

  showEditNotification() {
    Swal.fire({
      title: "Vendedor Editado!",
      text: "El vendedor se ha editado correctamente!",
      icon: "success",
      confirmButtonText: "Aceptar"
    }).then(() => {
      this.sellerService.sellerActualizar.next([]);
    });
  }

  closeDialog(success?: boolean) {
    this.dialogRef.close(success);
  }

  //VALIDACIONES EN TODOS LOS CAMPOS
  initSellerForm() {
    this.sellerForm = this.fb.group({
      typeDocument: ['', [Validators.required]],
      numberDocument: ['', {
        validators: [Validators.required],
        asyncValidators: [this.validateNumberDocument.bind(this)],
      }],
      names: ['', [Validators.required, this.validateName.bind(this)]],
      lastName: ['', [Validators.required, this.validateLastName.bind(this)]],
      cellPhone: ['', [Validators.required, this.validateCellPhone.bind(this)]],
      email: ['', [Validators.required, Validators.email]],
      salary: ['', [Validators.required, this.priceFormatValidator]],
      sellerRol: ['', [Validators.required]],
      sellerUser: ['', [Validators.required]],
      sellerPassword: ['', [Validators.required]],
    });

    if (this.data) {
      this.sellerForm.patchValue(this.data);
      // Quitamos la validación de documento existente cuando se está editando
      this.sellerForm.get('numberDocument')?.setAsyncValidators([]);
    }
  }

  //VALIDACION PARA NOMBRES Y APELLIDOS
  validateName = (control: FormControl) => {
    // Se ajusta la expresión regular para permitir ñ y tildes
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

  validateLastName = (control: FormControl) => {
    // Se ajusta la expresión regular para permitir ñ y tildes
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

  //VALIDACION PARA NUMERO DE DOCUMENTO
  async validateNumberDocument(control: AbstractControl) {
    const typeDocument = this.sellerForm.get('typeDocument')?.value;
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
    const isDocumentExists = await this.sellerService.checkDocumentExists(numberDocument).toPromise();
    if (isDocumentExists) {
      return { documentExists: true };
    }
    return null;
  }
  //VALIDACION PARA EL CAMPO CELL_PHONE
  validateCellPhone = (control: FormControl) => {
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
  //VALIDACION PARA SALARY
  priceFormatValidator(control: FormControl) {
    // Expresión regular para validar el formato del precio
    const priceRegex = /^\d{1,8}(\.\d{1,2})?$/;
    // Validador personalizado para validar el formato del precio
    const valid = priceRegex.test(control.value);
    return valid ? null : { invalidPriceFormat: true };
  }

  get form() {
    return this.sellerForm.controls;
  }
}
