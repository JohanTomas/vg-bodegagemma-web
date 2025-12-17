package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.sistventas.model.Supplier;

import java.util.List;
import java.util.Optional;
@Repository
public interface SupplierRepo extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findSupplierByNumberDocument(String numberDocument);
    List<Supplier> findByNumberDocument(String numberDocument);
    List<Supplier> findByActive(String active);
}
