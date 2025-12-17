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

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientService {
    HashMap<String, Object> datos;
    private final PersonRepo personRepo;
    @Autowired
    public ClientService(PersonRepo personRepo){this.personRepo = personRepo;}
    // Método para formatear la fecha de nacimiento
    private Person formatDateOfBirth(Person person) {
        if (person != null && person.getBirthdate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            String formattedDate = person.getBirthdate().format(formatter);
            formattedDate = formattedDate.replace(".", ""); // Eliminar el punto después del mes
            person.setBirthdateFormatted(formattedDate);
        }
        return person;
    }
    // Listado completo de todas las personas
    public List<Person> getPersons(){
        return this.personRepo.findAll();
    }
    //busqueda por numberdocument de todos los clientes
    public List<Person> getExistingClients(String numberDocument) {
        return personRepo.findByRolPersonAndNumberDocument("C", numberDocument);
    }
    // Listar clientes activos (rolPerson = "C")
    public List<Person> getClients() {
        return this.personRepo.findByRolPerson("C").stream()
                .map(this::formatDateOfBirth)
                .sorted(Comparator.comparing(Person::getId).reversed())
                .collect(Collectors.toList());
    }
    // Listar clientes activos (rolPerson = "C")
    public List<Person> getActiveClients() {
        return this.personRepo.findByRolPersonAndActive("C", "A").stream()
                .map(this::formatDateOfBirth)
                .sorted(Comparator.comparing(Person::getId).reversed()) // Ordenar por nombre
                .collect(Collectors.toList());
    }

    // Listar clientes inactivos (rolPerson = "C")
    public List<Person> getInactiveClients() {
        return this.personRepo.findByRolPersonAndActive("C", "I").stream()
                .map(this::formatDateOfBirth)
                .sorted(Comparator.comparing(Person::getId).reversed())
                .collect(Collectors.toList());
    }
    // Obtener cliente por ID
    public ResponseEntity<Object> getClientById(Long id){
        Optional<Person> optionalClient = personRepo.findById(id);
        datos = new HashMap<>();
        if (optionalClient.isPresent()){
            Person client = optionalClient.get();
            datos.put("data", client);
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("mensaje", "No se encontró un cliente con el ID proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }
    // Listado con paginador en clientes activos
    public Page<Person> listPageableClientsA(Pageable pageable){
        // Obtiene todos los clientes activos
        List<Person> allActiveClients = this.personRepo.findByRolPersonAndActive("C", "A");
        // Ordena toda la lista
        allActiveClients.sort(Comparator.comparing(Person::getId).reversed());
        // Aplica la paginación
        List<Person> formattedClients = allActiveClients.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(this::formatDateOfBirth)
                .collect(Collectors.toList());
        return new PageImpl<>(formattedClients, pageable, allActiveClients.size());
    }
    // Listado con paginador en clientes inactivos
    public Page<Person> listPageableClientsI(Pageable pageable){
        // Obtiene todos los clientes inactivos
        List<Person> allInactiveClients = this.personRepo.findByRolPersonAndActive("C", "I");
        // Ordena toda la lista
        allInactiveClients.sort(Comparator.comparing(Person::getId).reversed());
        // Aplica la paginación
        List<Person> formattedClients = allInactiveClients.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(this::formatDateOfBirth)
                .collect(Collectors.toList());
        return new PageImpl<>(formattedClients, pageable, allInactiveClients.size());
    }
    //INGRESAR UN NUEVO CLIENTE
    public ResponseEntity<Object> newClient(Person client) {
        // Establecer el rolPerson como "C" y el setActive como "A"
        client.setRolPerson("C");
        client.setActive("A");
        // Insertar el nuevo cliente
        Person savedClient = personRepo.save(client);
        HashMap<String, Object> responseData = new HashMap<>();
        responseData.put("mensaje", "Se guardó con éxito");
        responseData.put("data", savedClient);
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }

    // Actualizar
    public ResponseEntity<Object> updateClient(Long id, Person updateClient){
        Optional<Person> optionalClient = personRepo.findById(id);
        datos = new HashMap<>();
        if (optionalClient.isPresent()){
            Person existingClient = optionalClient.get();
            updateExistingClient(existingClient, updateClient); // Método para actualizar el cliente existente con los datos del cliente actualizado
            // Guardar el cliente actualizado en la base de datos
            personRepo.save(existingClient);
            datos.put("mensaje", "Cliente actualizado con éxito");
            datos.put("data", existingClient);
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("mensaje", "No se encontró un cliente con el ID proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }
    // Método para actualizar el cliente existente con los datos del cliente actualizado
    private void updateExistingClient(Person existingClient, Person updateClient) {
        // Campos no modificables o con valores predeterminados
        existingClient.setTypeDocument(updateClient.getTypeDocument());
        existingClient.setNumberDocument(updateClient.getNumberDocument());
        existingClient.setNames(updateClient.getNames());
        existingClient.setLastName(updateClient.getLastName());
        existingClient.setEmail(updateClient.getEmail());
        existingClient.setCellPhone(updateClient.getCellPhone());
        existingClient.setBirthdate(updateClient.getBirthdate());
    }
    //Eliminar cliente
    public ResponseEntity<Object> deleteclient(Long id){
        datos = new HashMap<>();
        boolean existe = this.personRepo.existsById(id);
        if(!existe){
            datos.put("error",true);
            datos.put("mensaje","No existe un cliente con ese id");
            return new ResponseEntity<>(datos, HttpStatus.CONFLICT);
        }
        personRepo.deleteById(id);
        datos.put("mensaje","Cliente eliminado");
        return new ResponseEntity<>(datos, HttpStatus.ACCEPTED);
    }
    //Eliminar cliente Logicamente
    public ResponseEntity<Object> deleteClientById(Long id) {
        Optional<Person> optionalClient = personRepo.findById(id);
        if (optionalClient.isPresent()) {
            Person existingClient = optionalClient.get();
            if (existingClient.getActive().equals("I")) {
                return ResponseEntity.badRequest().body("{\"message\": \"El cliente ya está inactivo.\"}");
            }
            // Cambiar el estado del cliente a inactivo
            existingClient.setActive("I");
            // Guardar el cliente actualizado en la base de datos
            personRepo.save(existingClient);
            return ResponseEntity.ok("{\"message\": \"Cliente marcado como inactivo.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    //Activar Cliente
    public ResponseEntity<Object> activateClientById(Long id) {
        Optional<Person> optionalClient = personRepo.findById(id);
        if (optionalClient.isPresent()) {
            Person client = optionalClient.get();
            if (client.getActive().equals("A")) {
                return ResponseEntity.badRequest().body("{\"message\": \"El cliente ya está activo.\"}");
            }
            client.setActive("A");
            personRepo.save(client);
            return ResponseEntity.ok("{\"message\": \"Cliente activado correctamente.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
