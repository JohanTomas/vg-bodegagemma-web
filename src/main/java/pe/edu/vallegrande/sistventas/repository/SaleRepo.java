package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.vallegrande.sistventas.dto.Sale;

import java.util.List;

public interface SaleRepo extends JpaRepository<Sale, Long> {
    // Nuevo método para encontrar ventas por estado
    List<Sale> findByActive(String active);

}
