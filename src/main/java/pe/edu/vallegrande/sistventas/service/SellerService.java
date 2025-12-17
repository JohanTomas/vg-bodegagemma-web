package pe.edu.vallegrande.sistventas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sistventas.model.Person;
import pe.edu.vallegrande.sistventas.repository.PersonRepo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SellerService {
    HashMap<String, Object> datos;
    private final PersonRepo personRepo;

    @Autowired
    public SellerService(PersonRepo personRepo){
        this.personRepo = personRepo;
    }
    // Listado completo de todas las personas
    public List<Person> getPersons(){
        return this.personRepo.findAll();
    }
    //busqueda por numberdocument de todos los vendedores
    public List<Person> getExistingSellers(String numberDocument) {
        return personRepo.findByRolPersonAndNumberDocument("V", numberDocument);
    }
    // Listar vendedores activos (rolPerson = "V")
    public List<Person> getSellers() {
        return this.personRepo.findByRolPerson("V").stream()
                .sorted(Comparator.comparing(Person::getId).reversed())
                .collect(Collectors.toList());
    }
    // Listar vendedores activos (rolPerson = "V")
    public List<Person> getActiveSellers() {
        return this.personRepo.findByRolPersonAndActive("V", "A").stream()
                .sorted(Comparator.comparing(Person::getId).reversed())
                .collect(Collectors.toList());
    }
    // Listar vendedores inactivos (rolPerson = "V")
    public List<Person> getInactiveSellers() {
        return this.personRepo.findByRolPersonAndActive("V", "I").stream()
                .sorted(Comparator.comparing(Person::getId).reversed())
                .collect(Collectors.toList());
    }
    // Listado con paginador en vendedores activos
    public Page<Person> listPageableSellerA(Pageable pageable){
        // Obtiene todos los vendedores activos
        List<Person> allActiveSellers = this.personRepo.findByRolPersonAndActive("V", "A");
        // Ordena toda la lista por ID
        allActiveSellers.sort(Comparator.comparing(Person::getId).reversed());
        // Aplica la paginación
        List<Person> formattedSellers = allActiveSellers.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(formattedSellers, pageable, allActiveSellers.size());
    }

    // Listado con paginador en vendedores inactivos
    public Page<Person> listPageableSellerI(Pageable pageable){
        // Obtiene todos los vendedores inactivos
        List<Person> allInactiveSellers = this.personRepo.findByRolPersonAndActive("V", "I");
        // Ordena toda la lista por ID
        allInactiveSellers.sort(Comparator.comparing(Person::getId).reversed());
        // Aplica la paginación
        List<Person> formattedSellers = allInactiveSellers.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(formattedSellers, pageable, allInactiveSellers.size());
    }

    // Obtener vendedor por ID
    public ResponseEntity<Object> getSellerById(Long id){
        Optional<Person> optionalCSeller = personRepo.findById(id);
        datos = new HashMap<>();

        if (optionalCSeller.isPresent()){
            datos.put("data", optionalCSeller.get());
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("mensaje", "No se encontró un vendedor con el ID proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }
    // Insertar un nuevo Vendedor
    public ResponseEntity<Object> newSeller(Person seller) {
        // Establecer el rolPerson como "V" y el setActive como "A"
        seller.setRolPerson("V");
        seller.setActive("A");
        // Insertar el nuevo seller
        Person savedSeller = personRepo.save(seller);
        HashMap<String, Object> responseData = new HashMap<>();
        responseData.put("mensaje", "Se guardó con éxito");
        responseData.put("data", savedSeller);
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }

    // Actualizar por id
    public ResponseEntity<Object> updateSeller(Long id, Person updateSeller){
        Optional<Person> optionalSeller = personRepo.findById(id);
        HashMap<String, Object> datos = new HashMap<>();
        if (optionalSeller.isPresent()){
            Person existingSeller = optionalSeller.get();
            updateExistingSeller(existingSeller, updateSeller); // Método para actualizar el vendedor existente con los datos del vendedor actualizado
            // Guardar el vendedor actualizado en la base de datos
            personRepo.save(existingSeller);
            datos.put("mensaje", "Vendedor actualizado con éxito");
            datos.put("data", existingSeller);
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("mensaje", "No se encontró un vendedor con el ID proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }
    // Método para actualizar el vendedor existente con los datos del vendedor actualizado
    private void updateExistingSeller(Person existingSeller, Person updateSeller) {
        // Campos no modificables o con valores predeterminados
        existingSeller.setTypeDocument(updateSeller.getTypeDocument());
        existingSeller.setNumberDocument(updateSeller.getNumberDocument());
        existingSeller.setNames(updateSeller.getNames());
        existingSeller.setLastName(updateSeller.getLastName());
        existingSeller.setEmail(updateSeller.getEmail());
        existingSeller.setCellPhone(updateSeller.getCellPhone());
        existingSeller.setSalary(updateSeller.getSalary());
        existingSeller.setSellerRol((updateSeller.getSellerRol()));
        existingSeller.setSellerUser((updateSeller.getSellerUser()));
        existingSeller.setSellerPassword((updateSeller.getSellerPassword()));
    }
    //Eliminar Vendedor
    public ResponseEntity<Object> deleteseller(Long id){
        datos = new HashMap<>();
        boolean existe = this.personRepo.existsById(id);
        if(!existe){
            datos.put("error",true);
            datos.put("mensaje","No existe un vendedor con ese id");
            return new ResponseEntity<>(datos, HttpStatus.CONFLICT);
        }
        personRepo.deleteById(id);
        datos.put("mensaje","Vendedor eliminado");
        return new ResponseEntity<>(datos, HttpStatus.ACCEPTED);
    }
    //Eliminar vendedor Logicamente
    public ResponseEntity<Object> deleteSellerById(Long id) {
        Optional<Person> optionalSeller = personRepo.findById(id);
        if (optionalSeller.isPresent()) {
            Person existingSeller = optionalSeller.get();
            if (existingSeller.getActive().equals("I")) {
                return ResponseEntity.badRequest().body("{\"message\": \"El vendedor ya está inactivo.\"}");
            }
            // Cambiar el estado del Vendedor a inactivo
            existingSeller.setActive("I");
            // Guardar el vendedor actualizado en la base de datos
            personRepo.save(existingSeller);
            return ResponseEntity.ok("{\"message\": \"Vendedor marcado como inactivo.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    //Activar Vendedor
    public ResponseEntity<Object> activateSellerById(Long id) {
        Optional<Person> optionalSeller = personRepo.findById(id);
        if (optionalSeller.isPresent()) {
            Person seller = optionalSeller.get();
            if (seller.getActive().equals("A")) {
                return ResponseEntity.badRequest().body("{\"message\": \"El vendedor ya está activo.\"}");
            }
            seller.setActive("A");
            personRepo.save(seller);
            return ResponseEntity.ok("{\"message\": \"Vendedor activado correctamente.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    public ResponseEntity<Object> changePassword(Long sellerId, String oldPassword, String newPassword) {
        Optional<Person> sellerOptional = personRepo.findById(sellerId);
        HashMap<String, Object> responseData = new HashMap<>();

        if (!sellerOptional.isPresent()) {
            responseData.put("mensaje", "Seller not found");
            return new ResponseEntity<>(responseData, HttpStatus.NOT_FOUND);
        }

        Person seller = sellerOptional.get();

        // Aquí deberías agregar la lógica para verificar la contraseña antigua
        if (!seller.getSellerPassword().equals(oldPassword)) {
            responseData.put("mensaje", "Old password is incorrect");
            return new ResponseEntity<>(responseData, HttpStatus.BAD_REQUEST);
        }

        // Cambiar la contraseña
        seller.setSellerPassword(newPassword);
        personRepo.save(seller);

        responseData.put("mensaje", "Password updated successfully");
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }
}
