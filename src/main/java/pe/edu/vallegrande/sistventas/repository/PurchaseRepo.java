package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.vallegrande.sistventas.dto.Purchase;

import java.util.List;

public interface PurchaseRepo extends JpaRepository<Purchase, Long> {
    // Nuevo método para encontrar compras por estado
    List<Purchase> findByActive(String active);

}
