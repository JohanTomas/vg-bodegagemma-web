package pe.edu.vallegrande.sistventas.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sistventas.model.Supplier;
import pe.edu.vallegrande.sistventas.repository.SupplierRepo;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupplierService {
    HashMap<String, Object> datos;
    private final SupplierRepo supplierRepo;
    @Autowired
    public SupplierService(SupplierRepo supplierRepo){
        this.supplierRepo = supplierRepo;
    }
    // Listado completo de todas las proveedores
    public List<Supplier> getSuppliers(){
        return this.supplierRepo.findAll();
    }
    // Listar proveedores activos
    public List<Supplier> getActiveSuppliers() {
        return this.supplierRepo.findByActive("A").stream()
                .sorted(Comparator.comparing(Supplier::getId).reversed())
                .collect(Collectors.toList());
    }
    // Listar proveedores inactivos
    public List<Supplier> getInactiveSuppliers() {
        return this.supplierRepo.findByActive("I").stream()
                .sorted(Comparator.comparing(Supplier::getId).reversed())
                .collect(Collectors.toList());
    }
    // Busque por numero Documento de todos los proveedores
    public List<Supplier> getExistingSuppliers(String numberDocument) {
        return supplierRepo.findByNumberDocument(numberDocument);
    }
    // Obtener proveedores por ID
    public ResponseEntity<Object> getSupplierById(Long id){
        Optional<Supplier> optionalSupplier = supplierRepo.findById(id);
        HashMap<String, Object> responseData = new HashMap<>();
        if (optionalSupplier.isPresent()){
            Supplier supplier = optionalSupplier.get();
            responseData.put("data", supplier);
            return new ResponseEntity<>(responseData, HttpStatus.OK);
        } else {
            responseData.put("error", true);
            responseData.put("mensaje", "No se encontró una proveedor con el ID proporcionado.");
            return new ResponseEntity<>(responseData, HttpStatus.NOT_FOUND);
        }
    }
    // Listado con paginador en proveedores activos
    public Page<Supplier> listPageableSuppliersA(Pageable pageable){
        // Obtiene todos los vendedores activos
        List<Supplier> allActiveSellers = this.supplierRepo.findByActive("A");
        // Ordena toda la lista por ID
        allActiveSellers.sort(Comparator.comparing(Supplier::getId).reversed());
        // Aplica la paginación
        List<Supplier> formattedSellers = allActiveSellers.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(formattedSellers, pageable, allActiveSellers.size());
    }
    // Listado con paginador en proveedores inactivos
    public Page<Supplier> listPageableSuppliersI(Pageable pageable){
        // Obtiene todos los vendedores activos
        List<Supplier> allActiveSellers = this.supplierRepo.findByActive("I");
        // Ordena toda la lista por ID
        allActiveSellers.sort(Comparator.comparing(Supplier::getId).reversed());
        // Aplica la paginación
        List<Supplier> formattedSellers = allActiveSellers.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(formattedSellers, pageable, allActiveSellers.size());
    }
    //INGRESAR UNA PROVEEDORES
    public ResponseEntity<Object> newSupplier(Supplier supplier) {
        // Establecer el setActive como "A"
        supplier.setActive("A");
        // Verificar si el proveedor ya existe en la base de datos
        Optional<Supplier> existingSupplier = supplierRepo.findSupplierByNumberDocument(supplier.getNumberDocument());
        if (existingSupplier.isPresent()) {
            // Si el proveedor ya existe, devolver un mensaje de error
            HashMap<String, Object> responseData = new HashMap<>();
            responseData.put("error", true);
            responseData.put("mensaje", "Ya existe un proveedor con ese nombre");
            return new ResponseEntity<>(responseData, HttpStatus.CONFLICT);
        }
        // Si no existe, insertar una nuevo proveedor
        Supplier savedSupplier = supplierRepo.save(supplier);
        HashMap<String, Object> responseData = new HashMap<>();
        responseData.put("mensaje", "Se guardó con éxito");
        responseData.put("data", savedSupplier);
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }

    // Actualizar
    public ResponseEntity<Object> updateSupplier(Long id, Supplier updateSupplier){
        Optional<Supplier> optionalSupplier = supplierRepo.findById(id);
        HashMap<String, Object> datos = new HashMap<>();
        if (optionalSupplier.isPresent()){
            Supplier existingSupplier = optionalSupplier.get();
            updateExistingSupplier(existingSupplier, updateSupplier);
            // Guardar el proveedor actualizado en la base de datos
            supplierRepo.save(existingSupplier);
            datos.put("mensaje", "Proveedor actualizado con éxito");
            datos.put("data", existingSupplier);
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("mensaje", "No se encontró un Proveedor con el ID proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }
    // Método para actualizar al proveedor existente con los datos del proveedor actualizado
    private void updateExistingSupplier(Supplier existingSupplier, Supplier updateSupplier) {
        // Campos no modificables o con valores predeterminados
        existingSupplier.setRuc(updateSupplier.getRuc());
        existingSupplier.setNameCompany(updateSupplier.getNameCompany());
        existingSupplier.setTypeDocument(updateSupplier.getTypeDocument());
        existingSupplier.setNumberDocument(updateSupplier.getNumberDocument());
        existingSupplier.setNames(updateSupplier.getNames());
        existingSupplier.setLastName(updateSupplier.getLastName());
        existingSupplier.setEmail(updateSupplier.getEmail());
        existingSupplier.setCellPhone(updateSupplier.getCellPhone());
    }
    //Eliminar proveedor
    public ResponseEntity<Object> deletesupplier(Long id){
        datos = new HashMap<>();
        boolean existe = this.supplierRepo.existsById(id);
        if(!existe){
            datos.put("error",true);
            datos.put("mensaje","No existe un proveedor con ese id");
            return new ResponseEntity<>(datos, HttpStatus.CONFLICT);
        }
        supplierRepo.deleteById(id);
        datos.put("mensaje","Proveedor eliminado");
        return new ResponseEntity<>(datos, HttpStatus.ACCEPTED);
    }
    //Eliminar proveedor Logicamente
    public ResponseEntity<Object> deleteSupplierById(Long id) {
        Optional<Supplier> optionalSupplier = supplierRepo.findById(id);
        if (optionalSupplier.isPresent()) {
            Supplier existingSupplier = optionalSupplier.get();
            if (existingSupplier.getActive().equals("I")) {
                return ResponseEntity.badRequest().body("{\"message\": \"La proveedor ya está inactivo.\"}");
            }
            // Cambiar el estado del proveedor a inactivo
            existingSupplier.setActive("I");
            // Guardar el proveedor actualizado en la base de datos
            supplierRepo.save(existingSupplier);
            return ResponseEntity.ok("{\"message\": \"Proveedor marcado como inactivo.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    //Activar Proveedor
    public ResponseEntity<Object> activateSupplierById(Long id) {
        Optional<Supplier> optionalSupplier = supplierRepo.findById(id);
        if (optionalSupplier.isPresent()) {
            Supplier supplier = optionalSupplier.get();
            if (supplier.getActive().equals("A")) {
                return ResponseEntity.badRequest().body("{\"message\": \"La Proveedor ya está activo.\"}");
            }
            supplier.setActive("A");
            supplierRepo.save(supplier);
            return ResponseEntity.ok("{\"message\": \"Proveedor activado correctamente.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
