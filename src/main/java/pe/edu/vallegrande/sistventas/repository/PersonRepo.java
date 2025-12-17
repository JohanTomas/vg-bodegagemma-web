package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.sistventas.model.Person;

import java.util.List;

@Repository
public interface PersonRepo extends JpaRepository<Person, Long> {
    List<Person> findByRolPersonAndNumberDocument(String rolPerson, String numberDocument);
    List<Person> findByRolPerson(String rolPerson);
    List<Person> findByRolPersonAndActive(String rolPerson, String active);
}
